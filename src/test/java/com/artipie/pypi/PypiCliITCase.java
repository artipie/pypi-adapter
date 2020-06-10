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
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.Testcontainers;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

/**
 * A test which ensures {@code gem} console tool compatibility with the adapter.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.SystemPrintln")
@DisabledIfSystemProperty(named = "os.name", matches = "Windows.*")
public final class PypiCliITCase {

    /**
     * Test start docker container, set up all python utils and download python packege.
     * @param temp Path to temporary directory.
     * @checkstyle MethodsOrderCheck (5 lines)
     */
    @Test
    public void pypiInstallWorks(@TempDir final Path temp)
        throws IOException, InterruptedException {
        final Vertx vertx = Vertx.vertx();
        final VertxSliceServer server = new VertxSliceServer(
            vertx,
            new PySlice(new FileStorage(temp, vertx.fileSystem()))
        );
        final int port = server.start();
        Testcontainers.exposeHostPorts(port);
        final CommonTestRuntimeWrapper runtime = new CommonTestRuntimeWrapper();
        final CommonTestRuntimeWrapper.PypiContainer pypi = runtime.constructPypiContainer();
        MatcherAssert.assertThat(
            runtime.bash(
                pypi,
            "pip install --user --index-url https://test.pypi.org/simple/ --no-deps artipietestpkg"
            ),
            Matchers.startsWith("Looking in indexes: https://test.pypi.org/simple")
        );
        MatcherAssert.assertThat(
            runtime.bash(pypi, "python simplprg.py"),
            Matchers.equalTo("Import test is ok\n")
        );
        pypi.stop();
        server.close();
        vertx.close();
    }

    /**
     * For debug and integration purpose add @Test annotation to the function and run that test.
     * @param temp Path to temporary directory.
     * @checkstyle MethodsOrderCheck (5 lines)
     * @checkstyle MagicNumberCheck (20 lines)
     */
    public void pypiLongTermServerRun(@TempDir final Path temp)
        throws IOException, InterruptedException {
        FileUtils.copyDirectory(new File("./src/test/resources/example_pkg/dist"), temp.toFile());
        final Vertx vertx = Vertx.vertx();
        final VertxSliceServer server = new VertxSliceServer(
            vertx,
            new PySlice(new FileStorage(temp, vertx.fileSystem())),
            8080
        );
        server.start();
        Logger.debug(PypiCliITCase.class, "sleping...");
        Thread.sleep(360_000);
        server.close();
        vertx.close();
    }

}
