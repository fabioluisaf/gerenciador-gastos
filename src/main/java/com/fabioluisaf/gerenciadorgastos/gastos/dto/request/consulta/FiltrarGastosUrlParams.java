package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;

public record FiltrarGastosUrlParams(
	LocalDate inicio,
	LocalDate fim,
	List<UUID> metodoPagamentoId,
	List<String> categoria,
	UUID faturaId
) {

	@AssertTrue(message = "INTERVALO_INVALIDO")
	public boolean isIntervaloValido() {
		return inicio == null || fim == null || !inicio.isAfter(fim);
	}
}
