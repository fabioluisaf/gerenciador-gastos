package com.fabioluisaf.gerenciadorgastos.gastos.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.gastos.model.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.Gasto;
import com.fabioluisaf.gerenciadorgastos.shared.constantes.FormatoData;
import com.fasterxml.jackson.annotation.JsonFormat;

public record GastoResponse(
    UUID id,

    @JsonFormat(pattern = FormatoData.DIA_MES_ANO) 
    LocalDate data,

    String nome,

    BigDecimal valor,

    String categoria,

    MetodoPagamento metodoPagamento,

    String descricao,

    boolean realizado
) {

	public static GastoResponse from(Gasto entidade) {
		return new GastoResponse(
            entidade.getId(),
            entidade.getData(),
            entidade.getNome(),
            entidade.getValor(),
            entidade.getCategoria(),
            entidade.getMetodoPagamento(),
            entidade.getDescricao(),
            entidade.isRealizado()
        );
	}
}
