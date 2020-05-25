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
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
        return null;
    }
    /*@Override
    public Response get() {
        return connection -> CompletableFuture
            .supplyAsync(this::key)
            .thenCompose(
                key -> this.storage.exists(key).thenCompose(
                    exists -> {
                        final CompletionStage<Void> sent;
                        if (exists) {
                            sent = this.storage.value(key).thenCompose(
                                data -> connection.accept(
                                    RsStatus.OK,
                                    Collections.emptyList(),
                                    data
                                )
                            );
                        } else {
                            sent = new RsWithStatus(RsStatus.NOT_FOUND).send(connection);
                        }
                        return sent;
                    }
                )
            );
    }*/

    /**
     * Builds key to storage value from path.
     *
     * @return Key to storage value.
     * @todo #28:15m/DEV Extract it into another class
     */
    private Key.From key() {
        final String normalized;
        if (this.path.charAt(0) == '/') {
            normalized = this.path.substring(1);
        } else {
            normalized = this.path;
        }
        return new Key.From(normalized);
    }
}
