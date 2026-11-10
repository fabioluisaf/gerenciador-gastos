package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.atualizacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.shared.serialization.DataDeserializer;
import com.fabioluisaf.gerenciadorgastos.shared.serialization.PeriodoDeserializer;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record AtualizarCompraParceladaRequest(
    @NotBlank 
    String nome,

    @NotBlank 
    String categoria,

    @NotNull
    UUID metodoPagamentoId,

    @JsonDeserialize(using = PeriodoDeserializer.class)
    @Schema(type = "string", example = "2026-11", description = "Periodo da primeira fatura: AAAA-MM, MM-AAAA, AAAA.MM, MM.AAAA ou MM/AAAA.")
    YearMonth faturaPeriodo,

    String descricao,

    @NotNull 
    @PositiveOrZero 
    @Digits(integer = 17, fraction = 2) 
    BigDecimal valorTotal,

    @NotNull 
    @Positive 
    Integer quantidadeParcelas,

    @NotNull
    @JsonDeserialize(using = DataDeserializer.class)
    @Schema(type = "string", example = "10/11/2026", description = "Data da compra: DD/MM/AAAA, DD-MM-AAAA ou AAAA-MM-DD.")
    LocalDate data,

    @Schema(description = "Valores opcionais na ordem das parcelas, com quantidade e soma iguais as da compra. "
            + "Se omitidos, preserva os valores atuais ou recalcula quando o total ou a quantidade mudar.")
    List<@NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal> valoresParcelas
) {
}
