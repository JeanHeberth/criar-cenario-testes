package com.br.criarcenariotestes.business.autoqa.model.scenario;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.List;

/**
 * Lê como texto tanto {@code "uma string"} quanto {@code {"description": "uma
 * string"}}.
 *
 * Existe porque o schema pede lista de strings, mas o modelo enriquece esses
 * campos como objetos quando a entrada é longa — e aí a resposta INTEIRA era
 * descartada no parse, derrubando a análise por um detalhe de formato que não
 * muda o conteúdo. Reforçar a instrução no prompt reduz a frequência, mas não
 * elimina: depende do humor da geração, enquanto isto é determinístico.
 *
 * Só extrai o texto de um objeto; não inventa valor nem aceita tipo
 * arbitrário, então continua rejeitando resposta genuinamente malformada.
 */
public class TextoFlexivelDeserializer extends JsonDeserializer<String> {

    /** Chaves que o modelo usa na prática ao "promover" a string a objeto. */
    private static final List<String> CHAVES_DE_TEXTO =
            List.of("description", "descricao", "text", "texto", "value", "valor", "name", "nome");

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode no = parser.getCodec().readTree(parser);

        if (no == null || no.isNull()) {
            return null;
        }

        if (no.isTextual()) {
            return no.asText();
        }

        if (no.isObject()) {
            for (String chave : CHAVES_DE_TEXTO) {
                JsonNode valor = no.get(chave);
                if (valor != null && valor.isTextual() && !valor.asText().isBlank()) {
                    return valor.asText();
                }
            }
        }

        if (no.isNumber() || no.isBoolean()) {
            return no.asText();
        }

        return context.reportInputMismatch(String.class,
                "Esperava texto (ou objeto com um campo de texto), veio: %s", no.getNodeType());
    }
}
