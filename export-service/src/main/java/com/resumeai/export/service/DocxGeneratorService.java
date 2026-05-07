package com.resumeai.export.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;

@Service
public class DocxGeneratorService {

    private final ObjectMapper objectMapper;

    public DocxGeneratorService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public File generateDocx(String resumeDataJson, File targetFile) throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(targetFile)) {
            JsonNode root = objectMapper.readTree(resumeDataJson);

            XWPFParagraph heading = document.createParagraph();
            XWPFRun headingRun = heading.createRun();
            headingRun.setBold(true);
            headingRun.setFontSize(18);
            headingRun.setText(root.path("title").asText("Resume Export"));

            XWPFParagraph subHeading = document.createParagraph();
            subHeading.createRun().setText(root.path("targetJobTitle").asText(""));

            for (JsonNode section : root.path("sections")) {
                XWPFParagraph sectionTitle = document.createParagraph();
                XWPFRun titleRun = sectionTitle.createRun();
                titleRun.setBold(true);
                titleRun.setText(section.path("title").asText("Section"));

                XWPFParagraph body = document.createParagraph();
                body.createRun().setText(section.path("content").asText(""));
            }

            document.write(out);
        }
        return targetFile;
    }
}
