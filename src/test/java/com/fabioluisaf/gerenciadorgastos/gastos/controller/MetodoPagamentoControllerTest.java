package com.fabioluisaf.gerenciadorgastos.gastos.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.UsoMetodoPagamentoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.InstituicoesTeste;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.FaturaRepository;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.GastoRepository;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.InstituicaoFinanceiraRepository;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.MetodoPagamentoRepository;
import com.fabioluisaf.gerenciadorgastos.gastos.service.InstituicaoFinanceiraService;
import com.fabioluisaf.gerenciadorgastos.gastos.service.MetodoPagamentoService;
import com.fabioluisaf.gerenciadorgastos.shared.config.ApiProperties;
import com.fabioluisaf.gerenciadorgastos.shared.exception.GlobalExceptionHandler;
import com.fabioluisaf.gerenciadorgastos.shared.exception.NegocioExceptionHandler;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;

class MetodoPagamentoControllerTest {

	private MetodoPagamentoRepository repository;
	private GastoRepository gastoRepository;
	private MockMvc mvc;

	@BeforeEach
	void configurar() {
		repository = mock(MetodoPagamentoRepository.class);
		gastoRepository = mock(GastoRepository.class);
		final var instituicaoRepository = mock(InstituicaoFinanceiraRepository.class);
		when(instituicaoRepository.findById(InstituicoesTeste.NUBANK_ID)).thenReturn(Optional.of(InstituicoesTeste.nubank()));
		when(instituicaoRepository.findById(InstituicoesTeste.CAIXA_ID)).thenReturn(Optional.of(InstituicoesTeste.caixa()));
		final var service = new MetodoPagamentoService(
				repository, gastoRepository, mock(FaturaRepository.class),
				new InstituicaoFinanceiraService(instituicaoRepository, repository)
		);
		final var presenter = new ApiResponsePresenter(new ApiProperties("v1"));
		final var validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mvc = MockMvcBuilders.standaloneSetup(new MetodoPagamentoController(service, presenter))
				.setControllerAdvice(new GlobalExceptionHandler(presenter), new NegocioExceptionHandler(presenter))
				.setValidator(validator)
				.build();
	}

	@Test
	void criaEAtualizaInstituicaoFinanceiraPeloId() throws Exception {
		final var id = UUID.randomUUID();
		when(repository.save(any(MetodoPagamento.class))).thenAnswer(invocacao -> {
			final MetodoPagamento metodo = invocacao.getArgument(0);
			metodo.setId(id);

			return metodo;
		});
		final var metodo = MetodoPagamento.criar(TipoMetodoPagamento.PIX, null, null);
		when(repository.findById(id)).thenReturn(Optional.of(metodo));

		for (final var instituicao : List.of(InstituicoesTeste.nubank(), InstituicoesTeste.caixa())) {
			final var conteudo = """
					{"tipo":"PIX","instituicaoFinanceiraId":"%s","nome":"Conta"}
					""".formatted(instituicao.getId());
			mvc.perform(post("/metodos-pagamento").contentType(MediaType.APPLICATION_JSON).content(conteudo))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.dados.instituicaoFinanceira.id").value(instituicao.getId().toString()))
					.andExpect(jsonPath("$.dados.instituicaoFinanceira.nome").value(instituicao.getNome()));
			mvc.perform(put("/metodos-pagamento/{id}", id)
					.contentType(MediaType.APPLICATION_JSON).content("""
							{"instituicaoFinanceiraId":"%s","nome":"Conta"}
							""".formatted(instituicao.getId())))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.dados.instituicaoFinanceira.nome").value(instituicao.getNome()));
			assertEquals(instituicao, metodo.getInstituicaoFinanceira());
		}
	}

	@Test
	void aceitaTipoEmQualquerCaseNaCriacaoEAtualizaSemTipo() throws Exception {
		final var id = UUID.randomUUID();
		when(repository.save(any(MetodoPagamento.class))).thenAnswer(invocacao -> {
			final MetodoPagamento metodo = invocacao.getArgument(0);
			metodo.setId(id);

			return metodo;
		});
		for (final var tipo : List.of("PIX", "Pix", "pix", "PiX",
				"CREDITO", "Credito", "credito", "cReDiTo",
				"DEBITO", "Debito", "debito", "dEbItO",
				"BOLETO", "Boleto", "boleto", "bOlEtO",
				"DOC", "Doc", "doc", "dOc",
				"TED", "Ted", "ted", "tEd")) {
			final var esperado = TipoMetodoPagamento.valueOf(tipo.toUpperCase(Locale.ROOT));
			final var metodo = MetodoPagamento.criar(esperado, esperado == TipoMetodoPagamento.CREDITO ? 10 : null,
					esperado == TipoMetodoPagamento.CREDITO ? 17 : null);
			when(repository.findById(id)).thenReturn(Optional.of(metodo));
			final var fechamento = esperado == TipoMetodoPagamento.CREDITO ? "10" : "null";
			final var vencimento = esperado == TipoMetodoPagamento.CREDITO ? "17" : "null";
			final var conteudo = """
					{"tipo":"%s","instituicaoFinanceiraId":"00000000-0000-0000-0000-00000000000b","nome":"Conta","diaFechamento":%s,"diaVencimento":%s}
					""".formatted(tipo, fechamento, vencimento);
			mvc.perform(post("/metodos-pagamento").contentType(MediaType.APPLICATION_JSON).content(conteudo))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.dados.tipo").value(esperado.name()))
					.andExpect(jsonPath("$.dados.diaVencimento").value(
							esperado == TipoMetodoPagamento.CREDITO ? 17 : null));
			mvc.perform(put("/metodos-pagamento/{id}", id)
					.contentType(MediaType.APPLICATION_JSON).content("""
							{"instituicaoFinanceiraId":"00000000-0000-0000-0000-00000000000b","nome":"Atualizado","diaFechamento":%s,"diaVencimento":%s}
							""".formatted(fechamento, vencimento)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.dados.tipo").value(esperado.name()));
			assertEquals(esperado, metodo.getTipo());
			assertEquals("Atualizado", metodo.getNome());
			assertEquals(esperado == TipoMetodoPagamento.CREDITO ? 10 : null, metodo.getDiaFechamento());
			assertEquals(esperado == TipoMetodoPagamento.CREDITO ? 17 : null, metodo.getDiaVencimento());
		}
	}

	@Test
	void atualizaFechamentoDeCreditoSemEnviarTipo() throws Exception {
		final var id = UUID.randomUUID();
		final var metodo = MetodoPagamento.criar(TipoMetodoPagamento.CREDITO, 10, 17);
		metodo.setNome("Original");
		when(repository.findById(id)).thenReturn(Optional.of(metodo));
		when(repository.save(metodo)).thenReturn(metodo);

		mvc.perform(put("/metodos-pagamento/{id}", id).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"instituicaoFinanceiraId":"00000000-0000-0000-0000-00000000000b","nome":"Alterado","diaFechamento":20,"diaVencimento":27}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados.tipo").value("CREDITO"))
				.andExpect(jsonPath("$.dados.diaFechamento").value(20))
				.andExpect(jsonPath("$.dados.diaVencimento").value(27));

		assertEquals(TipoMetodoPagamento.CREDITO, metodo.getTipo());
		assertEquals(20, metodo.getDiaFechamento());
		assertEquals(27, metodo.getDiaVencimento());
		assertEquals("Alterado", metodo.getNome());
		verify(repository).save(metodo);
	}

	@Test
	void rejeitaFechamentoIncompativelNaCriacaoEAtualizacao() throws Exception {
		final var id = UUID.randomUUID();
		for (final var tipo : TipoMetodoPagamento.values()) {
			final Integer diaOriginal = tipo == TipoMetodoPagamento.CREDITO ? 10 : null;
			final var metodo = MetodoPagamento.criar(tipo, diaOriginal, tipo == TipoMetodoPagamento.CREDITO ? 17 : null);
			metodo.setNome("Original");
			when(repository.findById(id)).thenReturn(Optional.of(metodo));
			final var diaInvalido = tipo == TipoMetodoPagamento.CREDITO ? "null" : "10";
			final var codigo = tipo == TipoMetodoPagamento.CREDITO
					? "DIA_FECHAMENTO_OBRIGATORIO_PARA_CREDITO"
					: "DIA_FECHAMENTO_PERMITIDO_APENAS_PARA_CREDITO";
			final var conteudo = """
					{"tipo":"%s","instituicaoFinanceiraId":"00000000-0000-0000-0000-00000000000b","nome":"Alterado","diaFechamento":%s}
					""".formatted(tipo.name(), diaInvalido);
			mvc.perform(post("/metodos-pagamento").contentType(MediaType.APPLICATION_JSON).content(conteudo))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.dados.mensagem").value(codigo));
			mvc.perform(put("/metodos-pagamento/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
					{"instituicaoFinanceiraId":"00000000-0000-0000-0000-00000000000b","nome":"Alterado","diaFechamento":%s}
					""".formatted(diaInvalido)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.dados.mensagem").value(codigo));
			assertEquals(diaOriginal, metodo.getDiaFechamento());
			assertEquals("Original", metodo.getNome());
		}
		verify(repository, never()).save(any());
	}

	@Test
	void exigeVencimentoValidoParaCreditoNaCriacaoEAtualizacao() throws Exception {
		final var id = UUID.randomUUID();
		for (final var dia : List.of("null", "0", "32")) {
			final var metodo = MetodoPagamento.criar(TipoMetodoPagamento.CREDITO, 10, 17);
			when(repository.findById(id)).thenReturn(Optional.of(metodo));
			for (final var request : List.of(post("/metodos-pagamento"), put("/metodos-pagamento/{id}", id))) {
				final var resultado = mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content("""
						{"tipo":"CREDITO","instituicaoFinanceiraId":"00000000-0000-0000-0000-00000000000b","nome":"Cartao","diaFechamento":10,"diaVencimento":%s}
						""".formatted(dia)))
						.andExpect(status().isBadRequest());
				if (dia.equals("null")) {
					resultado.andExpect(jsonPath("$.dados.mensagem").value("DIA_VENCIMENTO_OBRIGATORIO_PARA_CREDITO"));
				} else {
					resultado.andExpect(jsonPath(
							"$.dados.errosValidacao[?(@.campo == 'diaVencimento')].codigo",
							hasItem(dia.equals("0") ? "ABAIXO_DO_MINIMO" : "ACIMA_DO_MAXIMO")));
				}
			}
			assertEquals(17, metodo.getDiaVencimento());
		}
		mvc.perform(post("/metodos-pagamento").contentType(MediaType.APPLICATION_JSON).content("""
				{"tipo":"CREDITO","instituicaoFinanceiraId":"00000000-0000-0000-0000-00000000000b","nome":"Cartao","diaFechamento":10}
				"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.dados.mensagem").value("DIA_VENCIMENTO_OBRIGATORIO_PARA_CREDITO"));
		verify(repository, never()).save(any());
	}

	@Test
	void rejeitaVencimentoEmMetodosNaoCredito() throws Exception {
		final var id = UUID.randomUUID();
		for (final var tipo : TipoMetodoPagamento.values()) {
			if (tipo == TipoMetodoPagamento.CREDITO) {
				continue;
			}
			when(repository.findById(id)).thenReturn(Optional.of(MetodoPagamento.criar(tipo, null, null)));
			for (final var request : List.of(post("/metodos-pagamento"), put("/metodos-pagamento/{id}", id))) {
				mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content("""
						{"tipo":"%s","instituicaoFinanceiraId":"00000000-0000-0000-0000-00000000000b","nome":"Conta","diaVencimento":17}
						""".formatted(tipo.name())))
						.andExpect(status().isBadRequest())
						.andExpect(jsonPath("$.dados.mensagem").value("DIA_VENCIMENTO_PERMITIDO_APENAS_PARA_CREDITO"));
			}
		}
		verify(repository, never()).save(any());
	}

	@Test
	void rejeitaTipoAusenteOuInvalidoNaCriacao() throws Exception {
		for (final var valor : List.of("null", "\"\"", "\"desconhecido\"")) {
			final var conteudo = """
					{"tipo":%s,"instituicaoFinanceiraId":"00000000-0000-0000-0000-00000000000b","nome":"Conta"}
					""".formatted(valor);
			final var resultado = mvc.perform(post("/metodos-pagamento")
					.contentType(MediaType.APPLICATION_JSON).content(conteudo))
					.andExpect(status().isBadRequest());
			if (valor.equals("null")) {
				resultado.andExpect(jsonPath(
						"$.dados.errosValidacao[?(@.campo == 'tipo')].codigo",
						hasItem("CAMPO_OBRIGATORIO")));
			} else {
				resultado.andExpect(jsonPath("$.dados.mensagem").value("REQUISICAO_INVALIDA"));
			}
		}
		verifyNoInteractions(repository);
	}

	@Test
	void rejeitaInstituicaoAusenteOuInvalidaNaCriacaoEAtualizacao() throws Exception {
		for (final var valor : List.of("null", "\"DESCONHECIDA\"")) {
			final var conteudo = """
					{"tipo":"PIX","instituicaoFinanceiraId":%s,"nome":"Conta"}
					""".formatted(valor);
			for (final var request : List.of(post("/metodos-pagamento"),
					put("/metodos-pagamento/{id}", UUID.randomUUID()))) {
				final var resultado = mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(conteudo))
						.andExpect(status().isBadRequest());
				if (valor.equals("null")) {
					resultado.andExpect(jsonPath(
							"$.dados.errosValidacao[?(@.campo == 'instituicaoFinanceiraId')].codigo",
							hasItem("CAMPO_OBRIGATORIO")));
				} else {
					resultado.andExpect(jsonPath("$.dados.mensagem").value("REQUISICAO_INVALIDA"));
				}
			}
		}
		verifyNoInteractions(repository);
	}

	@Test
	void respondeNaoEncontradoParaInstituicaoInexistente() throws Exception {
		final var conteudo = """
				{"tipo":"PIX","instituicaoFinanceiraId":"%s","nome":"Conta"}
				""".formatted(UUID.randomUUID());
		mvc.perform(post("/metodos-pagamento").contentType(MediaType.APPLICATION_JSON).content(conteudo))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.dados.mensagem").value("RECURSO_NAO_ENCONTRADO"));
		verify(repository, never()).save(any());
	}

	@Test
	void listaAQuantidadeDeGastosDeCadaMetodoDePagamento() throws Exception {
		final var credito = UUID.randomUUID();
		final var pix = UUID.randomUUID();
		when(gastoRepository.contarGastosPorMetodoPagamento()).thenReturn(List.of(
				new UsoMetodoPagamentoResponse(credito, 12L), new UsoMetodoPagamentoResponse(pix, 3L)));

		mvc.perform(get("/metodos-pagamento/uso"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados[0].metodoPagamentoId").value(credito.toString()))
				.andExpect(jsonPath("$.dados[0].quantidadeGastos").value(12))
				.andExpect(jsonPath("$.dados[1].metodoPagamentoId").value(pix.toString()))
				.andExpect(jsonPath("$.dados[1].quantidadeGastos").value(3));
	}
}
