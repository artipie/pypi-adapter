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

import java.util.stream.Stream;

/**
 * Python package info.
 * @since 0.6
 */
public interface PackageInfo {

    /**
     * Package name.
     * @return Name of the project
     */
    String name();

    /**
     * Package version.
     * @return Version of the project
     */
    String version();

    /**
     * Implementation of {@link PackageInfo} that parses python metadata PKG-INFO file to obtain
     * required information. For more details see
     * <a href="https://www.python.org/dev/peps/pep-0314/">PEP-314</a>.
     * @since 0.6
     */
    final class FromMetadata implements PackageInfo {

        /**
         * Input.
         */
        private final String input;

        /**
         * Ctor.
         * @param input Input
         */
        public FromMetadata(final String input) {
            this.input = input;
        }

        @Override
        public String name() {
            return this.read("Name");
        }

        @Override
        public String version() {
            return this.read("Version");
        }

        /**
         * Reads header value by name.
         * @param header Header name
         * @return Header value
         */
        private String read(final String header) {
            final String name = String.format("%s:", header);
            return Stream.of(this.input.split("\n"))
                .filter(line -> line.startsWith(name)).findFirst()
                .map(line ->  line.replace(name, "").trim())
                .orElseThrow(
                    () -> new IllegalArgumentException(
                        String.format("Invalid metadata file, header %s not found", header)
                    )
                );
        }
    }
}
