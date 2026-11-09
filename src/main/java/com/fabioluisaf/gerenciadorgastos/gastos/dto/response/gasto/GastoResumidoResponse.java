package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.gasto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.metodopagamento.MetodoPagamentoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.Gasto;
import com.fabioluisaf.gerenciadorgastos.shared.constantes.FormatoData;
import com.fasterxml.jackson.annotation.JsonFormat;

public record GastoResumidoResponse(
    @JsonFormat(pattern = FormatoData.DIA_MES_ANO)
    LocalDate data,

    String nome,

    BigDecimal valor,

    String categoria,

    MetodoPagamentoResponse metodoPagamento
) {

	public static GastoResumidoResponse from(Gasto entidade) {
		return new GastoResumidoResponse(
				entidade.getData(),
				entidade.getNome(),
				entidade.getValor(),
				entidade.getCategoria(),
				MetodoPagamentoResponse.from(entidade.getMetodoPagamento())
		);
	}
}
