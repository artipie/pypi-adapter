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

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.slice.KeyFromPath;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;

/**
 * SliceIndex returns formatted html output with index of repository packages.
 *
 * @since 0.2
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class SliceIndex implements Slice {

    /**
     * Artipie artifacts storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Storage
     */
    SliceIndex(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> publisher
    ) {
        final Key rqkey = new KeyFromPath(new RequestLineFrom(line).uri().toString());
        return new AsyncResponse(
            this.storage.list(rqkey)
                .thenApply(
                    list -> list.stream()
                        .map(
                            key ->
                                String.format(
                                    "<a href=\"/%s\">%s</a><br/>", key.string(),
                                    SliceIndex.filename(key)
                                )
                        )
                        .collect(Collectors.joining())
                ).thenApply(
                    list -> new RsWithBody(
                        new RsWithHeaders(
                            new RsWithStatus(RsStatus.OK),
                            new ContentType("text/html")
                        ),
                        String.format(
                            "<!DOCTYPE html>\n<html>\n  </body>\n%s\n</body>\n</html>", list
                        ),
                        StandardCharsets.UTF_8
                    )
                )
        );
    }

    /**
     * Returns filename, last part of storage key.
     * @param stkey Storage key
     * @return Key part
     */
    private static String filename(final Key stkey) {
        final String[] parts = stkey.string().split("/");
        return parts[parts.length - 1];
    }
}
