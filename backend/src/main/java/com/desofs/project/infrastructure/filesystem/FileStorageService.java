package com.desofs.project.infrastructure.filesystem;

import com.desofs.project.shared.exceptions.InvalidFilePathException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Service
@Slf4j
public class FileStorageService implements IFileStorageService {

    @Value("${app.file-storage.path:./uploads}")
    private String baseStoragePath;

    @Override
    public void createStorageDirectory(String directoryPath) {
        Path basePath = Paths.get(baseStoragePath).toAbsolutePath().normalize();
        Path resolved = basePath.resolve(Paths.get(directoryPath).normalize()).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new InvalidFilePathException();
        }
        try {
            Files.createDirectories(resolved);
        } catch (IOException e) {
            log.error("Failed to create storage directory.", e);
            throw new RuntimeException("Failed to create storage directory", e);
        }
    }

    @Override
    public String storeFile(String directory, String filename, byte[] content) {
        Path basePath = Paths.get(baseStoragePath).toAbsolutePath().normalize();
        Path dirPath = Paths.get(directory).toAbsolutePath().normalize();
        if (!dirPath.startsWith(basePath)) {
            throw new InvalidFilePathException();
        }

        try {
            Files.createDirectories(dirPath);
        } catch (IOException e) {
            log.error("Failed to create storage directory for file storage.", e);
            throw new RuntimeException("Failed to create storage directory", e);
        }

        Path filePath = dirPath.resolve(filename).normalize();
        if (!filePath.startsWith(dirPath)) {
            throw new InvalidFilePathException();
        }

        try {
            Files.write(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to store file '{}' in directory '{}'.", filename, dirPath, e);
            throw new RuntimeException("Failed to store file", e);
        }

        return filePath.toAbsolutePath().toString();
    }

    @Override
    public byte[] readFile(String filepath)  {
        Path basePath = Paths.get(baseStoragePath).toAbsolutePath().normalize();
        Path resolved = Paths.get(filepath).toAbsolutePath().normalize();
        if (!resolved.startsWith(basePath)) {
            throw new InvalidFilePathException();
        }
        try {
            return Files.readAllBytes(resolved);
        } catch (IOException e) {
            log.error("Failed to read stored file.", e);
            throw new RuntimeException("Failed to read file", e);
        }
    }

    @Override
    public void deleteFile(String filepath) {
        Path basePath = Paths.get(baseStoragePath).toAbsolutePath().normalize();
        Path resolved = Paths.get(filepath).toAbsolutePath().normalize();
        if (!resolved.startsWith(basePath)) {
            throw new InvalidFilePathException();
        }

        try {
            Files.deleteIfExists(resolved);
        } catch (IOException e) {
            log.error("Failed to delete stored file.", e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }
}
