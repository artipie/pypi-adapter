package com.artipie.pypi;

import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * BinaryArtifactTest.
 *
 * @since 0.1
 */
public class BinaryArtifactTest {

    private Storage storage;

    /**
     * Tested Py slice.
     */
    private PySlice pyslice;

    @BeforeEach
    void init(final @TempDir Path temp) {
        this.storage = new FileStorage(temp);
        this.pyslice = new PySlice("/base/", this.storage);
    }

    @Test
    public void shouldGetBinaryArtifactTest() {
        final String string = "string";
        BinaryArtifact artifact = new BinaryArtifact(string, string);
        CompletableFuture<Void> future = artifact.save(storage);
        future.thenRun(() -> {
                }
        );

    }
}
