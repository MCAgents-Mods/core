package io.github.mcagentsmods.core.common.file;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Moves a mod's files from where an older version kept them into the mod's own directory.
 *
 * <p>Every MCAgents mod keeps its files in {@code {game_dir}/MCAgents/{mod}/}. A mod that kept a
 * file elsewhere before calls {@link #moveIfAbsent(Path, Path)} once at start-up, before it opens
 * the file, so a player who updates keeps what they had:
 *
 * <pre>
 * ModFiles.moveIfAbsent(gameDirectory.resolve("MCAgents/data.db"), paths.database());
 * </pre>
 *
 * <p>A file already at the new place is never overwritten: it is what the player has used since
 * the move, and the old one is left where it is.
 */
public final class ModFiles {

    private ModFiles() {
        throw new AssertionError("No instances.");
    }

    /**
     * Moves {@code legacy} to {@code target} when {@code legacy} exists and {@code target} does
     * not, creating {@code target}'s parent directories. Works for a file or a whole directory on
     * the same disk. Doing nothing is not an error, so it is safe to call on every start.
     *
     * @param legacy where an older version kept the file
     * @param target where the mod keeps it now
     * @return whether this call moved it
     * @throws IOException if the move was needed and failed; {@code legacy} is then untouched
     */
    public static boolean moveIfAbsent(Path legacy, Path target) throws IOException {
        Objects.requireNonNull(legacy, "legacy");
        Objects.requireNonNull(target, "target");
        if (!Files.exists(legacy, LinkOption.NOFOLLOW_LINKS)
                || Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try {
            Files.move(legacy, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(legacy, target);
        }
        return true;
    }

    /**
     * Deletes {@code directory} if it is an empty directory, such as the one a move left behind.
     *
     * @return whether this call deleted it
     * @throws IOException if it was empty and could not be deleted
     */
    public static boolean deleteIfEmpty(Path directory) throws IOException {
        Objects.requireNonNull(directory, "directory");
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        try (var entries = Files.list(directory)) {
            if (entries.findAny().isPresent()) {
                return false;
            }
        }
        try {
            return Files.deleteIfExists(directory);
        } catch (DirectoryNotEmptyException e) {
            return false;
        }
    }
}
