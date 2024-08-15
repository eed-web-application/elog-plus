package edu.stanford.slac.elog_plus.v1.service;

import com.github.javafaker.Faker;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;

@Service
public class DocumentGenerationService {
    private static final float MARGIN = 50;
    private static final float FONT_SIZE = 12;
    private static final float LEADING = 1.5f * FONT_SIZE;

    public InputStream getTestJpeg() throws URISyntaxException {
        return getClass().getClassLoader().getResourceAsStream("test.jpg");
    }

    public InputStream getTestPng() throws URISyntaxException {
        return getClass().getClassLoader().getResourceAsStream("test.png");
    }

    public InputStream getTestPS() throws URISyntaxException {
        return getClass().getClassLoader().getResourceAsStream("test.ps");
    }

    public InputStream getTestPSAlternateMimeType() throws URISyntaxException {
        return getClass().getClassLoader().getResourceAsStream("postscript-alternate-mime-type.ps");
    }

    public PDDocument generatePdf() throws IOException {
        Faker faker = new Faker();
        PDDocument doc = new PDDocument();
        List<String> paragraphs = faker.lorem().paragraphs(10);

        PDPage page = new PDPage();
        doc.addPage(page);

        try (PDPageContentStream contents = new PDPageContentStream(doc, page)) {
            contents.beginText();
            contents.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14);
            contents.newLineAtOffset(100, 700);

            for (String paragraph : paragraphs) {
                contents.showText(paragraph);
                contents.newLineAtOffset(0, -15); // Move to the next line
            }
            contents.endText();
        } catch (IOException e) {
            System.err.println("Exception while trying to create pdf document - " + e);
        }
        return doc;
    }
}