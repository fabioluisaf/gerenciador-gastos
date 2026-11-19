package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.comum;

import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.InstituicaoFinanceira;

public record InstituicaoFinanceiraResponse(
    UUID id,
    String nome,
    boolean possuiIcone,
    String cor
) {

	public static InstituicaoFinanceiraResponse from(InstituicaoFinanceira entidade) {
		return new InstituicaoFinanceiraResponse(
				entidade.getId(), entidade.getNome(), entidade.possuiIcone(), entidade.getCor());
	}
}
