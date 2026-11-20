package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao;

import java.util.UUID;

public record UsoMetodoPagamentoResponse(
    UUID metodoPagamentoId,
    Long quantidadeGastos
) {
}
