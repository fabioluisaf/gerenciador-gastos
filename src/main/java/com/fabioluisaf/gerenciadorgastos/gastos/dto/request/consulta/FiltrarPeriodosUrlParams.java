package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta;

import java.util.List;
import java.util.UUID;

public record FiltrarPeriodosUrlParams(
	List<UUID> metodoPagamentoId,
	List<String> categoria,
	String nome
) {
}
