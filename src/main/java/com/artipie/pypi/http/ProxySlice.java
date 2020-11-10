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
import com.artipie.asto.Key;
import com.artipie.asto.cache.Cache;
import com.artipie.asto.cache.CacheControl;
import com.artipie.asto.cache.Remote;
import com.artipie.asto.ext.KeyLastPart;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.pypi.NormalizedProjectName;
import io.reactivex.Flowable;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
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
     * Python artifacts formats.
     */
    private static final String FORMATS = ".*\\.(whl|tar\\.gz|zip|tar\\.bz2|tar\\.Z|tar|egg)";

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
        return new AsyncResponse(
            this.cache.load(
                ProxySlice.keyFromPath(line),
                new Remote.WithErrorHandling(
                    () -> {
                        final CompletableFuture<Optional<? extends Content>> promise =
                            new CompletableFuture<>();
                        this.origin.response(line, Headers.EMPTY, Content.EMPTY).send(
                            (rsstatus, rsheaders, rsbody) -> {
                                final CompletableFuture<Void> term = new CompletableFuture<>();
                                headers.set(rsheaders);
                                if (rsstatus.success()) {
                                    final Flowable<ByteBuffer> body = Flowable.fromPublisher(rsbody)
                                        .doOnError(term::completeExceptionally)
                                        .doOnTerminate(() -> term.complete(null));
                                    promise.complete(Optional.of(new Content.From(body)));
                                } else {
                                    promise.complete(Optional.empty());
                                }
                                return term;
                            }
                        );
                        return promise;
                    }
                ),
                CacheControl.Standard.ALWAYS
            ).handle(
                (content, throwable) -> {
                    final CompletableFuture<Response> result = new CompletableFuture<>();
                    if (throwable == null && content.isPresent()) {
                        result.complete(
                            new RsFull(
                                RsStatus.OK,
                                new Headers.From(ProxySlice.contentType(headers.get(), line)),
                                content.get()
                            )
                        );
                    } else {
                        result.complete(new RsWithStatus(RsStatus.NOT_FOUND));
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
                    if (new RequestLineFrom(line).uri().toString().matches(ProxySlice.FORMATS)) {
                        res = new Header(name, "multipart/form-data");
                    }
                    return res;
                }
            );
    }

    /**
     * Obtains key from request line with names normalization.
     * @param line Request line
     * @return Instance of {@link Key}.
     */
    private static Key keyFromPath(final String line) {
        final URI uri = new RequestLineFrom(line).uri();
        Key res = new KeyFromPath(uri.getPath());
        if (!uri.toString().matches(ProxySlice.FORMATS)) {
            final String last = new KeyLastPart(res).get();
            res = new Key.From(
                res.string().replaceAll(
                    String.format("%s$", last), new NormalizedProjectName.Simple(last).value()
                )
            );
        }
        return res;
    }
}
