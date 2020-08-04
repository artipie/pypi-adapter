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

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link ValidFilename}.
 * @since 0.6
 * @checkstyle ParameterNumberCheck (500 lines)
 */
class ValidFilenameTest {

    @ParameterizedTest
    @CsvSource({
        "my-project,0.3,my-project-0.3.tar,true",
        "my-project,0.3,my-project-0.4.tar,false",
        "Another_project,123.93,Another_project-123.93.zip,true",
        "very-difficult-project,1.0a2,very-difficult-project-1.0a2-py3-any-none.whl,true",
        "one,0.0.1,two-0.2.tar.gz,false"
    })
    void checks(final String name, final String version, final String filename, final boolean res) {
        MatcherAssert.assertThat(
            new ValidFilename(
                new PackageInfo.FromMetadata(this.metadata(name, version)), filename
            ).valid(),
            new IsEqual<>(res)
        );
    }

    private String metadata(final String name, final String version) {
        return String.join(
            "\n",
            String.format("Name: %s", name),
            String.format("Version: %s", version)
        );
    }

}
