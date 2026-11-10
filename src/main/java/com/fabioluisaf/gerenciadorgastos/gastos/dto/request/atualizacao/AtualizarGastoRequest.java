package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.atualizacao;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fabioluisaf.gerenciadorgastos.shared.serialization.DataDeserializer;
import com.fabioluisaf.gerenciadorgastos.shared.validation.DataObrigatoriaSeRealizado;
import com.fabioluisaf.gerenciadorgastos.shared.validation.GastoComDataERealizacao;
import tools.jackson.databind.annotation.JsonDeserialize;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

@DataObrigatoriaSeRealizado
public record AtualizarGastoRequest(
    @JsonDeserialize(using = DataDeserializer.class)
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

    @JsonDeserialize(using = DataDeserializer.class)
    LocalDate faturaDataFechamento,

    String descricao,

    @NotNull
    Boolean realizado
) implements GastoComDataERealizacao {
}
