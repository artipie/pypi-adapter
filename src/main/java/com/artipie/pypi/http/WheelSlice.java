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
import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.slice.KeyFromPath;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * WheelSlice save and manage whl and tgz entries.
 *
 * @since 0.2
 */
final class WheelSlice implements Slice {

    /**
     * Pattern to obtain package name from uploaded file name: for file name
     * 'artipietestpkg-0.0.3.tar.gz', then package name is 'artipietestpkg'.
     */
    private static final Pattern PKG_NAME = Pattern.compile("(.*?)-.*");

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
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> iterable,
        final Publisher<ByteBuffer> publisher
    ) {
        return new AsyncResponse(
            new Multipart(iterable, publisher).content().thenApply(
                data -> this.storage.save(
                    new Key.From(
                        new KeyFromPath(new RequestLineFrom(line).uri().toString()),
                        WheelSlice.key(data.fileName())
                    ),
                    new Content.From(data.bytes())
                )
            ).handle(
                (ignored, throwable) -> {
                    Response res = new RsWithStatus(RsStatus.CREATED);
                    if (throwable != null) {
                        res = new RsWithStatus(RsStatus.BAD_REQUEST);
                    }
                    return res;
                }
            )
        );
    }

    /**
     * Key from filename.
     * @param filename Filename
     * @return Storage key
     */
    private static Key key(final String filename) {
        final Matcher matcher = PKG_NAME.matcher(filename);
        if (matcher.matches()) {
            return new Key.From(matcher.group(1), filename);
        }
        throw new IllegalArgumentException("Invalid file");
    }
}
