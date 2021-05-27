/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/python-adapter/LICENSE.txt
 */
package com.artipie.pypi.meta;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.z.ZCompressorInputStream;
import org.apache.commons.io.IOUtils;

/**
 * Python package metadata.
 * @since 0.6
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public interface Metadata {

    /**
     * Read package metadata from python artifact.
     * @return Instance of {@link PackageInfo}.
     */
    PackageInfo read();

    /**
     * Metadata from archive implementation.
     * @since 0.6
     */
    final class FromArchive implements Metadata {

        /**
         * Path to archive.
         */
        private final Path file;

        /**
         * Name of the file.
         */
        private final String filename;

        /**
         * Ctor.
         * @param file Path to archive
         * @param filename Filename
         */
        public FromArchive(final Path file, final String filename) {
            this.file = file;
            this.filename = filename;
        }

        /**
         * Ctor.
         * @param file Path to archive
         */
        public FromArchive(final Path file) {
            this(file, file.getFileName().toString());
        }

        @Override
        public PackageInfo read() {
            final PackageInfo res;
            if (Stream.of("tar", "zip", "whl", "egg").anyMatch(this.filename::endsWith)) {
                res = this.readZipTarOrWhl();
            } else if (this.filename.endsWith("tar.gz")) {
                res = this.readTarGz();
            } else if (this.filename.endsWith("tar.Z")) {
                res = this.readTarZ();
            } else if (this.filename.endsWith("tar.bz2")) {
                res = this.readBz();
            } else {
                throw new UnsupportedOperationException("Unsupported archive type");
            }
            return res;
        }

        /**
         * Reads tar.Z files.
         * @return PackageInfo
         */
        private PackageInfo readTarZ() {
            try (
                ZCompressorInputStream origin = new ZCompressorInputStream(
                    new BufferedInputStream(Files.newInputStream(this.file))
                )
            ) {
                return FromArchive.unpack(origin);
            } catch (final IOException | ArchiveException ex) {
                throw FromArchive.error(ex);
            }
        }

        /**
         * Reads tar.Z files.
         * @return PackageInfo
         */
        private PackageInfo readBz() {
            try (
                BZip2CompressorInputStream origin = new BZip2CompressorInputStream(
                    new BufferedInputStream(Files.newInputStream(this.file))
                )
            ) {
                return FromArchive.unpack(origin);
            } catch (final IOException | ArchiveException ex) {
                throw FromArchive.error(ex);
            }
        }

        /**
         * Reads metadata from zip, tar or wheel archive.
         * @return PackageInfo
         */
        private PackageInfo readZipTarOrWhl() {
            try (
                ArchiveInputStream input = new ArchiveStreamFactory().createArchiveInputStream(
                    new BufferedInputStream(Files.newInputStream(this.file))
                )
            ) {
                return FromArchive.readArchive(input);
            } catch (final ArchiveException | IOException ex) {
                throw FromArchive.error(ex);
            }
        }

        /**
         * Reads metadata from zip or tar archive.
         * @return PackageInfo
         */
        private PackageInfo readTarGz() {
            try (GzipCompressorInputStream input =
                new GzipCompressorInputStream(Files.newInputStream(this.file));
                TarArchiveInputStream tar = new TarArchiveInputStream(input)) {
                return FromArchive.readArchive(tar);
            } catch (final IOException ex) {
                throw FromArchive.error(ex);
            }
        }

        /**
         * Reads archive from compressor input stream, creates ArchiveInputStream and
         * calls {@link Metadata.FromArchive#readArchive} to extract metadata info.
         * @param origin Origin input stream
         * @return Package info
         * @throws IOException On IO error
         * @throws ArchiveException In case on problems to unpack
         */
        private static PackageInfo unpack(final InputStream origin)
            throws IOException, ArchiveException {
            try (
                ArchiveInputStream input = new ArchiveStreamFactory().createArchiveInputStream(
                    new BufferedInputStream(new ByteArrayInputStream(IOUtils.toByteArray(origin)))
                )
            ) {
                return readArchive(input);
            }
        }

        /**
         * Error.
         * @param err Original exception
         * @return IllegalArgumentException instance
         */
        private static IllegalArgumentException error(final Exception err) {
            return new IllegalArgumentException("Failed to parse python package", err);
        }

        /**
         * Reads archive.
         * @param input Archive to read
         * @return PackageInfo if package info file found
         * @throws IOException On error
         */
        @SuppressWarnings("PMD.AssignmentInOperand")
        private static PackageInfo readArchive(final ArchiveInputStream input) throws IOException {
            ArchiveEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                if (!input.canReadEntryData(entry) || entry.isDirectory()) {
                    continue;
                }
                if (entry.getName().contains("PKG-INFO") || entry.getName().contains("METADATA")) {
                    return new PackageInfo.FromMetadata(
                        IOUtils.toString(input, StandardCharsets.US_ASCII)
                    );
                }
            }
            throw new IllegalArgumentException("Package metadata file not found");
        }

    }

}
