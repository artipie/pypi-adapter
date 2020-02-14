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
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * PackagesMetaTest.
 *
 * @since 0.1
 */
public class PackagesMetaTest {

    /**
     * Simple test for artifact meta.
     *
     * @checkstyle LineLengthCheck (9 lines).
     */
    @Test
    public void artifactTest() {
        final PackagesMeta meta = new PackagesMeta();
        MatcherAssert.assertThat(
            meta.update(new ArtifactMeta("artifact-1.0.0")).html(),
            Matchers.equalTo(
                "<html><head></head><body><table><thead><tr><th>Filename</th></tr></thead><tbody><tr><td><a href=\"artifact-1.0.0.tar.gz\">artifact-1.0.0.tar.gz</a></td></tr></tbody></table></body></html>"
            )
        );
    }

    /**
     * Simple test for artifact meta.
     *
     * @checkstyle LineLengthCheck (9 lines).
     */
    @Test
    public void packageTest() {
        final PackagesMeta meta = new PackagesMeta();
        MatcherAssert.assertThat(
            meta.update(new PackageMeta("package")).html(),
            Matchers.equalTo(
                "<html><head></head><body><table><thead><tr><th>Filename</th></tr></thead><tbody><tr><td><a href=\"package\">package</a></td></tr></tbody></table></body></html>"
            )
        );
    }

    /**
     * Simple test for artifact meta.
     *
     * @checkstyle LineLengthCheck (9 lines).
     */
    @Test
    public void packageDuplicationTest() {
        final String html = "<html><head></head><body><table><thead><tr><th>Filename</th></tr></thead><tbody><tr><td><a href=\"single-package\">single-package</a></td></tr></tbody></table></body></html>";
        final PackagesMeta meta = new PackagesMeta(html);
        MatcherAssert.assertThat(
            meta.update(new PackageMeta("single-package")).html(),
            Matchers.equalTo(
                html
            )
        );
    }
}
