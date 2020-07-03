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

import com.artipie.asto.fs.FileStorage;
import com.artipie.pypi.http.PySlice;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.Testcontainers;

/**
 * A test which ensures repository compatibility with
 * the {@code list}  operation at the adapter.
 *
 * @since 0.2
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle NonStaticMethodCheck (500 lines)
 * @checkstyle LineLengthCheck (500 lines).
 */
@SuppressWarnings("PMD.SystemPrintln")
@DisabledIfSystemProperty(named = "os.name", matches = "Windows.*")
public final class PypiListTCase {

    /**
     * Test start docker container, set up all python utils and publish python packeges.
     * After publish complete test perform {@code list} operation
     * @param temp Path to temporary directory.
     * @checkstyle MethodsOrderCheck (5 lines)
     * @throws IOException In case of network error
     * @throws InterruptedException In case of network error or something else
     */
    @Test
    @Disabled
    public void pypiListWorks(@TempDir final Path temp)
        throws IOException, InterruptedException {
        final Vertx vertx = Vertx.vertx();
        try (VertxSliceServer server = new VertxSliceServer(
            vertx,
            new PySlice(new FileStorage(temp, vertx.fileSystem()))
        )) {
            final int port = server.start();
            Testcontainers.exposeHostPorts(port);
            try (PypiContainer runtime = new PypiContainer()) {
                runtime.installTooling();
                final String adr = String.format("http://127.0.0.1:%s", port);
                runtime.bash(
                    String.format("python3 -m twine upload --repository-url %s -u artem.lazarev -p pass --verbose example_pkg/dist/*", adr)
                );
                final HttpResponse response = HttpClientBuilder.create().build().execute(new HttpGet(adr));
                MatcherAssert.assertThat(
                    response.getStatusLine().getStatusCode(),
                    Matchers.equalTo(HttpStatus.SC_OK)
                );
            }
        }
        vertx.close();
    }
}
