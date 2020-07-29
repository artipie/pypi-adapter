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

import org.cactoos.io.InputStreamOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.llorllale.cactoos.matchers.MatcherOf;

/**
 * Test for {@link com.artipie.pypi.meta.PackageInfo.FromMetadata}.
 * @since 0.6
 * @checkstyle LeftCurlyCheck (500 lines)
 */
class PackageInfoFromMetadataTest {

    @ParameterizedTest
    @CsvSource({
        "my-project,0.3",
        "Another project,123-93",
        "Very-very-difficult project,3"
    })
    void readsMetadata(final String name, final String version) {
        MatcherAssert.assertThat(
            new PackageInfo.FromMetadata(new InputStreamOf(this.metadata(name, version))),
            Matchers.allOf(
                new MatcherOf<>(info -> { return version.equals(info.version()); }),
                new MatcherOf<>(info -> { return name.equals(info.name()); })
            )
        );
    }

    @Test
    void throwsExceptionIfInfoNotFound() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new PackageInfo.FromMetadata(new InputStreamOf("some text")).name()
        );
    }

    private String metadata(final String name, final String version) {
        return String.join(
            "\n",
            "Metadata-Version: 2.1",
            String.format("Name: %s", name),
            String.format("Version: %s", version),
            "Summary: A sample Python project",
            "Author: Someone",
            "Author-email: someone@example.com"
        );
    }

}
