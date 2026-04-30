package com.br.criarcenariotestes.business.document;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class PdfTextExtractor {

    public String extrairTexto(InputStream inputStream) {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            String texto = stripper.getText(document);

            return limparTexto(texto);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao ler PDF", e);
        }
    }

    private String limparTexto(String texto) {
        return texto
                .replaceAll("\\s{2,}", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}