package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record BuscarGastosPorIntervaloRequest(
		@NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
		@NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
) {
	@AssertTrue
	public boolean isIntervaloValido() {
		return inicio == null || fim == null || !inicio.isAfter(fim);
	}
}
