package com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento;

import java.util.UUID;

public final class InstituicoesTeste {

	public static final UUID NUBANK_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
	public static final UUID CAIXA_ID = UUID.fromString("00000000-0000-0000-0000-00000000000b");

	private InstituicoesTeste() {
	}

	public static InstituicaoFinanceira nubank() {
		return comId(NUBANK_ID, "Nubank");
	}

	public static InstituicaoFinanceira caixa() {
		return comId(CAIXA_ID, "Caixa");
	}

	private static InstituicaoFinanceira comId(UUID id, String nome) {
		final var instituicao = new InstituicaoFinanceira(nome);
		instituicao.setId(id);
		return instituicao;
	}
}
