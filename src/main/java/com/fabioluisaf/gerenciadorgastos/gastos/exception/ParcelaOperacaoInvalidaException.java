package com.fabioluisaf.gerenciadorgastos.gastos.exception;

public class ParcelaOperacaoInvalidaException extends RuntimeException {

	public ParcelaOperacaoInvalidaException() {
		super("PARCELA_DEVE_SER_ALTERADA_PELA_COMPRA");
	}
}
