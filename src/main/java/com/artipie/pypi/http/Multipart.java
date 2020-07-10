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

import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Remaining;
import hu.akarnokd.rxjava2.interop.SingleInterop;
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
    private static final Pattern FILENAME = Pattern.compile(".*filename=\"(.*)\"\\X*");

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
        return new Concatenation(this.body)
            .single()
            .map(Remaining::new)
            .map(Remaining::bytes)
            .map(ByteArrayInputStream::new)
            .map(input -> new MultipartStream(input, this.boundary(), Multipart.BUFFER, null))
            .map(Multipart::content)
            .to(SingleInterop.get());
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
