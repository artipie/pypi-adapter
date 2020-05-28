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
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

/**
 * A test which ensures {@code gem} console tool compatibility with the adapter.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ParameterNumberCheck (500 lines)
 * @checkstyle ParameterNameCheck (500 lines)
 * @checkstyle MethodBodyCommentsCheck (500 lines)
 * @checkstyle LineLengthCheck (500 lines)
 * @checkstyle DesignForExtensionCheck (500 lines)
 */
@SuppressWarnings("PMD.SystemPrintln")
@DisabledIfSystemProperty(named = "os.name", matches = "Windows.*")
public final class PypiCliITCase {

    /**
     * Root for test repository.
     */
    public static final String REPO_ROOT = "/simple";

    @Test
    public void pypiInstallWorks(@TempDir final Path temp)
        throws IOException, InterruptedException {
        final Vertx vertx = Vertx.vertx();
        final VertxSliceServer server = new VertxSliceServer(
            vertx,
            new PySlice(PypiCliITCase.REPO_ROOT, new FileStorage(temp, vertx.fileSystem()))
        );
        final int port = server.start();
        Testcontainers.exposeHostPorts(port);
        final PypiContainer pypi = new PypiContainer()
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind("./src/test/resources", "/home");
        pypi.start();
        MatcherAssert.assertThat(
            this.bash(
                pypi,
            "pip install --user --index-url https://test.pypi.org/simple/ --no-deps artipietestpkg"
            ),
            Matchers.startsWith("Looking in indexes: https://test.pypi.org/simple")
        );
        MatcherAssert.assertThat(
            this.bash(pypi, "python simplprg.py"),
            Matchers.equalTo("Import test is ok\n")
        );
        pypi.stop();
        server.close();
        vertx.close();
    }

    /**
     * Executes a bash command in a python container.
     * @param pypi The python container.
     * @param command Bash command to execute.
     * @return Exit code.
     * @throws IOException If fails.
     * @throws InterruptedException If fails.
     */
    private String bash(final PypiContainer pypi, final String command)
        throws IOException, InterruptedException {
        final Container.ExecResult exec = pypi.execInContainer(
            "/bin/bash",
            "-c",
            command
        );
        Logger.info(PypiCliITCase.class, exec.getStdout());
        Logger.info(PypiCliITCase.class, exec.getStderr());
        return exec.getStdout();
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
            new PySlice(PypiCliITCase.REPO_ROOT, new FileStorage(temp, vertx.fileSystem())),
            8080
            );
        server.start();
        Logger.debug(PypiCliITCase.class, "sleping...");
        Thread.sleep(360_000);
        server.close();
        vertx.close();
    }

    /**
     * Inner subclass to instantiate python container.
     *
     * @since 0.1
     */
    private static class PypiContainer extends GenericContainer<PypiContainer> {
        PypiContainer() {
            super("python:3");
        }
    }
}
