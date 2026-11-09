package com.fabioluisaf.gerenciadorgastos.gastos.model;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TipoMetodoPagamento {
	CREDITO,
	DEBITO,
	PIX;

	@JsonCreator
	public static TipoMetodoPagamento from(String valor) {
		return valueOf(valor.toUpperCase(Locale.ROOT));
	}
}
