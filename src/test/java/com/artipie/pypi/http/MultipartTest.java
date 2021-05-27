/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/python-adapter/LICENSE.txt
 */
package com.artipie.pypi.http;

import com.artipie.http.Headers;
import com.artipie.http.headers.ContentType;
import io.reactivex.Flowable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.CompletionException;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.cactoos.list.ListOf;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.llorllale.cactoos.matchers.MatcherOf;

/**
 * Test for {@link Multipart}.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
class MultipartTest {

    @Test
    void failsIfHeadersAreEmpty() {
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                CompletionException.class,
                () -> new Multipart(Collections.emptyList(), Flowable.empty())
                    .content().toCompletableFuture().join()
            ).getCause(),
            new AllOf<>(
                new ListOf<Matcher<? super Throwable>>(
                    new MatcherOf<>(ex -> ex instanceof IllegalStateException),
                    new MatcherOf<>(
                        ex -> {
                            return ex.getMessage().contains("Cannot find header \"Content-Type\"");
                        })
                )
            )
        );
    }

    @Test
    void failsIfBoundaryIsNotProvided() {
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                CompletionException.class,
                () -> new Multipart(
                    new Headers.From(new ContentType("123")), Flowable.empty()
                ).content().toCompletableFuture().join()
            ).getCause(),
            new AllOf<>(
                new ListOf<Matcher<? super Throwable>>(
                    new MatcherOf<>(ex -> ex instanceof NullPointerException),
                    new MatcherOf<>(
                        ex -> {
                            return ex.getMessage().contains("Boundary not specified");
                        })
                )
            )
        );
    }

    @Test
    void parsesMultipart() throws IOException {
        final byte[] bytes = "python content".getBytes();
        final String filename = "my_package-0.1.tar.gz";
        final String boundary = "098";
        final Multipart.Data res = new Multipart(
            new Headers.From(new ContentType(String.format("Multipart;boundary=%s", boundary))),
            Flowable.fromArray(ByteBuffer.wrap(this.multipartBody(bytes, filename, boundary)))
        ).content().toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Returns correct filename",
            res.fileName(),
            new IsEqual<>(filename)
        );
        MatcherAssert.assertThat(
            "Returns correct data",
            res.bytes(),
            new IsEqual<>(bytes)
        );
    }

    private byte[] multipartBody(final byte[] input, final String filename, final String boundary)
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
