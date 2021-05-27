/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/python-adapter/LICENSE.txt
 */
package com.artipie.pypi.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.Permissions;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.pypi.PypiContainer;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.nio.file.Path;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.Testcontainers;

/**
 * A test which ensures {@code pip} console tool compatibility with the adapter.
 *
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DisabledIfSystemProperty(named = "os.name", matches = "Windows.*")
public final class PySliceITCase {

    /**
     * Vertx.
     */
    private Vertx vertx;

    /**
     * Vertx slice server.
     */
    private VertxSliceServer server;

    @BeforeEach
    void start() {
        this.vertx = Vertx.vertx();
    }

    @AfterEach
    void stop() {
        if (this.server != null) {
            this.server.close();
        }
        if (this.vertx != null) {
            this.vertx.close();
        }
    }

    @Test
    void canPublishAndInstallWithAuth(@TempDir final Path temp) throws Exception {
        final FileStorage storage = new FileStorage(temp);
        final String user = "alladin";
        final String pswd = "opensesame";
        final int port = this.startServer(
            storage,
            (identity, perm) -> user.equals(identity.name())
                && ("download".equals(perm) || "upload".equals(perm)),
            new Authentication.Single(user, pswd)
        );
        try (PypiContainer runtime = new PypiContainer()) {
            runtime.installTooling();
            MatcherAssert.assertThat(
                "AlarmTime successfully uploaded",
                runtime.bash(
                    // @checkstyle LineLengthCheck (1 line)
                    String.format("python3 -m twine upload --repository-url %s -u %s -p %s --verbose pypi_repo/alarmtime-0.1.5.tar.gz", runtime.localAddress(port), user, pswd)
                ),
                new StringContainsInOrder(
                    new ListOf<String>(
                        "Uploading alarmtime-0.1.5.tar.gz", "100%"
                    )
                )
            );
            MatcherAssert.assertThat(
                "AlarmTime found in storage",
                storage.exists(new Key.From("alarmtime/alarmtime-0.1.5.tar.gz")).join(),
                new IsEqual<>(true)
            );
            MatcherAssert.assertThat(
                "AlarmTime successfully installed",
                runtime.bash(
                    String.format(
                        // @checkstyle LineLengthCheck (1 line)
                        "pip install --index-url %s --no-deps --trusted-host host.testcontainers.internal alarmtime",
                        runtime.localAddress(port, user, pswd)
                    )
                ),
                new StringContains("Successfully installed alarmtime-0.1.5")
            );
        }
    }

    @Test
    void canPublishAndInstallIfNameIsNotNormalized() throws Exception {
        final Storage storage = new InMemoryStorage();
        final int port = this.startServer(storage);
        try (PypiContainer runtime = new PypiContainer()) {
            runtime.installTooling();
            MatcherAssert.assertThat(
                "ABtests successfully uploaded",
                runtime.bash(
                    // @checkstyle LineLengthCheck (1 line)
                    String.format("python3 -m twine upload --repository-url %s -u any -p any --verbose pypi_repo/ABtests-0.0.2.1-py2.py3-none-any.whl", runtime.localAddress(port))
                ),
                new StringContainsInOrder(
                    new ListOf<String>(
                        "Uploading ABtests-0.0.2.1-py2.py3-none-any.whl", "100%"
                    )
                )
            );
            MatcherAssert.assertThat(
                "ABtests found in storage",
                storage.exists(new Key.From("abtests/ABtests-0.0.2.1-py2.py3-none-any.whl")).join(),
                new IsEqual<>(true)
            );
            MatcherAssert.assertThat(
                "ABtests successfully installed",
                runtime.bash(
                    String.format(
                        // @checkstyle LineLengthCheck (1 line)
                        "pip install --index-url %s --no-deps --trusted-host host.testcontainers.internal ABtests",
                        runtime.localAddress(port)
                    )
                ),
                new StringContains("Successfully installed ABtests-0.0.2.1")
            );
        }
    }

    @Test
    void canInstallWithVersion(@TempDir final Path temp) throws Exception {
        final FileStorage storage = new FileStorage(temp);
        this.putPackages(storage);
        final int port = this.startServer(storage);
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
        }
    }

    @Test
    void canSearch() throws Exception {
        final Storage storage = new InMemoryStorage();
        this.putPackages(storage);
        final int port = this.startServer(storage);
        try (PypiContainer runtime = new PypiContainer()) {
            MatcherAssert.assertThat(
                runtime.bash(
                    String.format(
                        // @checkstyle LineLengthCheck (1 line)
                        "pip search alarmtime --index %s",
                        runtime.localAddress(port)
                    )
                ),
                Matchers.stringContainsInOrder("AlarmTime", "0.1.5")
            );
        }
    }

    private void putPackages(final Storage storage) {
        new TestResource("pypi_repo/alarmtime-0.1.5.tar.gz")
            .saveTo(storage, new Key.From("alarmtime", "alarmtime-0.1.5.tar.gz"));
    }

    private int startServer(final Storage storage, final Permissions perms,
        final Authentication auth) {
        this.server = new VertxSliceServer(
            this.vertx,
            new LoggingSlice(new PySlice(storage, perms, auth))
        );
        final int port = this.server.start();
        Testcontainers.exposeHostPorts(port);
        return port;
    }

    private int startServer(final Storage storage) {
        return this.startServer(storage, Permissions.FREE, Authentication.ANONYMOUS);
    }

}
