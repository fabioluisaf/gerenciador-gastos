package com.fabioluisaf.gerenciadorgastos.gastos.exception;

import java.util.UUID;

import lombok.Getter;

@Getter
public class FaturaExistenteException extends RuntimeException {

	private final UUID faturaId;

	public FaturaExistenteException(UUID faturaId) {
		this.faturaId = faturaId;
	}
}
