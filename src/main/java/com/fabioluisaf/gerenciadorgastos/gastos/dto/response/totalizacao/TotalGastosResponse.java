package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao;

import java.math.BigDecimal;

public record TotalGastosResponse(
	BigDecimal realizado,
	BigDecimal planejado
) {
}
