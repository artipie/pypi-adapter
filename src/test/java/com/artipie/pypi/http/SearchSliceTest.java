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
package com.artipie.pypi.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.Headers;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.pypi.meta.Metadata;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link SearchSlice}.
 * @since 0.7
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class SearchSliceTest {

    /**
     * Test storage.
     */
    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void returnsEmptyXmlWhenArtifactNotFound() {
        MatcherAssert.assertThat(
            new SearchSlice(this.storage),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasHeaders(new Header("content-type", "text/xml")),
                    new RsHasBody(SearchSlice.empty())
                ),
                new RequestLine(RqMethod.POST, "/"),
                Headers.EMPTY,
                new Content.From(this.xml("my_project").getBytes())
            )
        );
    }

    @ParameterizedTest
    @CsvSource({
        "artipie-sample-0.2.tar.gz,artipie-sample",
        "artipie-sample-2.1.tar.Z,artipie-sample",
        "artipie-sample-2.1.tar.bz2,artipie-sample",
        "artipie_sample-2.1-py3.7.egg,artipie-sample",
        "artipie_sample-0.2-py3-none-any.whl,artipie-sample",
        "alarmtime-0.1.5.tar.gz,alarmtime",
        "ABtests-0.0.2.1-py2.py3-none-any.whl,abtests"
    })
    void returnsXmlWithInfoWhenArtifactFound(final String pckg, final String name) {
        final TestResource resource = new TestResource(String.format("pypi_repo/%s", pckg));
        resource.saveTo(this.storage, new Key.From(name, pckg));
        MatcherAssert.assertThat(
            new SearchSlice(this.storage),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasHeaders(new Header("content-type", "text/xml")),
                    new RsHasBody(
                        SearchSlice.found(new Metadata.FromArchive(resource.asPath()).read())
                    )
                ),
                new RequestLine(RqMethod.POST, "/"),
                Headers.EMPTY,
                new Content.From(this.xml(name).getBytes())
            )
        );
    }

    private String xml(final String name) {
        return String.join(
            "\n", "<?xml version='1.0'?>",
            "<methodCall>",
            "<methodName>search</methodName>",
            "<params>",
            "<param>",
            "<value><struct>",
            "<member>",
            "<name>name</name>",
            "<value><array><data>",
            String.format("<value><string>%s</string></value>", name),
            "</data></array></value>",
            "</member>",
            "<member>",
            "<name>summary</name>",
            "<value><array><data>",
            "<value><string>abcdef</string></value>",
            "</data></array></value>",
            "</member>",
            "</struct></value>",
            "</param>",
            "<param>",
            "<value><string>or</string></value>",
            "</param>",
            "</params>",
            "</methodCall>"
        );
    }

}
