package com.desofs.project.infrastructure.filesystem;

import com.desofs.project.shared.exceptions.InvalidFilePathException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storageOperations_WithValidPaths_CreateStoreReadAndDeleteFile() throws Exception {
        FileStorageService service = createService();
        Path documentsDirectory = tempDir.resolve("documents");
        byte[] content = "report contents".getBytes(StandardCharsets.UTF_8);

        service.createStorageDirectory("documents");
        String storedPath = service.storeFile(documentsDirectory.toString(), "report.txt", content);
        byte[] storedContent = service.readFile(storedPath);
        service.deleteFile(storedPath);

        assertThat(Files.isDirectory(documentsDirectory)).isTrue();
        assertThat(Path.of(storedPath)).isEqualTo(documentsDirectory.resolve("report.txt").toAbsolutePath().normalize());
        assertThat(storedContent).isEqualTo(content);
        assertThat(Files.exists(Path.of(storedPath))).isFalse();
    }

    @Test
    void createStorageDirectory_WhenPathTraversalDetected_ThrowsSecurityException() {
        FileStorageService service = createService();
        Path outside = tempDir.resolve("..").resolve("outside");

        assertThatThrownBy(() -> service.createStorageDirectory(outside.toString()))
                .isInstanceOf(InvalidFilePathException.class);
    }

    @Test
    void createStorageDirectory_WhenParentIsFile_ThrowsRuntimeException() throws Exception {
        Path baseFile = Files.writeString(tempDir.resolve("storage-file"), "not a directory");
        FileStorageService service = createService(baseFile);

        assertThatThrownBy(() -> service.createStorageDirectory("documents"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create storage directory");
    }

    @Test
    void storeFile_WhenDirectoryCannotBeCreated_ThrowsRuntimeException() throws Exception {
        FileStorageService service = createService();
        Path parentFile = Files.writeString(tempDir.resolve("existing-file"), "not a directory");
        Path impossibleDirectory = parentFile.resolve("documents");

        assertThatThrownBy(() -> service.storeFile(impossibleDirectory.toString(), "report.txt", "data".getBytes()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create storage directory");
    }

    @Test
    void storeFile_WhenFilenameTraversalDetected_ThrowsSecurityException() {
        FileStorageService service = createService();

        assertThatThrownBy(() -> service.storeFile(tempDir.toString(), "../evil.txt", "data".getBytes()))
                .isInstanceOf(InvalidFilePathException.class);
    }

    @Test
    void storeFile_WhenTargetIsDirectory_ThrowsRuntimeException() throws Exception {
        FileStorageService service = createService();
        Files.createDirectory(tempDir.resolve("report.txt"));

        assertThatThrownBy(() -> service.storeFile(tempDir.toString(), "report.txt", "data".getBytes()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to store file");
    }

    @Test
    void readFile_WhenPathTraversalDetected_ThrowsSecurityException() {
        FileStorageService service = createService();
        Path outside = tempDir.resolve("..").resolve("outside.txt");

        assertThatThrownBy(() -> service.readFile(outside.toString()))
                .isInstanceOf(InvalidFilePathException.class);
    }

    @Test
    void readFile_WhenFileDoesNotExist_ThrowsRuntimeException() {
        FileStorageService service = createService();
        Path missingFile = tempDir.resolve("missing.txt");

        assertThatThrownBy(() -> service.readFile(missingFile.toString()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to read file");
    }

    @Test
    void deleteFile_WhenPathTraversalDetected_ThrowsSecurityException() {
        FileStorageService service = createService();
        Path outside = tempDir.resolve("..").resolve("outside.txt");

        assertThatThrownBy(() -> service.deleteFile(outside.toString()))
                .isInstanceOf(InvalidFilePathException.class);
    }

    @Test
    void deleteFile_WhenDirectoryIsNotEmpty_ThrowsRuntimeException() throws Exception {
        FileStorageService service = createService();
        Path directory = Files.createDirectory(tempDir.resolve("not-empty"));
        Files.writeString(directory.resolve("nested.txt"), "content");

        assertThatThrownBy(() -> service.deleteFile(directory.toString()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to delete file");
    }

    private FileStorageService createService() {
        return createService(tempDir);
    }

    private FileStorageService createService(Path baseStoragePath) {
        FileStorageService service = new FileStorageService();
        ReflectionTestUtils.setField(service, "baseStoragePath", baseStoragePath.toString());
        return service;
    }
}
