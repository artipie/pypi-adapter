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

import com.artipie.http.Connection;
import com.artipie.http.Response;
import com.artipie.http.rs.Header;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;

/**
 * HtmlIndexResponse produce and format html index of the pypi repository.
 *
 * @since 0.2
 * @todo #33:90min split {@link #send(Connection)} to 3 different stage of sending: HEADER,
 *  content of repository and FOOTER. There is no requrements to send it in one response body.
 *  But for tests purpose and speed up the development process i put it in 1 ByteBuffer now.
 */
final class HtmlIndexResponse  implements Response {

    /**
     * Header of the response HTML page.
     */
    private static final String HEADER = "<!DOCTYPE html>\n<html>\n  <body>";

    /**
     * Footer of the response HTML page.
     */
    private static final String FOOTER = "</body>\n</html>";

    /**
     * Buffer with html formated package list.
     */
    private final String packages;

    /**
     * Ctor.
     * @param packages Formated package list.
     */
    protected HtmlIndexResponse(final String packages) {
        this.packages = packages;
    }

    @Override
    public CompletionStage<Void> send(final Connection connection) {
        final ByteBuffer buff = ByteBuffer.allocate(
            HEADER.length()
            + this.packages.length()
            + FOOTER.length()
        );
        buff.put(HtmlIndexResponse.HEADER.getBytes(StandardCharsets.UTF_8))
            .put(this.packages.getBytes(StandardCharsets.UTF_8))
            .put(HtmlIndexResponse.FOOTER.getBytes(StandardCharsets.UTF_8));
        return new RsWithBody(
            new RsWithHeaders(
                new RsWithStatus(RsStatus.OK),
                new Header("Content-Type", "text/html")
            ),
            buff
        ).send(connection);
    }
}
