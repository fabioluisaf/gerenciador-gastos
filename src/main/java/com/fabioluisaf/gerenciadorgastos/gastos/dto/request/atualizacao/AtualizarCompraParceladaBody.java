package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.atualizacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.shared.serialization.DataDeserializer;
import com.fabioluisaf.gerenciadorgastos.shared.serialization.PeriodoDeserializer;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.annotation.JsonDeserialize;

public record AtualizarCompraParceladaBody(
    
    @NotBlank 
    @Size(max = 255)
    String nome,

    @NotBlank 
    String categoria,

    @NotNull
    UUID metodoPagamentoId,

    @JsonDeserialize(using = PeriodoDeserializer.class)
    YearMonth faturaPeriodo,

    @Size(max = 255)
    String descricao,

    @NotNull 
    @PositiveOrZero 
    @Digits(integer = 17, fraction = 2) 
    BigDecimal valorTotal,

    @NotNull 
    @Positive 
    @Max(1000)
    Integer quantidadeParcelas,

    @NotNull
    @JsonDeserialize(using = DataDeserializer.class)
    LocalDate data,

    List<@NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal> valoresParcelas,

    @NotNull
    Boolean realizado
) {
}
