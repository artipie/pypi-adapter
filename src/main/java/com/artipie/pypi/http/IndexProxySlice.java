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
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.slice.KeyFromPath;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;

/**
 * Slice that proxies request with given request line and empty headers and body.
 * @since 0.7
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class IndexProxySlice implements Slice {

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
    IndexProxySlice(final Slice origin, final Cache cache) {
        this.origin = origin;
        this.cache = cache;
    }

    @Override
    public Response response(
        final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> pub
    ) {
        final CompletableFuture<Response> res = new CompletableFuture<>();
        this.cache.load(
            new KeyFromPath(new RequestLineFrom(line).uri().getPath()),
            () -> {
                final CompletableFuture<Content> promise = new CompletableFuture<>();
                this.origin.response(line, Headers.EMPTY, Content.EMPTY).send(
                    (status, rsheaders, rsbody) -> {
                        final CompletableFuture<Void> term = new CompletableFuture<>();
                        if (status.success()) {
                            final Flowable<ByteBuffer> body = Flowable.fromPublisher(rsbody)
                                .doOnError(term::completeExceptionally)
                                .doOnTerminate(() -> term.complete(null));
                            promise.complete(new Content.From(body));
                        } else {
                            promise.completeExceptionally(
                                new IllegalStateException(
                                    String.format(
                                        "Unsuccessful response code %s received",
                                        status.code()
                                    )
                                )
                            );
                            res.complete(new RsWithHeaders(new RsWithStatus(status), rsheaders));
                        }
                        return term;
                    }
                );
                return promise;
            },
            CacheControl.Standard.ALWAYS
        ).thenApply(content -> res.complete(new RsWithBody(StandardRs.OK, content)));
        return new AsyncResponse(res);
    }
}
