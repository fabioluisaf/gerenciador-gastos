package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem;

import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.InstituicaoFinanceira;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;

public record MetodoPagamentoResumidoResponse(
    InstituicaoFinanceira instituicaoFinanceira,
    TipoMetodoPagamento tipo
) {

	public static MetodoPagamentoResumidoResponse from(MetodoPagamento entidade) {
		return new MetodoPagamentoResumidoResponse(entidade.getInstituicaoFinanceira(), entidade.getTipo());
	}
}
