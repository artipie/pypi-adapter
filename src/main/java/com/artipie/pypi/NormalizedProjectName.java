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

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalized python project name from uploading filename.
 * <p>
 * According to
 * <a href="https://www.python.org/dev/peps/pep-0503/#normalized-names">PEP-503</a> only valid
 * characters in a name are the ASCII alphabet, ASCII numbers, ., -, and _. The name should be
 * lowercased with all runs of the characters ., -, or _ replaced with a single - character.
 * <p>
 * Implementations of this interface should return normalized project name
 * in {@link NormalizedProjectName#value()} method.
 *
 * @since 0.6
 */
public interface NormalizedProjectName {

    /**
     * Python project name.
     * @return String value
     */
    String value();

    /**
     * Normalized python project name from uploading filename.
     *
     * Uploaded file name has the following format [project_name]-[version].[extension] where
     * {@code project_name} and {@code version} are separated by -, extension can be either tar.gz
     * or whl. {@code project_name} can contain ASCII alphabet, ASCII numbers, ., -, and _. This
     * class extracts {@code project_name} and normalise it by replacing ., -, or _ with a single -
     * and making all characters lowecase.
     *
     * @since 0.6
     */
    final class FromFilename implements NormalizedProjectName {

        /**
         * Pattern to obtain package name from uploaded file name: for file name
         * 'Artipie-Testpkg-0.0.3.tar.gz', then package name is 'Artipie-Testpkg'.
         */
        private static final Pattern PKG_NAME = Pattern.compile("(.*)-.*");

        /**
         * File name.
         */
        private final String filename;

        /**
         * Ctor.
         * @param filename Filename
         */
        public FromFilename(final String filename) {
            this.filename = filename;
        }

        @Override
        public String value() {
            final Matcher matcher = FromFilename.PKG_NAME.matcher(this.filename);
            if (matcher.matches()) {
                return matcher.group(1).replaceAll("[-_.]+", "-").toLowerCase(Locale.US);
            }
            throw new IllegalArgumentException(
                // @checkstyle LineLengthCheck (1 line)
                "Python project filename must have format <project_name>-<version>.whl or <project_name>-<version>.tar.gz"
            );
        }
    }

}
