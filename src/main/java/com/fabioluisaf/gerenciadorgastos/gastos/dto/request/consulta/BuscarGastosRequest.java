package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta;

import java.time.YearMonth;

import com.fabioluisaf.gerenciadorgastos.shared.validation.AnoMinimo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record BuscarGastosRequest(
    @NotNull
    @AnoMinimo(2000)
    @Schema(type = "string", example = "2026-11", description = "Periodo: AAAA-MM, MM-AAAA, AAAA.MM ou MM.AAAA.")
    YearMonth periodo
) {
}
