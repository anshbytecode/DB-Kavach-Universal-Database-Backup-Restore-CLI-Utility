package com.dbbackup.service.compression;

import com.dbbackup.model.CompressionType;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class CompressionService {
    private static final Logger log = LoggerFactory.getLogger(CompressionService.class);

    public File compress(File sourceFile, CompressionType compressionType, File outputFile) throws IOException {
        if (compressionType == CompressionType.NONE) {
            return sourceFile;
        }

        log.info("Compressing {} using {}", sourceFile.getName(), compressionType);

        switch (compressionType) {
            case GZIP:
                compressGzip(sourceFile, outputFile);
                break;
            case ZIP:
                compressZip(sourceFile, outputFile);
                break;
            case TAR_GZ:
                compressTarGz(sourceFile, outputFile);
                break;
            default:
                throw new IllegalArgumentException("Unsupported compression type: " + compressionType);
        }

        return outputFile;
    }

    public File decompress(File compressedFile, CompressionType compressionType, File targetDir) throws IOException {
        if (compressionType == CompressionType.NONE) {
            return compressedFile;
        }

        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        log.info("Decompressing {} using {}", compressedFile.getName(), compressionType);

        switch (compressionType) {
            case GZIP:
                return decompressGzip(compressedFile, targetDir);
            case ZIP:
                return decompressZip(compressedFile, targetDir);
            case TAR_GZ:
                return decompressTarGz(compressedFile, targetDir);
            default:
                throw new IllegalArgumentException("Unsupported compression type: " + compressionType);
        }
    }

    private void compressGzip(File source, File target) throws IOException {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new GZIPOutputStream(new FileOutputStream(target))) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    private File decompressGzip(File source, File targetDir) throws IOException {
        String decompressedName = source.getName();
        if (decompressedName.endsWith(".gz")) {
            decompressedName = decompressedName.substring(0, decompressedName.length() - 3);
        } else {
            decompressedName += ".raw";
        }
        File targetFile = new File(targetDir, decompressedName);
        try (InputStream in = new GZIPInputStream(new FileInputStream(source));
             OutputStream out = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        return targetFile;
    }

    private void compressZip(File source, File target) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(target));
             InputStream in = new FileInputStream(source)) {
            ZipEntry entry = new ZipEntry(source.getName());
            zos.putNextEntry(entry);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                zos.write(buffer, 0, bytesRead);
            }
            zos.closeEntry();
        }
    }

    private File decompressZip(File source, File targetDir) throws IOException {
        File firstDecompressedFile = null;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(source))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File targetFile = new File(targetDir, entry.getName());
                if (firstDecompressedFile == null) firstDecompressedFile = targetFile;
                try (OutputStream out = new FileOutputStream(targetFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = zis.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                zis.closeEntry();
            }
        }
        return firstDecompressedFile != null ? firstDecompressedFile : new File(targetDir, "decompressed.sql");
    }

    private void compressTarGz(File source, File target) throws IOException {
        try (OutputStream fos = new FileOutputStream(target);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             GzipCompressorOutputStream gzos = new GzipCompressorOutputStream(bos);
             TarArchiveOutputStream tos = new TarArchiveOutputStream(gzos)) {
            TarArchiveEntry entry = new TarArchiveEntry(source, source.getName());
            tos.putArchiveEntry(entry);
            try (InputStream in = new FileInputStream(source)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    tos.write(buffer, 0, bytesRead);
                }
            }
            tos.closeArchiveEntry();
        }
    }

    private File decompressTarGz(File source, File targetDir) throws IOException {
        File firstFile = null;
        try (InputStream fis = new FileInputStream(source);
             BufferedInputStream bis = new BufferedInputStream(fis);
             GzipCompressorInputStream gzis = new GzipCompressorInputStream(bis);
             TarArchiveInputStream tis = new TarArchiveInputStream(gzis)) {
            TarArchiveEntry entry;
            while ((entry = tis.getNextEntry()) != null) {
                File outputFile = new File(targetDir, entry.getName());
                if (firstFile == null) firstFile = outputFile;
                try (OutputStream out = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = tis.read(buffer, 0, buffer.length)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
            }
        }
        return firstFile != null ? firstFile : new File(targetDir, "decompressed.sql");
    }
}
