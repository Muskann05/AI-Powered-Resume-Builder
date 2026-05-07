package com.resumeai.export.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;

@Service
public class JsonGeneratorService {

    public File generateJson(String resumeDataJson, File targetFile) throws Exception {
        try (FileWriter writer = new FileWriter(targetFile)) {
            writer.write(resumeDataJson);
        }
        return targetFile;
    }
}
