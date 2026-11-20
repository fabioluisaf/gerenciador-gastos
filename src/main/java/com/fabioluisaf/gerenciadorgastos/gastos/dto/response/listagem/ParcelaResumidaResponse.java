package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem;

import java.math.BigDecimal;
import java.util.UUID;

public record ParcelaResumidaResponse(
    Integer numero,
    Long quantidade,
    UUID compraParceladaId,
    BigDecimal valorTotalCompra
) {
}
