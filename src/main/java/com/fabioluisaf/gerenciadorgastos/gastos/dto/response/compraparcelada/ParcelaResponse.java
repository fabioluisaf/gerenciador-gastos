package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.compraparcelada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.gastos.model.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.Gasto;
import com.fabioluisaf.gerenciadorgastos.shared.constantes.FormatoData;
import com.fasterxml.jackson.annotation.JsonFormat;

public record ParcelaResponse(
    UUID id,
    Integer numeroParcela,
    @JsonFormat(pattern = FormatoData.DIA_MES_ANO) LocalDate data,
    String nome,
    BigDecimal valor,
    String categoria,
    MetodoPagamento metodoPagamento,
    String descricao,
    boolean realizado
) {
    public static ParcelaResponse from(Gasto parcela) {
        return new ParcelaResponse(
            parcela.getId(),
            parcela.getNumeroParcela(),
            parcela.getData(),
            parcela.getNome(),
            parcela.getValor(),
            parcela.getCategoria(),
            parcela.getMetodoPagamento(),
            parcela.getDescricao(),
            parcela.isRealizado()
        );
    }
}
