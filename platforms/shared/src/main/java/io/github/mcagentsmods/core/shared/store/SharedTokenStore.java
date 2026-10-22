package io.github.mcagentsmods.core.shared.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import io.github.mcagentsmods.core.api.llm.LlmCredentials;
import io.github.mcagentsmods.core.api.llm.LlmVendor;
import io.github.mcagentsmods.core.api.token.TokenStore;
import io.github.mcagentsmods.core.common.file.ModFiles;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The credential store shared by every MCAgents mod on a client.
 *
 * <p>Backed by a single {@value MinecraftDirectory#FILE_NAME} in core's directory
 * under the Minecraft directory, {@code MCAgents/core/}. One file, one name, deliberately: a player configures a token once
 * and every MCAgents mod they install finds it, rather than each mod keeping its
 * own copy of the same key and each needing to be updated separately.</p>
 *
 * <p>The shape is one list of tokens per vendor:</p>
 *
 * <pre>{@code
 * {
 *   "openrouter": { "token": ["sk-or-v1-..."] },
 *   "openai":     { "token": [] }
 * }
 * }</pre>
 *
 * <h2>Sharing safely</h2>
 *
 * <p>Because several mods may hold this file open, a write must never leave it
 * half-written — another mod reading a truncated file would see no credentials
 * and report them as missing. Writes therefore go to a temporary file in the
 * same directory and are moved into place atomically, and every write re-reads
 * first so a change another mod made is not clobbered.</p>
 *
 * <h2>Threading</h2>
 *
 * <p>Every method touches disk and holds a lock shared by every store open on
 * the same file in this process, so the client's and the server's stores in a
 * single player game cannot interleave a read and a write. Eviction arrives on
 * whichever thread a failed request completed on, and must never run on the
 * client's render thread.</p>
 *
 * <p>A file that could not be read is never written. The keys it holds are the
 * player's, and a write would replace them with whatever this store had left —
 * usually nothing.</p>
 */
public final class SharedTokenStore implements TokenStore {

    /**
     * The field holding a vendor's credential list.
     */
    private static final String TOKEN_FIELD = "token";

    /**
     * Written into a new file so the vendor sections are discoverable without
     * documentation.
     */
    private static final Set<String> KNOWN_VENDORS = Stream.of(LlmVendor.values())
            .map(LlmVendor::code)
            .collect(Collectors.toUnmodifiableSet());

    /**
     * One lock per file, shared by every store in this process open on it.
     */
    private static final ConcurrentHashMap<Path, Object> LOCKS = new ConcurrentHashMap<>();

    /**
     * Pretty printing is deliberate: a player edits this file by hand, and a
     * single-line JSON blob is hostile to that.
     */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * The shared file.
     */
    private final Path file;

    /**
     * Where problems are reported. A mod loader supplies its own logger.
     */
    private final Logger logger;

    /**
     * Held for every read and write of {@link #file}.
     */
    private final Object lock;

    /**
     * Whether the last read failed. While it is set nothing is written, so a file
     * the player broke by hand keeps their keys until they fix it.
     */
    private boolean readFailed;

    /**
     * Platforms already warned about an unusable entry, so a list read on every
     * keystroke of tab completion does not repeat the warning.
     */
    private final Set<String> warnedUnusable = ConcurrentHashMap.newKeySet();

    /**
     * The parsed contents. Replaced wholesale by {@link #reload()} rather than
     * merged into, so an evicted credential cannot come back from a stale copy.
     */
    private JsonObject document = new JsonObject();

    /**
     * Opens the store, creating the file with empty vendor sections if it is
     * missing.
     *
     * @param loaderDirectory The game directory the mod loader reported, or
     *                        {@code null} to fall back to the conventional
     *                        location for this operating system.
     * @param logger Where to report problems.
     * @throws NullPointerException When {@code logger} is {@code null}.
     */
    public SharedTokenStore(Path loaderDirectory, Logger logger) {
        this(MinecraftDirectory.resolveFile(loaderDirectory), Objects.requireNonNull(logger, "logger cannot be null"),
                "Set the " + MinecraftDirectory.OVERRIDE_ENV + " environment variable to choose another.");
    }

    /**
     * Opens the store on exactly this file, with no environment override.
     *
     * @param file The credential file.
     * @param logger Where to report problems.
     * @return The store.
     * @throws NullPointerException When either argument is {@code null}.
     */
    public static SharedTokenStore atFile(Path file, Logger logger) {
        return new SharedTokenStore(Objects.requireNonNull(file, "file cannot be null"),
                Objects.requireNonNull(logger, "logger cannot be null"), "");
    }

    private SharedTokenStore(Path file, Logger logger, String unusableHint) {
        this.logger = logger;
        this.file = file;
        this.lock = LOCKS.computeIfAbsent(file.toAbsolutePath().normalize(), ignored -> new Object());

        if (!MinecraftDirectory.isUsable(file.getParent())) {
            logger.warning("The directory " + file.getParent()
                    + " is missing or not writable. Tokens cannot be saved there. " + unusableHint);
        }

        synchronized (lock) {
            reload();
            if (!Files.exists(file)) {
                writeTemplate();
            }
        }
    }

    /**
     * Moves a credential file from where core kept it before 0.14.0 to where it
     * keeps it now, when only the old one exists. Never throws: a failure is
     * logged, and the store then starts on an empty file at the new place.
     *
     * @param legacy Where the file was kept.
     * @param file Where it is kept now.
     * @param logger Where to report the move, or why it failed.
     * @return {@code true} when this call moved it.
     */
    public static boolean moveLegacyFile(Path legacy, Path file, Logger logger) {
        Objects.requireNonNull(logger, "logger cannot be null");
        try {
            if (ModFiles.moveIfAbsent(legacy, file)) {
                logger.info("Moved " + legacy + " to " + file + ".");
                return true;
            }
        } catch (IOException | RuntimeException e) {
            logger.warning("Could not move " + legacy + " to " + file + " (" + e.getMessage()
                    + "). Move it by hand to keep the tokens in it.");
        }
        return false;
    }

    /**
     * Returns the path of the shared file.
     *
     * @return The path, for a diagnostic message.
     */
    public Path file() {
        return file;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> load(String vendorCode) {
        if (vendorCode == null || vendorCode.isBlank()) {
            return List.of();
        }
        synchronized (lock) {
            return loadLocked(vendorCode);
        }
    }

    private List<String> loadLocked(String vendorCode) {
        JsonElement section = document.get(vendorCode.trim().toLowerCase(Locale.ROOT));
        if (section == null || !section.isJsonObject()) {
            return List.of();
        }

        JsonElement tokens = section.getAsJsonObject().get(TOKEN_FIELD);
        List<String> found = new ArrayList<>();

        boolean skipped = false;
        if (tokens != null && tokens.isJsonArray()) {
            for (JsonElement entry : tokens.getAsJsonArray()) {
                skipped |= !addIfUsable(found, entry);
            }
        } else {
            // A player who wrote one token without the brackets has made a
            // reasonable guess. Rejecting it would look like the token was bad.
            skipped = !addIfUsable(found, tokens);
        }
        if (skipped && warnedUnusable.add(vendorCode.trim().toLowerCase(Locale.ROOT))) {
            logger.warning("A " + vendorCode.trim().toLowerCase(Locale.ROOT) + " entry in " + file.getFileName()
                    + " is not a usable token (it holds a space or an unusual character) and was skipped.");
        }
        return found;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Re-reads before writing, so a credential another MCAgents mod added
     * since this store last loaded survives.</p>
     */
    @Override
    public boolean add(String vendorCode, String token) {
        if (vendorCode == null || token == null || !LlmCredentials.isWellFormedKey(token.trim())) {
            return false;
        }

        synchronized (lock) {
            reload();
            if (readFailed) {
                return false;
            }
            String vendor = vendorCode.trim().toLowerCase(Locale.ROOT);
            List<String> tokens = new ArrayList<>(loadLocked(vendor));
            if (tokens.contains(token.trim())) {
                return false;
            }

            tokens.add(token.trim());
            setTokens(vendor, tokens);
            return write();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Re-reads before writing, so a credential another MCAgents mod added
     * since this store last loaded is preserved rather than overwritten.</p>
     */
    @Override
    public void evict(String vendorCode, String token) {
        if (vendorCode == null || token == null) {
            return;
        }

        synchronized (lock) {
            reload();
            if (readFailed) {
                return;
            }

            String vendor = vendorCode.trim().toLowerCase(Locale.ROOT);
            List<String> remaining = new ArrayList<>(loadLocked(vendor));
            if (!remaining.remove(token.trim())) {
                // Already gone — another mod evicted it, most likely. Not an error,
                // and rewriting the file would be pointless work.
                return;
            }

            setTokens(vendor, remaining);
            if (write()) {
                logger.warning("A " + vendor + " token was rejected and has been removed from " + file.getFileName()
                        + ". " + remaining.size() + " token(s) remain for that platform.");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reload() {
        synchronized (lock) {
            reloadLocked();
        }
    }

    private void reloadLocked() {
        if (!Files.exists(file)) {
            document = new JsonObject();
            readFailed = false;
            return;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                throw new IllegalStateException("the top level is not an object");
            }
            document = parsed.getAsJsonObject();
            readFailed = false;
        } catch (IOException | JsonParseException | IllegalStateException e) {
            // A malformed file must not be overwritten — it holds the player's
            // keys, and rewriting it would destroy them. Report and carry on
            // with nothing loaded, and refuse every write until it reads.
            // The exception's own text is not logged: a parser quotes what it
            // choked on, and here that is a key.
            document = new JsonObject();
            readFailed = true;
            logger.warning(file.getFileName() + " could not be read (" + e.getClass().getSimpleName()
                    + "). Tokens will be treated as unset until it is fixed. The file has not been modified.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String describe() {
        return file.toString();
    }

    /**
     * Adds a JSON element to a list when it is a usable credential.
     *
     * @param target Where to add it.
     * @param entry The element to consider, possibly {@code null}.
     * @return {@code false} only when the entry held text that is not a usable
     *         key; an absent or blank entry is not worth a warning.
     */
    private boolean addIfUsable(List<String> target, JsonElement entry) {
        if (entry == null || !entry.isJsonPrimitive()) {
            return true;
        }
        String value = entry.getAsString();
        // A blank entry is a placeholder the player never filled in, not a
        // credential. Treating it as one would produce a rejection, which would
        // then delete the line they were about to edit.
        if (value == null || value.isBlank()) {
            return true;
        }
        // Not sent: the HTTP client would refuse it with a message quoting it.
        if (!LlmCredentials.isWellFormedKey(value.trim())) {
            return false;
        }
        if (!target.contains(value.trim())) {
            target.add(value.trim());
        }
        return true;
    }

    /**
     * Replaces a vendor's credential list in the in-memory document.
     *
     * @param vendorCode The vendor.
     * @param tokens The credentials to record.
     */
    private void setTokens(String vendorCode, List<String> tokens) {
        JsonArray array = new JsonArray();
        tokens.forEach(array::add);

        JsonElement existing = document.get(vendorCode);
        JsonObject section = existing != null && existing.isJsonObject()
                ? existing.getAsJsonObject()
                : new JsonObject();

        section.add(TOKEN_FIELD, array);
        document.add(vendorCode, section);
    }

    /**
     * Writes an empty file with a section per known vendor.
     *
     * <p>Discoverability: a player opening the file sees which platforms exist
     * and where a key goes, without needing documentation open beside it.</p>
     */
    private void writeTemplate() {
        if (readFailed) {
            return;
        }
        for (String vendor : KNOWN_VENDORS) {
            setTokens(vendor, List.of());
        }
        if (write()) {
            logger.info("Created " + file + ". Add an API token there, then run the chat reload command.");
        }
    }

    /**
     * Writes the in-memory document to disk atomically.
     *
     * <p>Via a temporary file in the same directory, moved into place. Several
     * MCAgents mods may hold this file open, and one of them reading a
     * half-written file would see no credentials and report them missing. The
     * temporary file is a sibling deliberately — a move across filesystems is
     * not atomic.</p>
     *
     * @return {@code true} when the file was written.
     */
    private boolean write() {
        Path directory = file.getParent();
        Path temporary = null;
        try {
            if (directory != null) {
                Files.createDirectories(directory);
            }
            temporary = Files.createTempFile(directory, "mcagents", ".tmp");

            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(document, writer);
            }
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                // Some filesystems cannot. A plain replace is still better than
                // losing the write.
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            restrictPermissions();
            return true;
        } catch (IOException e) {
            logger.warning("Could not write " + file + " (" + e.getMessage()
                    + "). Token changes will not survive a restart.");
            deleteQuietly(temporary);
            return false;
        }
    }

    /**
     * Narrows the file's permissions to its owner, where the platform supports
     * it.
     *
     * <p>Best effort. On Windows, and on any filesystem without POSIX
     * permissions, this does nothing — and that is not worth failing a write
     * over, since the alternative is losing the credential entirely.</p>
     */
    private void restrictPermissions() {
        try {
            if (Files.getFileStore(file).supportsFileAttributeView(java.nio.file.attribute.PosixFileAttributeView.class)) {
                Files.setPosixFilePermissions(file,
                        java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            }
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            logger.fine("Could not restrict permissions on " + file.getFileName() + ": " + e.getMessage());
        }
    }

    /**
     * Deletes a temporary file, ignoring failure.
     *
     * @param path The file to delete, possibly {@code null}.
     */
    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Leaving a stray temporary file is not worth a second error
            // message on top of the write failure already reported.
        }
    }
}
