package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.gasto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Realizado soma gastos realizados; planejado soma todos os gastos, realizados ou nao.")
public record TotalCategoriaResponse(String categoria, BigDecimal realizado, BigDecimal planejado) {
}
