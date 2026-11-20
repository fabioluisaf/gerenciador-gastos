package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.comum.InstituicaoFinanceiraResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;

public record MetodoPagamentoResumidoResponse(
    InstituicaoFinanceiraResponse instituicaoFinanceira,
    TipoMetodoPagamento tipo
) {

	public static MetodoPagamentoResumidoResponse from(MetodoPagamento entidade) {
		return new MetodoPagamentoResumidoResponse(
				InstituicaoFinanceiraResponse.from(entidade.getInstituicaoFinanceira()), entidade.getTipo()
		);
	}
}
