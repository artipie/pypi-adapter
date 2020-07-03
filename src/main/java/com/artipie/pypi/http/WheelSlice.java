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
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import org.reactivestreams.Publisher;

/**
 * WheelSlice save and manage whl and tgz entries.
 *
 * @since 0.2
 */
final class WheelSlice implements Slice {

    /**
     * The Storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     *
     * @param storage Storage.
     */
    WheelSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> iterable,
        final Publisher<ByteBuffer> publisher
    ) {
        return new AsyncResponse(
            this.storage.save(
                new Key.From(this.name(line)),
                new Multipart(iterable, publisher).content()
            ).thenApply(ignored -> new RsWithStatus(RsStatus.CREATED))
        );
    }

    /**
     * Generate file name for temp file.
     *
     * @param line Params from request.
     * @return Temp File name.
     * @checkstyle NonStaticMethodCheck (500 lines).
     */
    private String name(final String line) {
        return String.format("tmp-%s-%s.tar.gz", line, UUID.randomUUID().toString());
    }
}
