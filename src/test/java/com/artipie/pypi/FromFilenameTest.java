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

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link NormalizedProjectName.FromFilename}.
 * @since 0.6
 */
class FromFilenameTest {

    @Test
    void throwsExceptionOnInvalidName() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new NormalizedProjectName.FromFilename("abc.txt").value()
        );
    }

    @ParameterizedTest
    @CsvSource({
        "abc-0.1.tar.gz,abc",
        "abc-123-0.2.tar.gz,abc-123",
        "My_Perfect_Python-1.0.tar.gz,my-perfect-python",
        "some._-Project-0.0.2.tar.gz,some-project",
        "0Ther--Pr0ject-0.2.3-py2-none-any.whl.whl,0ther-pr0ject",
        "simple-0-1-0-py3-cp33m-linux_x86.whl.whl,simple-0"
    })
    void normalisesNames(final String filename, final String normalized) {
        MatcherAssert.assertThat(
            new NormalizedProjectName.FromFilename(filename).value(),
            new IsEqual<>(normalized)
        );
    }
}
