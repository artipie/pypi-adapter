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

import com.jcabi.log.Logger;
import java.io.IOException;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

/**
 * A class with base utility  for tests, that instantiates container with python runtime.
 *
 * @since 0.2
 */
@SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
public final class PypiContainer extends GenericContainer<PypiContainer> {

    /**
     * Ctor.
     *  Construct container with python runtime and tools.
     */
    PypiContainer() {
        super("python:3");
        this.setCommand("tail", "-f", "/dev/null");
        this.setWorkingDirectory("/home/");
        this.withFileSystemBind("./src/test/resources", "/home");
        this.start();
    }

    /**
     * Install tooling(twine).
     *
     * @throws IOException          If fails.
     * @throws InterruptedException If fails.
     */
    public void installTooling() throws IOException, InterruptedException {
        this.bash("python3 -m pip install --user --upgrade twine");
    }

    /**
     * Executes a bash command in a python container.
     *
     * @param command Bash command to execute.
     * @return Stdout of command.
     * @throws IOException          If fails.
     * @throws InterruptedException If fails.
     */
    public String bash(final String command)
        throws IOException, InterruptedException {
        final Container.ExecResult exec =  this.execInContainer(
            "/bin/bash",
            "-c",
            command
        );
        Logger.info(PypiCliITCase.class, exec.getStdout());
        Logger.info(PypiCliITCase.class, exec.getStderr());
        return exec.getStdout();
    }

    /**
     * Address to access local port from the docker container.
     * @param port Port
     * @return Address
     * @checkstyle NonStaticMethodCheck (10 lines)
     */
    public String localAddress(final int port) {
        return String.format("http://host.testcontainers.internal:%d/", port);
    }
}
