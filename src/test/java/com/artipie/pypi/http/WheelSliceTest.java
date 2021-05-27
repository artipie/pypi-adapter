/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/python-adapter/LICENSE.txt
 */
package com.artipie.pypi.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.Headers;
import com.artipie.http.headers.ContentType;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsEmptyCollection;
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
        final byte[] body = new TestResource("pypi_repo/artipie-sample-0.2.tar").asBytes();
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
        final byte[] body = new TestResource("pypi_repo/ABtests-0.0.2.1-py2.py3-none-any.whl")
            .asBytes();
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
        final byte[] body = new TestResource("pypi_repo/artipie-sample-2.1.tar.bz2").asBytes();
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
            storage.list(Key.ROOT).join(),
            new IsEmptyCollection<>()
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

}
