package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.atualizacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.shared.serialization.DataDeserializer;
import com.fabioluisaf.gerenciadorgastos.shared.serialization.PeriodoDeserializer;
import com.fabioluisaf.gerenciadorgastos.shared.validation.DataObrigatoriaSeRealizado;
import com.fabioluisaf.gerenciadorgastos.shared.validation.RequestGastoComDataERealizacao;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.annotation.JsonDeserialize;

@DataObrigatoriaSeRealizado
public record AtualizarGastoBody(
    
    @JsonDeserialize(using = DataDeserializer.class)
    LocalDate data,

    @NotBlank
    @Size(max = 255)
    String nome,

    @NotNull
    @PositiveOrZero
    @Digits(integer = 17, fraction = 2)
    BigDecimal valor,

    @NotBlank
    String categoria,

    @NotNull
    UUID metodoPagamentoId,

    @JsonDeserialize(using = PeriodoDeserializer.class)
    YearMonth faturaPeriodo,

    @Size(max = 255)
    String descricao,

    @NotNull
    Boolean realizado
) implements RequestGastoComDataERealizacao {
}
