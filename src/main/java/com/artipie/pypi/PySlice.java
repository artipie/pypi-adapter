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
import com.artipie.http.Slice;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.*;

import java.util.regex.Pattern;

/**
 * PySlice.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class PySlice extends Slice.Wrap {

    /**
     * Content type.
     */
    public static final String TEXT_HTML = "text/html";

    /**
     * Constant for content type.
     */
    public static final String CONTENT_TYPE = "content-type";

    /**
     * Ctor.
     *
     * @param storage Storage storage.
     * @checkstyle UnusedFormalParameter (4 lines)
     */
    public PySlice(final Storage storage) {
        super(
            new SliceRoute(
                new SliceRoute.Path(
                    new RtRule.ByMethod(RqMethod.GET),
                    new SliceDownload(storage)
                ),
                PySlice.pathGet(
                    "^[a-zA-Z0-9]*.*\\.whl",
                    new SliceWithHeaders(
                        new LoggingSlice(new SliceDownload(storage)),
                        new Headers.From(PySlice.CONTENT_TYPE, PySlice.TEXT_HTML)
                    )
                ),
                PySlice.pathGet(
                    "^[a-zA-Z0-9]*.*\\.gz",
                    new SliceWithHeaders(
                        new LoggingSlice(new SliceDownload(storage)),
                        new Headers.From(PySlice.CONTENT_TYPE, PySlice.TEXT_HTML)
                    )
                ),
                new SliceRoute.Path(
                        new RtRule.ByMethod(RqMethod.POST),
                        PySlice.uploadPackage(storage)
                ),
                new SliceRoute.Path(
                    RtRule.FALLBACK,
                    new SliceSimple(
                        new RsWithStatus(RsStatus.  NOT_FOUND)
                    )
                )
            )
        );
    }

    /**
     * Ctor.
     *
     * @param storage Storage.
     */
    private static Slice uploadPackage(Storage storage) {
        return new WhelSlice(storage);
    }

    /**
     * This method simply encapsulates all the RtRule instantiations.
     *
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
