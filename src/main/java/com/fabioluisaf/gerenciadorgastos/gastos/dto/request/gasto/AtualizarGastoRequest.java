package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.gasto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fabioluisaf.gerenciadorgastos.shared.constantes.FormatoData;
import com.fabioluisaf.gerenciadorgastos.shared.validation.DataObrigatoriaSeRealizado;
import com.fabioluisaf.gerenciadorgastos.shared.validation.GastoComDataERealizacao;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

@DataObrigatoriaSeRealizado
public record AtualizarGastoRequest(
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
    UUID metodoPagamentoId,

    @JsonFormat(pattern = FormatoData.DIA_MES_ANO)
    LocalDate faturaDataFechamento,

    String descricao,

    @NotNull
    Boolean realizado
) implements GastoComDataERealizacao {
}
