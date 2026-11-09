package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.gasto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Realizado soma gastos realizados; planejado soma apenas os nao realizados.")
public record TotalCategoriaResponse(String categoria, BigDecimal realizado, BigDecimal planejado) {
}
