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
package com.artipie.pypi.meta;

import com.artipie.pypi.NormalizedProjectName;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Python package valid filename.
 * @since 0.6
 */
public final class ValidFilename {

    /**
     * Pattern to obtain package name from uploaded file name: for file name
     * 'Artipie-Testpkg-0.0.3.tar.gz', then package name is 'Artipie-Testpkg'.
     */
    private static final Pattern ARCHIVE_PTRN =
        Pattern.compile("(?<name>.*)-(?<version>[0-9a-z.]+?)\\.([a-zA-Z.]+)");

    /**
     * Python wheel package name pattern, for more details see
     * <a href="https://www.python.org/dev/peps/pep-0427/#file-name-convention">docs</a>.
     */
    private static final Pattern WHEEL_PTRN =
        Pattern.compile("(?<name>.*?)-(?<version>[0-9a-z.]+)(-\\d+)?-((py\\d.?)+)-(.*)-(.*).whl");

    /**
     * Package info data.
     */
    private final PackageInfo data;

    /**
     * Filename.
     */
    private final String filename;

    /**
     * Ctor.
     * @param data Package info data
     * @param filename Filename
     */
    public ValidFilename(final PackageInfo data, final String filename) {
        this.data = data;
        this.filename = filename;
    }

    /**
     * Is filename valid?
     * @return True if filename corresponds to project metadata, false - otherwise.
     */
    public boolean valid() {
        final Matcher matcher;
        if (this.filename.endsWith("whl")) {
            matcher = ValidFilename.WHEEL_PTRN.matcher(this.filename);
        } else {
            matcher = ValidFilename.ARCHIVE_PTRN.matcher(this.filename);
        }
        final String name = new NormalizedProjectName.Simple(this.data.name()).value();
        return matcher.matches()
            && name.equals(new NormalizedProjectName.Simple(matcher.group("name")).value())
            && this.data.version().equals(matcher.group("version"));
    }

}
