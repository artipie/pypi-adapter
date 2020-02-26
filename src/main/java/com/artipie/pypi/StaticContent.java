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

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.rs.RsWithStatus;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * StaticContent.
 *
 * @since 0.1
 */
public final class StaticContent implements Resource {
    /**
     * Resource path.
     */
    private final String path;

    /**
     * Storage to read content from.
     */
    private final Storage storage;

    /**
     * Ctor.
     *
     * @param path Resource path.
     * @param storage Storage to read content from.
     */
    public StaticContent(final String path, final Storage storage) {
        this.path = path;
        this.storage = storage;
    }

    @Override
    public Response get() {
        Response response;
        try {
            response = this.getAsync().get();
        } catch (final InterruptedException | ExecutionException ex) {
            response = new RsWithStatus(HttpURLConnection.HTTP_INTERNAL_ERROR);
        }
        return response;
    }

    /**
     * Serve GET method async.
     *
     * @return Response to request.
     */
    private Future<Response> getAsync() {
        return CompletableFuture
            .supplyAsync(() -> new Key.From(this.path))
            .thenCompose(
                key -> this.storage.exists(key).thenCompose(
                    exists -> {
                        final CompletionStage<Response> response;
                        if (exists) {
                            response = this.storage.value(key).thenApplyAsync(
                                data -> connection -> connection.accept(
                                    HttpURLConnection.HTTP_OK,
                                    Collections.emptyList(),
                                    data
                                )
                            );
                        } else {
                            response = CompletableFuture.completedFuture(
                                new RsWithStatus(HttpURLConnection.HTTP_NOT_FOUND)
                            );
                        }
                        return response;
                    }
                )
            );
    }
}
