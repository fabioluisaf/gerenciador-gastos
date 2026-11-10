package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import com.fabioluisaf.gerenciadorgastos.gastos.model.CompraParcelada;
import com.fabioluisaf.gerenciadorgastos.shared.serialization.DataDeserializer;
import com.fabioluisaf.gerenciadorgastos.shared.serialization.PeriodoDeserializer;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(type = "string", example = "10/11/2026", description = "Data da compra: DD/MM/AAAA, DD-MM-AAAA ou AAAA-MM-DD.")
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
    @Schema(type = "string", example = "2026-11", description = "Periodo da primeira fatura: AAAA-MM, MM-AAAA, AAAA.MM, MM.AAAA ou MM/AAAA.")
    YearMonth faturaPeriodo,

    String descricao
) {
    public CompraParcelada toEntity() {
        final var compra = new CompraParcelada();
        compra.setValorTotal(valorTotal);

        return compra;
    }
}
