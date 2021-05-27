/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/python-adapter/LICENSE.txt
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
    public PypiContainer() {
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
        Logger.info(this, exec.getStdout());
        Logger.info(this, exec.getStderr());
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

    /**
     * Address to access local port from the docker container.
     * @param port Port
     * @param user Username
     * @param pswd Password
     * @return Address
     * @checkstyle NonStaticMethodCheck (10 lines)
     */
    public String localAddress(final int port, final String user, final String pswd) {
        return String.format("http://%s:%s@host.testcontainers.internal:%d/", user, pswd, port);
    }
}
