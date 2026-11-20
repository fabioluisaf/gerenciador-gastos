package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao;

public record UsoCategoriaResponse(
    String categoria,
    Long quantidadeGastos
) {
}
