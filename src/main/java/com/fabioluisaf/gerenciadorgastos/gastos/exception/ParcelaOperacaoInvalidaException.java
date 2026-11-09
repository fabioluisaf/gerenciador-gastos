package com.fabioluisaf.gerenciadorgastos.gastos.exception;

public class ParcelaOperacaoInvalidaException extends RuntimeException {

	public ParcelaOperacaoInvalidaException() {
		super("Parcelas devem ser alteradas ou excluídas pela compra parcelada.");
	}
}
