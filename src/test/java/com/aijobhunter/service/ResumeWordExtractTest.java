package com.aijobhunter.service;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumeWordExtractTest {

    @TempDir
    Path tempDir;

    @Test
    void extractWordText_fromDocx_returnsBody() throws Exception {
        Path docx = tempDir.resolve("sample.docx");
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph p = doc.createParagraph();
            XWPFRun run = p.createRun();
            run.setText("AI Job Hunter 简历测试段落");
            try (var out = Files.newOutputStream(docx)) {
                doc.write(out);
            }
        }

        String text = ResumeService.extractWordText(docx, "docx");
        assertNotNull(text);
        assertTrue(text.contains("AI Job Hunter"), "extracted=" + text);
    }

    @Test
    void extractWordText_corruptFile_returnsNull() throws Exception {
        Path bad = tempDir.resolve("bad.docx");
        Files.writeString(bad, "not-a-real-docx");
        String text = ResumeService.extractWordText(bad, "docx");
        assertTrue(text == null || text.isBlank());
    }
}
