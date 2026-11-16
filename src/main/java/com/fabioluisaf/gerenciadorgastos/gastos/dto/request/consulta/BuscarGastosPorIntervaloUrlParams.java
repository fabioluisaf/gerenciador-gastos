package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta;

import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record BuscarGastosPorIntervaloUrlParams(

	@NotNull
	LocalDate inicio,

	@NotNull
	LocalDate fim
) {
	@AssertTrue
	public boolean isIntervaloValido() {
		return inicio == null || fim == null || !inicio.isAfter(fim);
	}
}
