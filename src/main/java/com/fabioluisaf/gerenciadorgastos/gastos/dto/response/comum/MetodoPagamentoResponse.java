package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.comum;

import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.InstituicaoFinanceira;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;

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
