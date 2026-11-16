package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta;

import java.util.UUID;

public record FiltrarGastosUrlParams(
	UUID metodoPagamentoId,
	String categoria
) {
}
