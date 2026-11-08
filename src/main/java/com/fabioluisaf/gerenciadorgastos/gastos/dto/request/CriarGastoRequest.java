package com.fabioluisaf.gerenciadorgastos.gastos.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fabioluisaf.gerenciadorgastos.gastos.model.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.Gasto;
import com.fabioluisaf.gerenciadorgastos.shared.constantes.FormatoData;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CriarGastoRequest(
    @NotNull
    @JsonFormat(
        pattern = "[" + FormatoData.DIA_MES_ANO + "]["
                      + FormatoData.DIA_MES_ANO_HIFEN + "]["
                      + FormatoData.ANO_MES_DIA_HIFEN + "]"
    )
    LocalDate data,

    @NotBlank
    String nome,

    @NotNull
    @PositiveOrZero
    BigDecimal valor,

    @NotBlank
    String categoria,

    @NotNull
    MetodoPagamento metodoPagamento,

    String descricao,

    @NotNull
    Boolean realizado
) {
    public Gasto toEntity() {
        return new Gasto(null, data, nome, valor, categoria, metodoPagamento, descricao, realizado);
    }
}
