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
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.pypi.PypiContainer;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import org.cactoos.list.ListOf;
import org.cactoos.text.TextOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.Testcontainers;

/**
 * Test for {@link PyProxySlice}.
 * @since 0.7
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class PyProxySliceITCase {

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Jetty client.
     */
    private final JettyClientSlices client = new JettyClientSlices();

    /**
     * Server port.
     */
    private int port;

    /**
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    /**
     * Test storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp() throws Exception {
        this.client.start();
        this.storage = new InMemoryStorage();
        this.server = new VertxSliceServer(
            PyProxySliceITCase.VERTX,
            new LoggingSlice(
                new PyProxySlice(
                    this.client, URI.create("https://pypi.org/simple"), this.storage
                )
            )
        );
        this.port = this.server.start();
    }

    @Test
    void installsFromProxy() throws IOException, InterruptedException {
        Testcontainers.exposeHostPorts(this.port);
        try (PypiContainer runtime = new PypiContainer()) {
            MatcherAssert.assertThat(
                runtime.bash(
                    String.format(
                        // @checkstyle LineLengthCheck (1 line)
                        "pip install --index-url %s --verbose --no-deps --trusted-host host.testcontainers.internal \"alarmtime\"",
                        runtime.localAddress(this.port)
                    )
                ),
                Matchers.containsString("Successfully installed alarmtime-0.1.4")
            );
            MatcherAssert.assertThat(
                "Requested items cached",
                this.storage.list(new Key.From("alarmtime")).join().isEmpty(),
                new IsEqual<>(false)
            );
        }
    }

    @Test
    void proxiesIndexRequest() throws Exception {
        final String key = "a2utils";
        final HttpURLConnection con = (HttpURLConnection) new URL(
            String.format("http://localhost:%s/%s/", this.port, key)
        ).openConnection();
        con.setRequestMethod(RqMethod.GET.value());
        MatcherAssert.assertThat(
            "Response status is 200",
            con.getResponseCode(),
            new IsEqual<>(Integer.parseInt(RsStatus.OK.code()))
        );
        final ListOf<String> expected = new ListOf<>(
            "<!DOCTYPE html>", "Links for a2utils",
            "a2utils-0.0.1-py3-none-any.whl", "a2utils-0.0.2-py3-none-any.whl"
        );
        MatcherAssert.assertThat(
            "Response body is html with packages list",
            new TextOf(con.getInputStream()).asString(),
            new StringContainsInOrder(expected)
        );
        MatcherAssert.assertThat(
            "Index page was added to storage",
            new PublisherAs(
                this.storage.value(new Key.From(key)).join()
            ).asciiString().toCompletableFuture().join(),
            new StringContainsInOrder(expected)
        );
        con.disconnect();
    }

    @Test
    void proxiesUnsuccessfulResponseStatus() throws Exception {
        final HttpURLConnection con = (HttpURLConnection) new URL(
            String.format("http://localhost:%s/abc/123/", this.port)
        ).openConnection();
        con.setRequestMethod(RqMethod.GET.value());
        MatcherAssert.assertThat(
            "Response status is 404",
            con.getResponseCode(),
            new IsEqual<>(Integer.parseInt(RsStatus.NOT_FOUND.code()))
        );
        MatcherAssert.assertThat(
            "Nothing was added to storage",
            this.storage.list(Key.ROOT).join().isEmpty(),
            new IsEqual<>(true)
        );
        con.disconnect();
    }

    @AfterEach
    void tearDown() throws Exception {
        this.client.stop();
        this.server.stop();
    }

}
