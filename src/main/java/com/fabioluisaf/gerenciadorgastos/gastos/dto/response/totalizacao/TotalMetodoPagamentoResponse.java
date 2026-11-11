package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao;

import java.math.BigDecimal;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.InstituicaoFinanceira;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;

public record TotalMetodoPagamentoResponse(
    UUID metodoPagamentoId,
    InstituicaoFinanceira instituicaoFinanceira,
    TipoMetodoPagamento tipo,
    String nome,
    BigDecimal realizado,
    BigDecimal planejado
) {
}
