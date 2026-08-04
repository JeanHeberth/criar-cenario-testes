package com.br.criarcenariotestes.business.autoqa.discovery.parser;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Component
public class MavenPomParser {

    public void parse(Path file, String relativePath, ParsedProjectFiles.Builder builder) {
        builder.mavenPom(true);
        builder.mavenPomPath(relativePath);
        try {
            DocumentBuilderFactory factory = secureDocumentBuilderFactory();
            factory.setNamespaceAware(true);
            Document document;
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                document = factory.newDocumentBuilder().parse(new InputSource(reader));
            }
            NodeList dependencies = document.getElementsByTagNameNS("*", "dependency");
            for (int i = 0; i < dependencies.getLength(); i++) {
                Node node = dependencies.item(i);
                if (node instanceof Element element) {
                    String groupId = textContent(element, "groupId");
                    String artifactId = textContent(element, "artifactId");
                    builder.mavenDependencies().add((groupId + ":" + artifactId).toLowerCase(Locale.ROOT));
                }
            }
        } catch (IOException | ParserConfigurationException | SAXException exception) {
            builder.warnings().add("pom.xml inválido: " + exception.getMessage());
        }
    }

    private DocumentBuilderFactory secureDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private String textContent(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagNameNS("*", tagName);
        if (nodes.getLength() == 0) {
            return "";
        }
        Node node = nodes.item(0);
        return node == null ? "" : node.getTextContent().trim();
    }
}
