package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FaturaResumidaResponse(
		UUID id,

		LocalDate dataFechamento,

		LocalDate dataVencimento,

		BigDecimal valorTotal
) {
}
