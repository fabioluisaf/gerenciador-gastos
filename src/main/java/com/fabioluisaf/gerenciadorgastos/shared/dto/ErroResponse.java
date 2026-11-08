package com.fabioluisaf.gerenciadorgastos.shared.dto;

import java.time.Instant;

public record ErroResponse(
    int status,
    String mensagem,
    String caminho,
    Instant instante
) {
}
