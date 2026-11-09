package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.compraparcelada;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fabioluisaf.gerenciadorgastos.gastos.model.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.shared.constantes.FormatoData;
import com.fasterxml.jackson.annotation.JsonFormat;

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
    MetodoPagamento metodoPagamento,

    String descricao,

    @NotNull 
    @PositiveOrZero 
    @Digits(integer = 17, fraction = 2) 
    BigDecimal valorTotal,

    @NotNull 
    @Positive 
    Integer quantidadeParcelas,

    @NotNull
    @JsonFormat(
        pattern = "[" + FormatoData.DIA_MES_ANO + "]["
                      + FormatoData.DIA_MES_ANO_HIFEN + "]["
                      + FormatoData.ANO_MES_DIA_HIFEN + "]"
    )
    LocalDate data
) {
}
