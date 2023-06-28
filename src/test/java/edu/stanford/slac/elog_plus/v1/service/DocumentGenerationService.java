package edu.stanford.slac.elog_plus.v1.service;

import com.github.javafaker.Faker;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
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

    public PDDocument generatePdf() throws IOException {
        Faker faker = new Faker();
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();

        document.addPage(page);
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE);
        List<String> paragraphs = faker.lorem().paragraphs(10);
        float yStart = page.getMediaBox().getHeight() - MARGIN;
        float xPosition = MARGIN;
        float yPosition = yStart;

        contentStream.beginText();
        contentStream.newLineAtOffset(xPosition, yPosition);

        for (String paragraph : paragraphs) {
            String[] words = paragraph.split(" ");
            for (String word : words) {
                String text = word + " ";
                float textWidth = FONT_SIZE * PDType1Font.HELVETICA.getStringWidth(text) / 1000;
                xPosition += textWidth;

                if (xPosition + textWidth > page.getMediaBox().getWidth() - MARGIN) {
                    contentStream.newLineAtOffset(-xPosition + MARGIN, -LEADING);
                    xPosition = MARGIN;
                    yPosition -= LEADING;

                    if (yPosition < MARGIN) {
                        contentStream.endText();
                        contentStream.close();

                        page = new PDPage();
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE);
                        contentStream.beginText();
                        contentStream.newLineAtOffset(MARGIN, yStart);

                        yPosition = yStart;
                    }
                }

                contentStream.showText(text);
            }
            contentStream.newLineAtOffset(-xPosition + MARGIN, -LEADING);
            xPosition = MARGIN;
            yPosition -= LEADING;
        }

        contentStream.endText();
        contentStream.close();
        return document;
    }
}
