package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.gastos.validation.EscopoTotalizacaoValido;
import com.fabioluisaf.gerenciadorgastos.shared.validation.AnoMinimo;

@EscopoTotalizacaoValido
public record TotalizarGastosRequest(
		@AnoMinimo(2000)
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
