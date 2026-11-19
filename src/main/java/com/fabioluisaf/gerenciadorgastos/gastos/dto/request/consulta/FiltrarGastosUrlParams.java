package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;

public record FiltrarGastosUrlParams(
	LocalDate inicio,
	LocalDate fim,
	UUID metodoPagamentoId,
	String categoria,
	UUID faturaId
) {

	@AssertTrue
	public boolean isIntervaloValido() {
		return inicio == null || fim == null || !inicio.isAfter(fim);
	}
}
