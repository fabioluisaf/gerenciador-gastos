package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao;

import java.time.YearMonth;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.shared.serialization.PeriodoDeserializer;
import com.fabioluisaf.gerenciadorgastos.shared.validation.AnoMinimo;

import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

public record CriarFaturaRequest(
		@NotNull UUID metodoPagamentoId,
		@NotNull @AnoMinimo(2000) @JsonDeserialize(using = PeriodoDeserializer.class) YearMonth periodo
) {
}
