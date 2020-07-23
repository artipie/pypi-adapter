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

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.pypi.NormalizedProjectName;
import io.reactivex.Single;
import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * Slice to redirect to normalized url.
 * @since 0.6
 */
public final class RedirectSlice implements Slice {

    /**
     * Full path header name.
     */
    private static final String HDR_FULL_PATH = "X-FullPath";

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final String rqline = new RequestLineFrom(line).uri().toString();
        final String last = rqline.split("/")[rqline.split("/").length - 1];
        return new AsyncResponse(
            Single.fromCallable(() -> last)
                .map(name -> new NormalizedProjectName.Simple(name).value())
                .map(
                    normalized -> new RqHeaders(headers, RedirectSlice.HDR_FULL_PATH).stream()
                    .findFirst()
                    .orElse(rqline).replaceAll(String.format("(%s\\/?)$", last), normalized)
                )
                .map(
                    url -> new RsWithHeaders(
                        new RsWithStatus(RsStatus.MOVED_PERMANENTLY),
                        new Headers.From("Location", url)
                    )
                )
        );
    }
}
