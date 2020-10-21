/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Artipie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.pypi.http;

import com.artipie.asto.Content;
import com.artipie.asto.cache.Cache;
import com.artipie.asto.cache.CacheControl;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.slice.KeyFromPath;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.StreamSupport;
import org.reactivestreams.Publisher;

/**
 * Slice that proxies request with given request line and empty headers and body,
 * caches and returns response from remote.
 * @since 0.7
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class ProxySlice implements Slice {

    /**
     * Origin.
     */
    private final Slice origin;

    /**
     * Cache.
     */
    private final Cache cache;

    /**
     * Ctor.
     * @param origin Origin
     * @param cache Cache
     */
    ProxySlice(final Slice origin, final Cache cache) {
        this.origin = origin;
        this.cache = cache;
    }

    @Override
    public Response response(
        final String line, final Iterable<Map.Entry<String, String>> ignored,
        final Publisher<ByteBuffer> pub
    ) {
        final AtomicReference<Headers> headers = new AtomicReference<>();
        final AtomicReference<RsStatus> status = new AtomicReference<>();
        return new AsyncResponse(
            this.cache.load(
                new KeyFromPath(new RequestLineFrom(line).uri().getPath()),
                () -> {
                    final CompletableFuture<Content> promise = new CompletableFuture<>();
                    this.origin.response(line, Headers.EMPTY, Content.EMPTY).send(
                        (rsstatus, rsheaders, rsbody) -> {
                            final CompletableFuture<Void> term = new CompletableFuture<>();
                            headers.set(rsheaders);
                            status.set(rsstatus);
                            if (rsstatus.success()) {
                                final Flowable<ByteBuffer> body = Flowable.fromPublisher(rsbody)
                                    .doOnError(term::completeExceptionally)
                                    .doOnTerminate(() -> term.complete(null));
                                promise.complete(new Content.From(body));
                            } else {
                                promise.completeExceptionally(
                                    new IllegalStateException(
                                        String.format(
                                            "Unsuccessful response code %s received",
                                            rsstatus.code()
                                        )
                                    )
                                );
                            }
                            return term;
                        }
                    );
                    return promise;
                },
                CacheControl.Standard.ALWAYS
            ).handle(
                (content, throwable) -> {
                    final CompletableFuture<Response> result = new CompletableFuture<>();
                    if (throwable == null) {
                        result.complete(
                            new RsFull(
                                RsStatus.OK,
                                new Headers.From(ProxySlice.contentType(headers.get(), line)),
                                content
                            )
                        );
                    } else {
                        result.complete(
                            new RsWithHeaders(new RsWithStatus(status.get()), headers.get())
                        );
                    }
                    return result;
                }
            ).thenCompose(Function.identity())
        );
    }

    /**
     * Obtains content-type from remote's headers or trays to guess it by request line.
     * @param headers Header
     * @param line Request line
     * @return Cleaned up headers.
     */
    private static Header contentType(final Headers headers, final String line) {
        final String name = "content-type";
        return StreamSupport.stream(headers.spliterator(), false)
            .filter(header -> header.getKey().equalsIgnoreCase(name))
            .findFirst().map(Header::new).orElseGet(
                () -> {
                    Header res = new Header(name, "text/html");
                    if (new RequestLineFrom(line).uri().toString()
                        .matches(".*\\.(whl|tar\\.gz|zip|tar\\.bz2|tar\\.Z|tar|egg)")) {
                        res = new Header(name, "multipart/form-data");
                    }
                    return res;
                }
            );
    }
}
