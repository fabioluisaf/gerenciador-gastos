package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fabioluisaf.gerenciadorgastos.gastos.validation.EscopoTotalizacaoValido;
import com.fabioluisaf.gerenciadorgastos.shared.validation.AnoMinimo;

@Schema(description = "Informe periodo OU faturaId. Totais ignoram filtros de categoria e metodo de pagamento.")
@EscopoTotalizacaoValido
public record TotalizarGastosRequest(
		@AnoMinimo(2000)
		@Schema(type = "string", example = "2026-11", description = "Periodo: AAAA-MM, MM-AAAA, AAAA.MM ou MM.AAAA.")
		YearMonth periodo,
		UUID faturaId
) {

	public LocalDate inicio() {
		return faturaId == null ? periodo.atDay(1) : null;
	}

	public LocalDate fim() {
		return faturaId == null ? periodo.atEndOfMonth() : null;
	}
}
