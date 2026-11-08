package com.fabioluisaf.gerenciadorgastos.shared.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ErroResponse(
    int status,
    String mensagem,
    String caminho,
    Instant instante,

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<ErroValidacao> errosValidacao
) {
}
