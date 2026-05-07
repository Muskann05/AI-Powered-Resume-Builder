package com.resumeai.export.service;

import com.itextpdf.html2pdf.HtmlConverter;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;

@Service
public class PdfGeneratorService {

    public File generatePdf(String html, File targetFile) throws Exception {
        try (FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            HtmlConverter.convertToPdf(html, outputStream);
        }
        return targetFile;
    }
}
