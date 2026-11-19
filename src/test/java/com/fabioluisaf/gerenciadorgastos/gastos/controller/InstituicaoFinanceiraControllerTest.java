package com.fabioluisaf.gerenciadorgastos.gastos.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.InstituicaoFinanceira;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.InstituicaoFinanceiraRepository;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.MetodoPagamentoRepository;
import com.fabioluisaf.gerenciadorgastos.gastos.service.InstituicaoFinanceiraService;
import com.fabioluisaf.gerenciadorgastos.shared.config.ApiProperties;
import com.fabioluisaf.gerenciadorgastos.shared.exception.GlobalExceptionHandler;
import com.fabioluisaf.gerenciadorgastos.shared.exception.NegocioExceptionHandler;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;

class InstituicaoFinanceiraControllerTest {

	private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 1, 2, 3 };
	private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1 };

	private InstituicaoFinanceiraRepository repository;
	private MetodoPagamentoRepository metodoPagamentoRepository;
	private MockMvc mvc;

	@BeforeEach
	void configurar() {
		repository = mock(InstituicaoFinanceiraRepository.class);
		metodoPagamentoRepository = mock(MetodoPagamentoRepository.class);
		when(repository.save(any(InstituicaoFinanceira.class))).thenAnswer(invocacao -> {
			final InstituicaoFinanceira instituicao = invocacao.getArgument(0);
			if (instituicao.getId() == null) {
				instituicao.setId(UUID.randomUUID());
			}
			return instituicao;
		});
		final var service = new InstituicaoFinanceiraService(repository, metodoPagamentoRepository);
		final var presenter = new ApiResponsePresenter(new ApiProperties("v1"));
		final var validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mvc = MockMvcBuilders.standaloneSetup(new InstituicaoFinanceiraController(service, presenter))
				.setControllerAdvice(new GlobalExceptionHandler(presenter), new NegocioExceptionHandler(presenter))
				.setValidator(validator)
				.build();
	}

	@Test
	void criaComIconeEDetectaOTipoPeloConteudo() throws Exception {
		final var icone = new MockMultipartFile("icone", "banco.png", "text/plain", PNG);

		mvc.perform(multipart("/instituicoes-financeiras").file(icone).param("nome", "  Banco Inter  "))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.dados.nome").value("Banco Inter"))
				.andExpect(jsonPath("$.dados.possuiIcone").value(true))
				.andExpect(jsonPath("$.dados.icone").doesNotExist());

		verify(repository).save(argThat(instituicao ->
				"image/png".equals(instituicao.getIconeTipo()) && instituicao.getIcone().length == PNG.length));
	}

	@Test
	void criaSemIcone() throws Exception {
		mvc.perform(multipart("/instituicoes-financeiras").param("nome", "Itaú"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.dados.possuiIcone").value(false));
	}

	@Test
	void rejeitaNomeVazioDuplicadoEIconeInvalido() throws Exception {
		mvc.perform(multipart("/instituicoes-financeiras").param("nome", " "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.dados.errosValidacao[0].campo").value("nome"))
				.andExpect(jsonPath("$.dados.errosValidacao[0].codigo").value("CAMPO_NAO_PODE_FICAR_VAZIO"));

		when(repository.existsByNomeIgnoreCase("Nubank")).thenReturn(true);
		mvc.perform(multipart("/instituicoes-financeiras").param("nome", "Nubank"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.dados.mensagem").value("INSTITUICAO_FINANCEIRA_JA_EXISTE"));

		final var svg = new MockMultipartFile("icone", "x.svg", "image/svg+xml",
				"<svg onload=\"alert(1)\"/>".getBytes());
		mvc.perform(multipart("/instituicoes-financeiras").file(svg).param("nome", "Banco"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.dados.mensagem").value("FORMATO_ICONE_INVALIDO"));

		final var grande = new byte[InstituicaoFinanceira.TAMANHO_MAXIMO_ICONE + 1];
		System.arraycopy(PNG, 0, grande, 0, PNG.length);
		mvc.perform(multipart("/instituicoes-financeiras")
				.file(new MockMultipartFile("icone", "g.png", "image/png", grande)).param("nome", "Banco"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.dados.mensagem").value("ICONE_MUITO_GRANDE"));

		verify(repository, never()).save(any());
	}

	@Test
	void atualizaNomeEMantemIconeQuandoNaoEnviado() throws Exception {
		final var instituicao = existente("Caixa", PNG, "image/png");

		mvc.perform(multipart(HttpMethod.PUT, "/instituicoes-financeiras/{id}", instituicao.getId())
				.param("nome", "Caixa Econômica"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados.nome").value("Caixa Econômica"))
				.andExpect(jsonPath("$.dados.possuiIcone").value(true));
		assertArrayEquals(PNG, instituicao.getIcone());

		mvc.perform(multipart(HttpMethod.PUT, "/instituicoes-financeiras/{id}", instituicao.getId())
				.file(new MockMultipartFile("icone", "novo.jpg", "image/jpeg", JPEG))
				.param("nome", "Caixa Econômica"))
				.andExpect(status().isOk());
		assertArrayEquals(JPEG, instituicao.getIcone());
		assertEquals("image/jpeg", instituicao.getIconeTipo());
	}

	@Test
	void rejeitaRenomearParaNomeDeOutraInstituicao() throws Exception {
		final var instituicao = existente("Caixa", null, null);
		when(repository.existsByNomeIgnoreCaseAndIdNot("nubank", instituicao.getId())).thenReturn(true);

		mvc.perform(multipart(HttpMethod.PUT, "/instituicoes-financeiras/{id}", instituicao.getId())
				.param("nome", "nubank"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.dados.mensagem").value("INSTITUICAO_FINANCEIRA_JA_EXISTE"));
		assertEquals("Caixa", instituicao.getNome());
	}

	@Test
	void devolveIconeComCacheValidadoPorEtag() throws Exception {
		final var instituicao = existente("Nubank", PNG, "image/png");

		final var resposta = mvc.perform(get("/instituicoes-financeiras/{id}/icone", instituicao.getId()))
				.andExpect(status().isOk())
				.andExpect(content().contentType("image/png"))
				.andExpect(content().bytes(PNG))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("Cache-Control", "no-cache"))
				.andReturn().getResponse();

		mvc.perform(get("/instituicoes-financeiras/{id}/icone", instituicao.getId())
				.header("If-None-Match", resposta.getHeader("ETag")))
				.andExpect(status().isNotModified());
	}

	@Test
	void respondeNaoEncontradoSemIconeOuSemInstituicao() throws Exception {
		final var semIcone = existente("Itaú", null, null);

		mvc.perform(get("/instituicoes-financeiras/{id}/icone", semIcone.getId()))
				.andExpect(status().isNotFound());
		mvc.perform(get("/instituicoes-financeiras/{id}", UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.dados.mensagem").value("RECURSO_NAO_ENCONTRADO"));
	}

	@Test
	void naoExcluiInstituicaoUsadaPorMetodoDePagamento() throws Exception {
		final var instituicao = existente("Nubank", null, null);
		when(metodoPagamentoRepository.existsByInstituicaoFinanceiraId(instituicao.getId())).thenReturn(true);

		mvc.perform(delete("/instituicoes-financeiras/{id}", instituicao.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.dados.mensagem").value("INSTITUICAO_FINANCEIRA_EM_USO"));
		verify(repository, never()).delete(any());

		when(metodoPagamentoRepository.existsByInstituicaoFinanceiraId(instituicao.getId())).thenReturn(false);
		mvc.perform(delete("/instituicoes-financeiras/{id}", instituicao.getId()))
				.andExpect(status().isNoContent());
		verify(repository).delete(instituicao);
	}

	private InstituicaoFinanceira existente(String nome, byte[] icone, String tipo) {
		final var instituicao = new InstituicaoFinanceira(nome);
		instituicao.setId(UUID.randomUUID());
		instituicao.atualizarIcone(icone, tipo);
		when(repository.findById(instituicao.getId())).thenReturn(Optional.of(instituicao));
		return instituicao;
	}
}
