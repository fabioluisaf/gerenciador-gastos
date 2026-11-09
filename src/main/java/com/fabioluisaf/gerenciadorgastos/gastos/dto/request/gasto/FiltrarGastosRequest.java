package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.gasto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record FiltrarGastosRequest(
		UUID metodoPagamentoId,
		@Schema(description = "Categoria exata, incluindo maiusculas e minusculas. Pode ser combinada com o metodo.")
		String categoria
) {
}
