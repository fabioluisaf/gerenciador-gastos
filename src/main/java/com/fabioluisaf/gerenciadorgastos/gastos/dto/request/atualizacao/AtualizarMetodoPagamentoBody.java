package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.atualizacao;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizarMetodoPagamentoBody(
	
	@NotNull 
	UUID instituicaoFinanceiraId,

	@NotBlank 
	@Size(max = 255)
	String nome,

	@Min(1) 
	@Max(31) 
	Integer diaFechamento,

	@Min(1) 
	@Max(31) 
	Integer diaVencimento
) {
}
