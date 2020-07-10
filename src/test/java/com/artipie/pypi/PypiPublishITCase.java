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

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.pypi.http.PySlice;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.nio.file.Path;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.Testcontainers;

/**
 * A test which ensures {@code python} console tool compatibility with the adapter.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DisabledIfSystemProperty(named = "os.name", matches = "Windows.*")
public final class PypiPublishITCase {

    @Test
    public void pypiPublishWorks(@TempDir final Path temp)
        throws IOException, InterruptedException {
        final Vertx vertx = Vertx.vertx();
        final Storage storage = new FileStorage(temp);
        final VertxSliceServer server = new VertxSliceServer(
            vertx, new PySlice(storage)
        );
        final int port = server.start();
        Testcontainers.exposeHostPorts(port);
        try (PypiContainer runtime = new PypiContainer()) {
            runtime.installTooling();
            MatcherAssert.assertThat(
                runtime.bash(
                    // @checkstyle LineLengthCheck (1 line)
                    String.format("python3 -m twine upload --repository-url %s -u artem.lazarev -p pass --verbose example_pkg/dist/*", runtime.localAddress(port))
                ),
                new StringContainsInOrder(
                    new ListOf<String>(
                        "Uploading artipietestpkg-0.0.3-py2-none-any.whl", "100%",
                        "Uploading artipietestpkg-0.0.3.tar.gz", "100%"
                    )
                )
            );
            MatcherAssert.assertThat(
                storage.exists(
                    new Key.From("artipietestpkg", "artipietestpkg-0.0.3-py2-none-any.whl")
                ).join()
                &&
                storage.exists(
                    new Key.From("artipietestpkg", "artipietestpkg-0.0.3.tar.gz")
                ).join(),
                new IsEqual<>(true)
            );
            runtime.stop();
        }
        server.close();
        vertx.close();
    }
}
