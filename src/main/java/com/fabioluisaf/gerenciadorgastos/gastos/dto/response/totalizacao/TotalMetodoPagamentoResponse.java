package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao;

import java.math.BigDecimal;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.comum.InstituicaoFinanceiraResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;

public record TotalMetodoPagamentoResponse(
    UUID metodoPagamentoId,
    InstituicaoFinanceiraResponse instituicaoFinanceira,
    TipoMetodoPagamento tipo,
    String nome,
    BigDecimal realizado,
    BigDecimal planejado
) {

	public TotalMetodoPagamentoResponse(
			UUID metodoPagamentoId, UUID instituicaoId, String instituicaoNome, boolean instituicaoPossuiIcone,
			String instituicaoCor, TipoMetodoPagamento tipo, String nome, BigDecimal realizado, BigDecimal planejado) {

		this(metodoPagamentoId,
				new InstituicaoFinanceiraResponse(instituicaoId, instituicaoNome, instituicaoPossuiIcone, instituicaoCor),
				tipo, nome, realizado, planejado);
	}
}
