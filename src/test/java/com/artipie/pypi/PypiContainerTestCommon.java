package com.artipie.pypi;

import com.jcabi.log.Logger;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;

/**
 * A base class for tests that instantiates container with python runtime.
 *
 * @since 0.2
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public class PypiContainerTestCommon {
    /**
     * Executes a bash command in a python container.
     * @param pypi The python container.
     * @param command Bash command to execute.
     * @return Stdout of command.
     * @throws IOException If fails.
     * @throws InterruptedException If fails.
     */
    protected String bash(final PypiContainer pypi, final String command)
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
     * Construct container with python runtime and tools.
     * @return container object.
     */
    protected PypiContainerTestCommon.PypiContainer constructPypiContainer() {
        final PypiContainer pypi = new PypiContainer()
                .withCommand("tail", "-f", "/dev/null")
                .withWorkingDirectory("/home/")
                .withFileSystemBind("./src/test/resources", "/home")
                .withNetworkMode("host");
        pypi.start();
        return pypi;
    }

    /**
     * Inner subclass to instantiate python container.
     *
     * @since 0.1
     */
    protected static class PypiContainer extends GenericContainer<PypiContainerTestCommon.PypiContainer> {
        PypiContainer() {
            super("python:3");
        }
    }
}
