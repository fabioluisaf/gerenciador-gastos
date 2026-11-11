package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta;

import java.util.UUID;

public record FiltrarGastosRequest(
		UUID metodoPagamentoId,
		String categoria
) {
}
