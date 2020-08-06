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
     * Simple {@link NormalizedProjectName} implementation: normalise given name
     * by replacing ., -, or _ with a single - and making all characters lowecase. Name can contain
     * ASCII alphabet, ASCII numbers, ., -, and _.
     * @since 0.6
     */
    final class Simple implements NormalizedProjectName {

        /**
         * Pattern to verify the name.
         */
        private static final Pattern VERIFY = Pattern.compile("[A-Za-z0-9.\\-_]+");

        /**
         * Name to normalize.
         */
        private final String name;

        /**
         * Ctor.
         * @param name Name to normalize
         */
        public Simple(final String name) {
            this.name = name;
        }

        @Override
        public String value() {
            if (Simple.VERIFY.matcher(this.name).matches()) {
                return this.name.replaceAll("[-_.]+", "-").toLowerCase(Locale.US);
            }
            throw new IllegalArgumentException(
                "Invalid name: python project should match [A-Za-z0-9.-_]+"
            );
        }
    }

}
