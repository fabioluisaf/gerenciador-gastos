package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fabioluisaf.gerenciadorgastos.shared.serialization.DataDeserializer;
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

    @JsonDeserialize(using = DataDeserializer.class)
    LocalDate faturaDataFechamento,

    String descricao
) {
}
