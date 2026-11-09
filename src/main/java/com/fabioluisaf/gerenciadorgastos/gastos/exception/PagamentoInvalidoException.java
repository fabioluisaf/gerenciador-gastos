package com.fabioluisaf.gerenciadorgastos.gastos.exception;

public class PagamentoInvalidoException extends RuntimeException {

	public PagamentoInvalidoException(String codigo) {
		super(codigo);
	}
}
