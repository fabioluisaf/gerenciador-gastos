package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao;

import java.math.BigDecimal;

public record TotalCategoriaResponse(
    String categoria, 
    BigDecimal realizado, 
    BigDecimal planejado
) {
}
