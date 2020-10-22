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
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.auth.Authentication;
import com.artipie.http.client.auth.Authenticator;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.pypi.PypiContainer;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.net.URI;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.Testcontainers;

/**
 * Test for {@link PyProxySlice} with authorisation.
 * @since 0.7
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@EnabledOnOs({OS.LINUX, OS.MAC})
class PyProxySliceAuthITCase {

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Jetty client.
     */
    private final JettyClientSlices client = new JettyClientSlices();

    /**
     * Vertx slice origin server instance.
     */
    private VertxSliceServer origin;

    /**
     * Vertx slice proxy server instance.
     */
    private VertxSliceServer proxy;

    /**
     * Proxy port.
     */
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        final String bob = "Bob";
        final String pswd = "abc123";
        final Storage storage = new InMemoryStorage();
        new TestResource("pypi_repo/alarmtime-0.1.5.tar.gz")
            .saveTo(storage, new Key.From("alarmtime", "alarmtime-0.1.5.tar.gz"));
        this.origin = new VertxSliceServer(
            PyProxySliceAuthITCase.VERTX,
            new LoggingSlice(
                new PySlice(
                    storage, (user, action) -> user.name().equals(bob),
                    new Authentication.Single(bob, pswd)
                )
            )
        );
        this.client.start();
        this.proxy = new VertxSliceServer(
            PyProxySliceAuthITCase.VERTX,
            new LoggingSlice(
                new PyProxySlice(
                    this.client,
                    URI.create(String.format("http://localhost:%d", this.origin.start())),
                    new Authenticator.Basic(bob, pswd),
                    new InMemoryStorage()
                )
            )
        );
        this.port = this.proxy.start();
    }

    @Test
    void installsFromProxy() throws IOException, InterruptedException {
        Testcontainers.exposeHostPorts(this.port);
        try (PypiContainer runtime = new PypiContainer()) {
            MatcherAssert.assertThat(
                runtime.bash(
                    String.format(
                        // @checkstyle LineLengthCheck (1 line)
                        "pip install --index-url %s --no-deps --trusted-host host.testcontainers.internal \"alarmtime\"",
                        runtime.localAddress(this.port)
                    )
                ),
                Matchers.containsString("Successfully installed alarmtime-0.1.5")
            );
        }
    }

    @AfterEach
    void close() throws Exception {
        this.proxy.stop();
        this.origin.stop();
        this.client.stop();
    }

}
