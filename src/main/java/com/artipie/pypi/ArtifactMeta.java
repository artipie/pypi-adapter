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

/**
 * ArtifactMeta.
 *
 * @since 0.1
 */
public final class ArtifactMeta implements Meta {
    /**
     * Name of artifact.
     */
    private final String name;

    /**
     * Version of artifact.
     */
    private final String version;

    /**
     * Pattern of html presentation of package meta info.
     */
    private final String pattern;

    /**
     * Ctor.
     *
     * @param name Name of artifact.
     * @param version Version of artifact.
     */
    public ArtifactMeta(final String name, final String version) {
        this(name, version, "<tr><td><a href=\"%s-%s.tar.gz\">%s-%s.tar.gz</a></td></tr>");
    }

    /**
     * Ctor.
     *
     * @param name Name of artifact.
     * @param version Version of artifact.
     */
    public ArtifactMeta(final String name, final String version, final String pattern) {
        this.name = name;
        this.version = version;
        this.pattern = pattern;
    }

    @Override
    public String html() {
        return String.format(this.pattern, this.name, this.version, this.name, this.version);
    }
}
