package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BuscarGastosRequest(
    @NotNull
    @Min(1)
    @Max(12)
    Integer mes,

    @NotNull
    @Min(2000)
    Integer ano
) {
}
