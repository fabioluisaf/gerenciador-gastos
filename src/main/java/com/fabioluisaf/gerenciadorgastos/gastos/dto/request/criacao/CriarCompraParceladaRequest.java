package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fabioluisaf.gerenciadorgastos.shared.constantes.FormatoData;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

public record CriarCompraParceladaRequest(
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
    @Digits(integer = 17, fraction = 2)
    BigDecimal valorTotal,

    @NotNull
    @Positive
    Integer quantidadeParcelas,

    @NotBlank
    String categoria,

    @NotNull
    UUID metodoPagamentoId,

    @JsonFormat(pattern = FormatoData.DIA_MES_ANO)
    LocalDate faturaDataFechamento,

    String descricao
) {
}
