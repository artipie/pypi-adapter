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

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.http.slice.SliceDownload;
import com.artipie.http.slice.SliceSimple;
import com.artipie.http.slice.SliceWithHeaders;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.reactivestreams.Publisher;
import org.testcontainers.Testcontainers;

/**
 * A test which ensures {@code pip} console tool compatibility with the adapter.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DisabledIfSystemProperty(named = "os.name", matches = "Windows.*")
public final class PypiCliITCase {

    /**
     * Vertx.
     */
    private Vertx vertx;

    @BeforeEach
    void start() {
        this.vertx = Vertx.vertx();
    }

    @AfterEach
    void stop() {
        this.vertx.close();
    }

    @Test
    void pypiInstallLatestVersionWorks(@TempDir final Path temp) throws Exception {
        final FileStorage storage = new FileStorage(temp);
        final Fake slice = new Fake(storage);
        this.putPackages(storage);
        try (VertxSliceServer server = new VertxSliceServer(this.vertx, new LoggingSlice(slice))) {
            final int port = server.start();
            Testcontainers.exposeHostPorts(port);
            try (PypiContainer runtime = new PypiContainer()) {
                MatcherAssert.assertThat(
                    runtime.bash(
                        String.format(
                            // @checkstyle LineLengthCheck (1 line)
                            "pip install  --index-url %s --no-deps --trusted-host host.testcontainers.internal alarmtime",
                            runtime.localAddress(port)
                        )
                    ),
                    new StringContains(true, "Successfully installed alarmtime-0.1.5")
                );
                runtime.stop();
            }
        }
    }

    @Test
    void pypiInstallWithVersionWorks(@TempDir final Path temp) throws Exception {
        final FileStorage storage = new FileStorage(temp);
        this.putPackages(storage);
        try (VertxSliceServer server = new VertxSliceServer(this.vertx, new Fake(storage))) {
            final int port = server.start();
            Testcontainers.exposeHostPorts(port);
            try (PypiContainer runtime = new PypiContainer()) {
                MatcherAssert.assertThat(
                    runtime.bash(
                        String.format(
                            // @checkstyle LineLengthCheck (1 line)
                            "pip install --index-url %s --no-deps --trusted-host host.testcontainers.internal \"alarmtime==0.1.5\"",
                            runtime.localAddress(port)
                        )
                    ),
                    Matchers.containsString("Successfully installed alarmtime-0.1.5")
                );
                runtime.stop();
            }
        }
    }

    private void putPackages(final Storage storage) throws Exception {
        storage.save(
            new Key.From("alarmtime", "AlarmTime-0.1.5.tar.gz"),
            new Content.From(
                Files.readAllBytes(
                    Paths.get(
                        Thread.currentThread().getContextClassLoader()
                        .getResource("pypi_repo/AlarmTime-0.1.5.tar.gz").toURI()
                    )
                )
            )
        ).join();
    }

    /**
     * Fake slice for test.
     * @since 0.3
     */
    private final class Fake implements Slice {

        /**
         * Storage.
         */
        private final Storage storage;

        /**
         * Ctor.
         * @param storage Storage
         */
        protected Fake(final Storage storage) {
            this.storage = storage;
        }

        @Override
        public Response response(
            final String rqline,
            final Iterable<Map.Entry<String, String>> iterable,
            final Publisher<ByteBuffer> publisher
        ) {
            final Response res;
            if (rqline.contains(".whl") || rqline.contains(".tar.gz")) {
                res = new SliceWithHeaders(
                    new SliceDownload(this.storage),
                    new Headers.From(new ContentType("application/octet-stream"))
                ).response(rqline, iterable, publisher);
            } else {
                res = new SliceSimple(
                    new RsWithHeaders(
                        new RsWithBody(
                            String.join(
                                "\n",
                                "<!DOCTYPE html>",
                                "<html>",
                                "  <head>",
                                "    <title>Test repository</title>",
                                "  </head>",
                                "  <body>",
                                // @checkstyle LineLengthCheck (1 line)
                                "    <a href=\"/alarmtime/AlarmTime-0.1.5.tar.gz\">AlarmTime-0.1.5.tar.gz</a><br/>",
                                "    </body>",
                                "</html>"
                            ),
                            StandardCharsets.UTF_8
                        ),
                        new Headers.From(new ContentType("text/html"))
                    )
                ).response(rqline, iterable, publisher);
            }
            return res;
        }

    }

}
