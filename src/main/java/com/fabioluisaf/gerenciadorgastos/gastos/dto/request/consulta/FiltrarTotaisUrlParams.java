package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record FiltrarTotaisUrlParams(

	@NotNull
	LocalDate inicio,

	@NotNull
	LocalDate fim,

	List<UUID> metodoPagamentoId,

	List<String> categoria,

	String nome
) {

	@AssertTrue(message = "INTERVALO_INVALIDO")
	public boolean isIntervaloValido() {
		return inicio == null || fim == null || !inicio.isAfter(fim);
	}
}
