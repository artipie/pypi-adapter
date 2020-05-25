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

package com.artipie.pypi;

import com.artipie.asto.Storage;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.http.slice.SliceDownload;
import com.artipie.http.slice.SliceSimple;
import com.artipie.http.slice.SliceWithHeaders;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * PySlice.
 *
 * @since 0.1
 */
public final class PySlice implements Slice {

    /**
     * Base path.
     */
    private final String base;

    /**
     * Storage for packages.
     */
    private final Storage storage;

    /**
     * Origin.
     */
    private final Slice origin;

    /**
     * Ctor.
     *
     * @param base    Base path.
     * @param storage Storage storage.
     */
    public PySlice(final String base, final Storage storage) {
        this.base = base;
        this.storage = storage;
        this.origin = new SliceRoute(
                new SliceRoute.Path(
                        new RtRule.ByMethod(RqMethod.GET),
                        new SliceDownload(storage)
                ),
                PySlice.pathGet(".+/@v/v.*\\.whl", PySlice.createSlice(storage, "text/html")),
                PySlice.pathGet(".+/@v/v.*\\.gz", PySlice.createSlice(storage, "text/html")),
                PySlice.pathGet("/simple/artipietestpkg/", PySlice.createSlice(storage, "text/html")),
                new SliceRoute.Path(
                        RtRule.FALLBACK,
                        new SliceSimple(
                                new RsWithStatus(RsStatus.NOT_FOUND)
                        )
                )
        );

    }

    @Override
    public Response response(
            final String line, final Iterable<Map.Entry<String, String>> headers,
            final Publisher<ByteBuffer> body) {
        return this.origin.response(line, headers, body);
    }

    /**
     * Creates slice instance.
     * @param storage Storage
     * @param type Content-type
     * @return Slice
     */
    private static Slice createSlice(final Storage storage, final String type) {
        return new SliceWithHeaders(
                new LoggingSlice( new SliceDownload(storage) ),
                new Headers.From("content-type", type)
        );
    }

    /**
     * This method simply encapsulates all the RtRule instantiations.
     * @param pattern Route pattern
     * @param slice Slice implementation
     * @return Path route slice
     */
    private static SliceRoute.Path pathGet(final String pattern, final Slice slice) {
        return new SliceRoute.Path(
                new RtRule.Multiple(
                        new RtRule.ByPath(Pattern.compile(pattern)),
                        new RtRule.ByMethod(RqMethod.GET)
                ),
                new LoggingSlice(slice)
        );
    }
}
