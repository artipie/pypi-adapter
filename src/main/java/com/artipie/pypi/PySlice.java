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
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsWithStatus;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.util.Map;
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
     * Ctor.
     *
     * @param base Base path.
     * @param storage Storage storage.
     */
    public PySlice(final String base, final Storage storage) {
        this.base = base;
        this.storage = storage;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> iterable,
        final Publisher<ByteBuffer> publisher) {
        final Response response;
        final RequestLineFrom request = new RequestLineFrom(line);
        final String path = request.uri().getPath();
        if (path.startsWith(this.base)) {
            final String relative = path.substring(this.base.length());
            final Resource resource = new StaticContent(relative, this.storage);
            if (request.method().equals("GET")) {
                response = resource.get();
            } else {
                response = new RsWithStatus(HttpURLConnection.HTTP_BAD_METHOD);
            }
        } else {
            response = new RsWithStatus(HttpURLConnection.HTTP_NOT_FOUND);
        }
        return response;
    }
}
