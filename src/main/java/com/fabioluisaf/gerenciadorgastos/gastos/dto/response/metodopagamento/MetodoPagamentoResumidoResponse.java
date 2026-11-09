package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.metodopagamento;

import com.fabioluisaf.gerenciadorgastos.gastos.model.InstituicaoFinanceira;
import com.fabioluisaf.gerenciadorgastos.gastos.model.TipoMetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.MetodoPagamento;

public record MetodoPagamentoResumidoResponse(
		InstituicaoFinanceira instituicaoFinanceira,
		TipoMetodoPagamento tipo
) {

	public static MetodoPagamentoResumidoResponse from(MetodoPagamento entidade) {
		return new MetodoPagamentoResumidoResponse(entidade.getInstituicaoFinanceira(), entidade.getTipo());
	}
}
