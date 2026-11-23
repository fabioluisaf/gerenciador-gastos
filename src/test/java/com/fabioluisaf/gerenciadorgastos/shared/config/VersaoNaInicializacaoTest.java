package com.fabioluisaf.gerenciadorgastos.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class VersaoNaInicializacaoTest {

	@Test
	void registraAVersaoNoLog(final CapturedOutput saida) {
		new VersaoNaInicializacao(new ApiProperties("2.2.0")).registrarVersao();

		assertThat(saida).contains("Gerenciador de gastos API versão 2.2.0");
	}

	@Test
	void registraAVersaoPadraoQuandoNaoInformada(final CapturedOutput saida) {
		new VersaoNaInicializacao(new ApiProperties(null)).registrarVersao();

		assertThat(saida).contains("Gerenciador de gastos API versão 0.0.0.0--nao_informada");
	}
}
