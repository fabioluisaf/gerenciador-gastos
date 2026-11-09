package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.gasto;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Informe mes e ano OU faturaId. Totais ignoram filtros de categoria e metodo de pagamento.")
public record TotalizarGastosRequest(
		@Min(1) @Max(12) Integer mes,
		@Min(2000) Integer ano,
		UUID faturaId
) {

	@AssertTrue(message = "INFORME_MES_E_ANO_OU_FATURA")
	@Schema(hidden = true)
	public boolean isEscopoValido() {
		if (faturaId != null) {
			return mes == null && ano == null;
		}

		return mes != null && ano != null;
	}

	public LocalDate inicio() {
		return faturaId == null ? YearMonth.of(ano, mes).atDay(1) : null;
	}

	public LocalDate fim() {
		return faturaId == null ? YearMonth.of(ano, mes).atEndOfMonth() : null;
	}
}
