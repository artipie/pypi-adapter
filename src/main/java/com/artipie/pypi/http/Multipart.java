/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/python-adapter/LICENSE.txt
 */
package com.artipie.pypi.http;

import com.artipie.asto.Content;
import com.artipie.asto.ext.PublisherAs;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;
import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.fileupload.ParameterParser;
import org.reactivestreams.Publisher;

/**
 * HTTP 'multipart/form-data' request.
 *
 * @since 0.2
 */
final class Multipart {

    /**
     * Size of multipart stream buffer.
     */
    private static final int BUFFER = 4096;

    /**
     * Pattern to obtain filename from multipart header.
     */
    private static final Pattern FILENAME = Pattern.compile(".*filename=\"(.*)\"(.*\\s*)*");

    /**
     * Request headers.
     */
    private final Iterable<Map.Entry<String, String>> headers;

    /**
     * Request body.
     */
    private final Publisher<ByteBuffer> body;

    /**
     * Ctor.
     *
     * @param headers Request headers.
     * @param body Request body.
     */
    Multipart(
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        this.headers = headers;
        this.body = body;
    }

    /**
     * Read content of file.
     *
     * @return Data.
     */
    public CompletionStage<Data> content() {
        return new PublisherAs(this.body).bytes()
            .thenApply(ByteArrayInputStream::new)
            .thenApply(input -> new MultipartStream(input, this.boundary(), Multipart.BUFFER, null))
            .thenApply(Multipart::content);
    }

    /**
     * Reads boundary from headers.
     *
     * @return Boundary bytes.
     */
    private byte[] boundary() {
        final String header = StreamSupport.stream(this.headers.spliterator(), false)
            .filter(entry -> entry.getKey().equalsIgnoreCase("Content-Type"))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException("Cannot find header \"Content-Type\"")
            );
        final ParameterParser parser = new ParameterParser();
        parser.setLowerCaseNames(true);
        final String boundary = Objects.requireNonNull(
            parser.parse(header, ';').get("boundary"),
            String.format("Boundary not specified: '%s'", header)
        );
        return boundary.getBytes(StandardCharsets.ISO_8859_1);
    }

    /**
     * Read content of file.
     *
     * @param stream Multipart stream.
     * @return Binary content of file.
     */
    private static Data content(final MultipartStream stream) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            boolean next = stream.skipPreamble();
            while (next) {
                final String header = stream.readHeaders();
                if (header.contains("filename")) {
                    final Matcher matcher = FILENAME.matcher(header);
                    stream.readBodyData(bos);
                    matcher.matches();
                    return new Data(matcher.group(1), bos.toByteArray());
                }
                stream.discardBodyData();
                next = stream.readBoundary();
            }
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to read body as multipart", ex);
        }
        throw new IllegalStateException("Body has no file data");
    }

    /**
     * Data from {@link Multipart}.
     * @since 0.3
     */
    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    static final class Data {

        /**
         * File name.
         */
        private final String fname;

        /**
         * Multipart body.
         */
        private final byte[] buffer;

        /**
         * Ctor.
         * @param filename Filename
         * @param buffer Content
         */
        Data(final String filename, final byte[] buffer) {
            this.fname = filename;
            this.buffer = buffer;
        }

        /**
         * Filename.
         * @return Name of the file
         */
        public String fileName() {
            return this.fname;
        }

        /**
         * Content.
         * @return Instance of {@link Content}
         */
        public byte[] bytes() {
            return this.buffer;
        }
    }
}
