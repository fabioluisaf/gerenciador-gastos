package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.fatura;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FaturaResumidaResponse(
		UUID id,
		LocalDate dataFechamento,
		BigDecimal valorTotal
) {
}
