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
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.auth.BasicIdentities;
import com.artipie.http.auth.Identities;
import com.artipie.http.auth.Permissions;
import com.artipie.pypi.PypiContainer;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.nio.file.Path;
import java.util.Optional;
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
            (name, perm) -> user.equals(name) && ("download".equals(perm) || "upload".equals(perm)),
            this.auth(user, pswd)
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
            runtime.stop();
        }
    }

    @Test
    void canPublishAndInstallIfNameIsNotNormalized() throws Exception {
        final Storage storage = new InMemoryStorage();
        final int port = this.startServer(storage, Permissions.FREE, Identities.ANONYMOUS);
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
            runtime.stop();
        }
    }

    @Test
    void canInstallWithVersion(@TempDir final Path temp) throws Exception {
        final FileStorage storage = new FileStorage(temp);
        this.putPackages(storage);
        final int port = this.startServer(storage, Permissions.FREE, Identities.ANONYMOUS);
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
            runtime.stop();
        }
    }

    private Identities auth(final String user, final String pswd) {
        return new BasicIdentities(
            (name, pass) -> {
                final Optional<String> res;
                if (user.equals(name) && pswd.equals(pass)) {
                    res = Optional.of(name);
                } else {
                    res = Optional.empty();
                }
                return res;
            }
        );
    }

    private void putPackages(final Storage storage) {
        new TestResource("pypi_repo/alarmtime-0.1.5.tar.gz")
            .saveTo(storage, new Key.From("alarmtime", "alarmtime-0.1.5.tar.gz"));
    }

    private int startServer(final Storage storage, final Permissions perms,
        final Identities auth) {
        this.server = new VertxSliceServer(this.vertx, new PySlice(storage, perms, auth));
        final int port = this.server.start();
        Testcontainers.exposeHostPorts(port);
        return port;
    }

}
