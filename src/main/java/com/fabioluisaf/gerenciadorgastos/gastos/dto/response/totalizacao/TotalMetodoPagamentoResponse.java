package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao;

import java.math.BigDecimal;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.InstituicaoFinanceira;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Realizado soma gastos realizados; planejado soma todos os gastos, realizados ou nao.")
public record TotalMetodoPagamentoResponse(
		UUID metodoPagamentoId,
		InstituicaoFinanceira instituicaoFinanceira,
		TipoMetodoPagamento tipo,
		String nome,
		BigDecimal realizado,
		BigDecimal planejado
) {
}
