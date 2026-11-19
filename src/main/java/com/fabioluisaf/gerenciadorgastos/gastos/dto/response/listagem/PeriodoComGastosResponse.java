package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem;

public record PeriodoComGastosResponse(
	Integer ano,
	Integer mes,
	long quantidade
) {
}
