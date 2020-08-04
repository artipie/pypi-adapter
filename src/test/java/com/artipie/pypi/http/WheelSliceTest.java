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

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.headers.ContentType;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.commons.io.IOUtils;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link WheelSlice}.
 * @since 0.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class WheelSliceTest {

    @Test
    void savesContentAndReturnsOk() throws IOException {
        final Storage storage = new InMemoryStorage();
        final String boundary = "098";
        final String filename = "artipie-sample-0.2.tar";
        final byte[] body = this.resource("pypi_repo/artipie-sample-0.2.tar");
        MatcherAssert.assertThat(
            "Returns CREATED status",
            new WheelSlice(storage).response(
                new RequestLine("GET", "/").toString(),
                new Headers.From(new ContentType(String.format("Multipart;boundary=%s", boundary))),
                Flowable.fromArray(ByteBuffer.wrap(this.multipartBody(body, boundary, filename)))
            ),
            new RsHasStatus(RsStatus.CREATED)
        );
        MatcherAssert.assertThat(
            "Saves content to storage",
            new PublisherAs(
                storage.value(new Key.From("artipie-sample", filename)).join()
            ).bytes().toCompletableFuture().join(),
            new IsEqual<>(body)
        );
    }

    @Test
    void savesContentByNormalizedNameAndReturnsOk() throws IOException {
        final Storage storage = new InMemoryStorage();
        final String boundary = "876";
        final String filename = "ABtests-0.0.2.1-py2.py3-none-any.whl";
        final String path = "super";
        final byte[] body = this.resource("pypi_repo/ABtests-0.0.2.1-py2.py3-none-any.whl");
        MatcherAssert.assertThat(
            "Returns CREATED status",
            new WheelSlice(storage).response(
                new RequestLine("GET", String.format("/%s", path)).toString(),
                new Headers.From(new ContentType(String.format("Multipart;boundary=%s", boundary))),
                Flowable.fromArray(
                    ByteBuffer.wrap(
                        this.multipartBody(
                            body,
                            boundary, filename
                        )
                    )
                )
            ),
            new RsHasStatus(RsStatus.CREATED)
        );
        MatcherAssert.assertThat(
            "Saves content to storage",
            new PublisherAs(
                storage.value(new Key.From(path, "abtests", filename)).join()
            ).bytes().toCompletableFuture().join(),
            new IsEqual<>(body)
        );
    }

    @Test
    void returnsBadRequestIfFileNameIsInvalid() throws IOException {
        final Storage storage = new InMemoryStorage();
        final String boundary = "876";
        final String filename = "artipie-sample-2020.tar.bz2";
        final byte[] body = this.resource("pypi_repo/artipie-sample-2.1.tar.bz2");
        MatcherAssert.assertThat(
            "Returns BAD_REQUEST status",
            new WheelSlice(storage).response(
                new RequestLine("GET", "/").toString(),
                new Headers.From(new ContentType(String.format("Multipart;boundary=%s", boundary))),
                Flowable.fromArray(
                    ByteBuffer.wrap(
                        this.multipartBody(
                            body,
                            boundary, filename
                        )
                    )
                )
            ),
            new RsHasStatus(RsStatus.BAD_REQUEST)
        );
        MatcherAssert.assertThat(
            "Storage is empty",
            storage.list(Key.ROOT).join().size(),
            new IsEqual<>(0)
        );
    }

    @Test
    void returnsBadRequestIfFileInvalid() throws IOException {
        final Storage storage = new InMemoryStorage();
        final String boundary = "000";
        final String filename = "myproject.whl";
        final byte[] body = "some code".getBytes();
        MatcherAssert.assertThat(
            new WheelSlice(storage).response(
                new RequestLine("GET", "/").toString(),
                new Headers.From(new ContentType(String.format("Multipart;boundary=%s", boundary))),
                Flowable.fromArray(ByteBuffer.wrap(this.multipartBody(body, boundary, filename)))
            ),
            new RsHasStatus(RsStatus.BAD_REQUEST)
        );
    }

    private byte[] multipartBody(final byte[] input, final String boundary, final String filename)
        throws IOException {
        try (ByteArrayOutputStream res = new ByteArrayOutputStream()) {
            MultipartEntityBuilder.create()
                .setBoundary(boundary)
                .addBinaryBody(
                    "some_file", input, org.apache.http.entity.ContentType.TEXT_PLAIN, filename
                )
                .build()
                .writeTo(res);
            return res.toByteArray();
        }
    }

    private byte[] resource(final String name) {
        try {
            return IOUtils.toByteArray(
                Thread.currentThread().getContextClassLoader().getResourceAsStream(name)
            );
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to load test recourses", ex);
        }
    }

}
