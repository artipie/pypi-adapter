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
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * BinaryArtifact.
 *
 * @since 0.1
 */
public final class BinaryArtifact implements Artifact {

    /**
     * Name of artifact.
     */
    private final String name;

    /**
     * Content of artifact.
     */
    private final Flow<ByteBuffer> content;

    /**
     * Ctor.
     *
     * @param name Name of artifact.
     * @param content Content of artifact.
     */
    public BinaryArtifact(final String name, final String content) {
        this.name = name;
        this.content = new ByteFlow(content);
    }

    @Override
    public CompletableFuture<Void> save(final Storage storage) {
        final String pkgname = this.name.split("-\\d.\\d.\\d.tar.gz")[0];
        return storage.save(new Key.From(pkgname, this.name), this.content.value());
    }
}
