package io.github.mcagentsmods.core.common.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

/**
 * The two bounds the JDK's HTTP client does not put on a response.
 *
 * <ul>
 *   <li><strong>Size.</strong> {@code BodyHandlers.ofString()} buffers a body of any size
 *       onto the heap. A provider or a proxy that answers with gigabytes takes the game down
 *       with it. {@link #ofString(int)} stops reading past a limit.</li>
 *   <li><strong>Time.</strong> {@code HttpRequest.timeout} stops counting once the headers
 *       arrive, so a server that sends headers and then stalls leaves the future pending
 *       forever. {@link #withDeadline(CompletableFuture, Duration)} bounds the whole
 *       exchange.</li>
 * </ul>
 */
public final class HttpLimits {

    private HttpLimits() {
        throw new AssertionError("No instances.");
    }

    /**
     * Reads the body as UTF-8 text, failing with an {@link IOException} — and cancelling the
     * download — once it passes {@code maxBytes}.
     */
    public static HttpResponse.BodyHandler<String> ofString(int maxBytes) {
        if (maxBytes < 1) {
            throw new IllegalArgumentException("maxBytes must be positive");
        }
        return info -> new LimitedSubscriber(maxBytes);
    }

    /**
     * Cancels {@code exchange} if it has not finished within {@code deadline}.
     *
     * <p>Cancelling the future the JDK client returned aborts the request, so the connection
     * is released rather than left to a server that stopped answering.
     *
     * @return {@code exchange}, for chaining
     */
    public static <T> CompletableFuture<T> withDeadline(CompletableFuture<T> exchange,
                                                        Duration deadline) {
        Objects.requireNonNull(exchange, "exchange");
        Objects.requireNonNull(deadline, "deadline");
        CompletableFuture.delayedExecutor(deadline.toMillis(), TimeUnit.MILLISECONDS)
                .execute(() -> exchange.cancel(true));
        return exchange;
    }

    private static final class LimitedSubscriber implements HttpResponse.BodySubscriber<String> {

        private final int maxBytes;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final CompletableFuture<String> result = new CompletableFuture<>();
        private Flow.Subscription subscription;

        LimitedSubscriber(int maxBytes) {
            this.maxBytes = maxBytes;
        }

        @Override
        public CompletionStage<String> getBody() {
            return result;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(List<ByteBuffer> items) {
            if (result.isDone()) {
                return;
            }
            for (ByteBuffer item : items) {
                if (buffer.size() + item.remaining() > maxBytes) {
                    subscription.cancel();
                    result.completeExceptionally(new IOException(
                            "The response was larger than " + maxBytes + " bytes"));
                    return;
                }
                byte[] chunk = new byte[item.remaining()];
                item.get(chunk);
                buffer.writeBytes(chunk);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            result.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            result.complete(buffer.toString(StandardCharsets.UTF_8));
        }
    }
}
