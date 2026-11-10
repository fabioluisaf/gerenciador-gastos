package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record FaturaResumidaResponse(
		UUID id,
		LocalDate dataFechamento,
		@Schema(description = "Soma apenas os gastos realizados; zero quando nao houver gastos realizados.")
		BigDecimal valorTotal
) {
}
