package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.compraparcelada;

import java.util.UUID;

public record InformacoesParcelaResponse(
    Integer numero,
    Long quantidade,
    UUID compraParceladaId
) {
}
