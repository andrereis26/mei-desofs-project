package com.desofs.project.infrastructure.filesystem;

public interface IFileStorageService {
    
    void createStorageDirectory(String directoryPath);

    String storeFile(String directory, String filename, byte[] content);

    byte[] readFile(String filepath);

    void deleteFile(String filepath);
}
