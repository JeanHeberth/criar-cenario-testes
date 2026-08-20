package com.br.criarcenariotestes.controller;

import com.br.criarcenariotestes.business.autoqa.navegacao.NavegacaoPastasResponse;
import com.br.criarcenariotestes.business.autoqa.navegacao.NavegacaoPastasService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Alimenta o seletor de pastas da tela do Auto QA, para o usuário escolher o
 * diretório do projeto em vez de digitar o caminho inteiro.
 *
 * Só faz sentido quando a API roda na mesma máquina do usuário — que é o modo
 * de desenvolvimento local. Numa API central, as pastas listadas seriam as do
 * servidor, não as de quem está na tela; nesse cenário a origem do projeto
 * precisa vir de repositório, não de caminho.
 *
 * O alcance é o mesmo de auto-qa.allowed-roots, que já governa o que o Auto QA
 * pode executar: este endpoint não amplia permissão, apenas deixa navegar o
 * que já era permitido. Com a lista vazia (default), não navega nada.
 */
@RestController
@RequestMapping("/api/auto-qa/pastas")
@RequiredArgsConstructor
public class AutoQaPastasController {

    private final NavegacaoPastasService navegacaoPastasService;

    @GetMapping
    public NavegacaoPastasResponse listar(@RequestParam(required = false) String caminho) {
        return navegacaoPastasService.listar(caminho);
    }
}
