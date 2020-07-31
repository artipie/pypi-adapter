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
import com.artipie.pypi.NormalizedProjectName;
import com.artipie.pypi.meta.Metadata;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.reactivestreams.Publisher;

/**
 * WheelSlice save and manage whl and tgz entries.
 *
 * @since 0.2
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
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
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> iterable,
        final Publisher<ByteBuffer> publisher
    ) {
        return new AsyncResponse(
            new Multipart(iterable, publisher).content().thenCompose(
                data -> {
                    final Path path = WheelSlice.save(data.bytes());
                    return this.storage.save(
                        new Key.From(
                            new KeyFromPath(new RequestLineFrom(line).uri().toString()),
                            new NormalizedProjectName.Simple(
                                new Metadata.FromArchive(path, data.fileName()).read().name()
                            ).value(),
                            data.fileName()
                        ),
                        new Content.From(data.bytes())
                    ).thenApply(nothing -> path);
                }
            ).handle(
                (path, throwable) -> {
                    Response res = new RsWithStatus(RsStatus.CREATED);
                    if (throwable != null) {
                        res = new RsWithStatus(RsStatus.BAD_REQUEST);
                    }
                    if (path != null) {
                        FileUtils.deleteQuietly(path.toFile());
                    }
                    return res;
                }
            )
        );
    }

    /**
     * Saves data to temp file.
     * @param data Data
     * @return Path to the temp file
     */
    private static Path save(final byte[] data) {
        try {
            final Path tmp = Files.createTempFile("py-artifact-", "");
            Files.write(tmp, data);
            return tmp;
        } catch (final IOException err) {
            throw new UncheckedIOException(err);
        }
    }
}
