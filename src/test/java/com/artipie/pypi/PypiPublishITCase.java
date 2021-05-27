/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/python-adapter/LICENSE.txt
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
