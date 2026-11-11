package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import com.fabioluisaf.gerenciadorgastos.gastos.model.CompraParcelada;
import com.fabioluisaf.gerenciadorgastos.shared.serialization.DataDeserializer;
import com.fabioluisaf.gerenciadorgastos.shared.serialization.PeriodoDeserializer;

import tools.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

public record CriarCompraParceladaRequest(
    @NotNull
    @JsonDeserialize(using = DataDeserializer.class)
    LocalDate data,

    @NotBlank
    String nome,

    @NotNull
    @PositiveOrZero
    @Digits(integer = 17, fraction = 2)
    BigDecimal valorTotal,

    @NotNull
    @Positive
    Integer quantidadeParcelas,

    @NotBlank
    String categoria,

    @NotNull
    UUID metodoPagamentoId,

    @JsonDeserialize(using = PeriodoDeserializer.class)
    YearMonth faturaPeriodo,

    String descricao
) {
    public CompraParcelada toEntity() {
        final var compra = new CompraParcelada();
        compra.setValorTotal(valorTotal);

        return compra;
    }
}
