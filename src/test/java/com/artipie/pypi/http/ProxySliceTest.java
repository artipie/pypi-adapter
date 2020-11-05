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
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.cache.FromRemoteCache;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.slice.SliceSimple;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link ProxySlice}.
 * @since 0.7
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class ProxySliceTest {

    /**
     * Test storage.
     */
    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void getsContentFromRemoteAndAdsItToCache() {
        final byte[] body = "some html".getBytes();
        final String key = "index";
        final Headers.From header = new Headers.From("content-type", "smth");
        MatcherAssert.assertThat(
            "Returns body from remote",
            new ProxySlice(
                new SliceSimple(new RsFull(RsStatus.OK, header, new Content.From(body))),
                new FromRemoteCache(this.storage)
            ),
            new SliceHasResponse(
                Matchers.allOf(new RsHasBody(body), new RsHasHeaders(header)),
                new RequestLine(RqMethod.GET, String.format("/%s", key))
            )
        );
        MatcherAssert.assertThat(
            "Stores index in cache",
            new BlockingStorage(this.storage).value(new Key.From(key)),
            new IsEqual<>(body)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "my project versions list in html,text/html,my_project",
        "my project wheel,multipart/form-data,my_project.whl"
    })
    void getsFromCacheOnError(final String data, final String header, final String key) {
        final byte[] body = data.getBytes();
        this.storage.save(new Key.From(key), new Content.From(body)).join();
        MatcherAssert.assertThat(
            "Returns body from cache",
            new ProxySlice(
                new SliceSimple(new RsWithStatus(RsStatus.INTERNAL_ERROR)),
                new FromRemoteCache(this.storage)
            ),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK), new RsHasBody(body),
                    new RsHasHeaders(new Header("content-type", header))
                ),
                new RequestLine(RqMethod.GET, String.format("/%s", key))
            )
        );
        MatcherAssert.assertThat(
            "Data stays intact in cache",
            new BlockingStorage(this.storage).value(new Key.From(key)),
            new IsEqual<>(body)
        );
    }

    @Test
    void proxiesStatusAndHeaderFromRemoteOnError() {
        final RsStatus status = RsStatus.BAD_REQUEST;
        final Headers.From header = new Headers.From("x-key", "value");
        MatcherAssert.assertThat(
            "Status 400 returned",
            new ProxySlice(
                new SliceSimple(new RsWithHeaders(new RsWithStatus(status), header)),
                new FromRemoteCache(this.storage)
            ),
            new SliceHasResponse(
                Matchers.allOf(new RsHasStatus(status), new RsHasHeaders(header)),
                new RequestLine(RqMethod.GET, "/any")
            )
        );
        MatcherAssert.assertThat(
            "Cache storage is empty",
            this.storage.list(Key.ROOT).join().isEmpty(),
            new IsEqual<>(true)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "my project versions list in html,My_Project,my-project",
        "my project wheel,My.Project.whl,My.Project.whl",
        "John's project tar,Johns.Project.tar.gz,Johns.Project.tar.gz",
        "Another index,AnotherIndex,anotherindex"
    })
    void normalisesNamesWhenNecessary(final String data, final String line, final String key) {
        final byte[] body = data.getBytes();
        final Headers.From header = new Headers.From("content-type", "smth");
        MatcherAssert.assertThat(
            "Returns body from remote",
            new ProxySlice(
                new SliceSimple(new RsFull(RsStatus.OK, header, new Content.From(body))),
                new FromRemoteCache(this.storage)
            ),
            new SliceHasResponse(
                Matchers.allOf(new RsHasBody(body), new RsHasHeaders(header)),
                new RequestLine(RqMethod.GET, String.format("/%s", line))
            )
        );
        MatcherAssert.assertThat(
            "Stores content in cache",
            new BlockingStorage(this.storage).value(new Key.From(key)),
            new IsEqual<>(body)
        );
    }

}
