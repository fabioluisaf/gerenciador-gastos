package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.metodopagamento;

import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.gastos.model.InstituicaoFinanceira;
import com.fabioluisaf.gerenciadorgastos.gastos.model.TipoMetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.MetodoPagamento;

public record MetodoPagamentoResponse(
		UUID id,
		TipoMetodoPagamento tipo,
		InstituicaoFinanceira instituicaoFinanceira,
		String nome,
		Integer diaFechamento
) {

	public static MetodoPagamentoResponse from(MetodoPagamento entidade) {
		return new MetodoPagamentoResponse(
				entidade.getId(),
				entidade.getTipo(),
				entidade.getInstituicaoFinanceira(),
				entidade.getNome(),
				entidade.getDiaFechamento()
		);
	}
}
