package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta;

import java.time.YearMonth;

import com.fabioluisaf.gerenciadorgastos.shared.validation.AnoMinimo;

import jakarta.validation.constraints.NotNull;

public record BuscarGastosRequest(
    @NotNull
    @AnoMinimo(2000)
    YearMonth periodo
) {
}
