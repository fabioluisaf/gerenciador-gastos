package com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TipoMetodoPagamento {
	CREDITO,
	DEBITO,
	PIX,
	BOLETO,
	DOC,
	TED;

	@JsonCreator
	public static TipoMetodoPagamento from(String valor) {
		return valueOf(valor.toUpperCase(Locale.ROOT));
	}
}
