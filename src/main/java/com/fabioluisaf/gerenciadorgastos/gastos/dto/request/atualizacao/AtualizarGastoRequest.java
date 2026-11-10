package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.atualizacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import com.fabioluisaf.gerenciadorgastos.shared.serialization.DataDeserializer;
import com.fabioluisaf.gerenciadorgastos.shared.serialization.PeriodoDeserializer;
import com.fabioluisaf.gerenciadorgastos.shared.validation.DataObrigatoriaSeRealizado;
import com.fabioluisaf.gerenciadorgastos.shared.validation.GastoComDataERealizacao;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

@DataObrigatoriaSeRealizado
public record AtualizarGastoRequest(
    @JsonDeserialize(using = DataDeserializer.class)
    @Schema(type = "string", example = "10/11/2026", description = "Data da compra: DD/MM/AAAA, DD-MM-AAAA ou AAAA-MM-DD.")
    LocalDate data,

    @NotBlank
    String nome,

    @NotNull
    @PositiveOrZero
    BigDecimal valor,

    @NotBlank
    String categoria,

    @NotNull
    UUID metodoPagamentoId,

    @JsonDeserialize(using = PeriodoDeserializer.class)
    @Schema(type = "string", example = "2026-11", description = "Periodo da fatura: AAAA-MM, MM-AAAA, AAAA.MM, MM.AAAA ou MM/AAAA.")
    YearMonth faturaPeriodo,

    String descricao,

    @NotNull
    Boolean realizado
) implements GastoComDataERealizacao {
}
