package com.fabioluisaf.gerenciadorgastos.gastos.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.InstituicaoFinanceira;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.FaturaRepository;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.GastoRepository;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.MetodoPagamentoRepository;
import com.fabioluisaf.gerenciadorgastos.gastos.service.MetodoPagamentoService;
import com.fabioluisaf.gerenciadorgastos.shared.config.ApiProperties;
import com.fabioluisaf.gerenciadorgastos.shared.exception.GlobalExceptionHandler;
import com.fabioluisaf.gerenciadorgastos.shared.exception.NegocioExceptionHandler;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;

class MetodoPagamentoControllerTest {

	private MetodoPagamentoRepository repository;
	private MockMvc mvc;

	@BeforeEach
	void configurar() {
		repository = mock(MetodoPagamentoRepository.class);
		final var service = new MetodoPagamentoService(
				repository, mock(GastoRepository.class), mock(FaturaRepository.class)
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
	void criaEAtualizaInstituicaoFinanceiraTipada() throws Exception {
		final var id = UUID.randomUUID();
		when(repository.save(any(MetodoPagamento.class))).thenAnswer(invocacao -> {
			final MetodoPagamento metodo = invocacao.getArgument(0);
			metodo.setId(id);

			return metodo;
		});
		final var metodo = MetodoPagamento.criar(TipoMetodoPagamento.PIX, null);
		when(repository.findById(id)).thenReturn(Optional.of(metodo));

		for (final var instituicao : InstituicaoFinanceira.values()) {
			final var conteudo = """
					{"tipo":"PIX","instituicaoFinanceira":"%s","nome":"Conta"}
					""".formatted(instituicao.name());
			mvc.perform(post("/metodos-pagamento").contentType(MediaType.APPLICATION_JSON).content(conteudo))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.dados.instituicaoFinanceira").value(instituicao.name()));
			mvc.perform(put("/metodos-pagamento/{id}", id)
					.contentType(MediaType.APPLICATION_JSON).content("""
							{"instituicaoFinanceira":"%s","nome":"Conta"}
							""".formatted(instituicao.name())))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.dados.instituicaoFinanceira").value(instituicao.name()));
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
			final var metodo = MetodoPagamento.criar(esperado, esperado == TipoMetodoPagamento.CREDITO ? 10 : null);
			when(repository.findById(id)).thenReturn(Optional.of(metodo));
			final var fechamento = esperado == TipoMetodoPagamento.CREDITO ? "10" : "null";
			final var conteudo = """
					{"tipo":"%s","instituicaoFinanceira":"CAIXA","nome":"Conta","diaFechamento":%s}
					""".formatted(tipo, fechamento);
			mvc.perform(post("/metodos-pagamento").contentType(MediaType.APPLICATION_JSON).content(conteudo))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.dados.tipo").value(esperado.name()));
			mvc.perform(put("/metodos-pagamento/{id}", id)
					.contentType(MediaType.APPLICATION_JSON).content("""
							{"instituicaoFinanceira":"CAIXA","nome":"Atualizado","diaFechamento":%s}
							""".formatted(fechamento)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.dados.tipo").value(esperado.name()));
			assertEquals(esperado, metodo.getTipo());
			assertEquals("Atualizado", metodo.getNome());
			assertEquals(esperado == TipoMetodoPagamento.CREDITO ? 10 : null, metodo.getDiaFechamento());
		}
	}

	@Test
	void atualizaFechamentoDeCreditoSemEnviarTipo() throws Exception {
		final var id = UUID.randomUUID();
		final var metodo = MetodoPagamento.criar(TipoMetodoPagamento.CREDITO, 10);
		metodo.setNome("Original");
		when(repository.findById(id)).thenReturn(Optional.of(metodo));
		when(repository.save(metodo)).thenReturn(metodo);

		mvc.perform(put("/metodos-pagamento/{id}", id).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"instituicaoFinanceira":"CAIXA","nome":"Alterado","diaFechamento":20}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados.tipo").value("CREDITO"))
				.andExpect(jsonPath("$.dados.diaFechamento").value(20));

		assertEquals(TipoMetodoPagamento.CREDITO, metodo.getTipo());
		assertEquals(20, metodo.getDiaFechamento());
		assertEquals("Alterado", metodo.getNome());
		verify(repository).save(metodo);
	}

	@Test
	void rejeitaFechamentoIncompativelNaCriacaoEAtualizacao() throws Exception {
		final var id = UUID.randomUUID();
		for (final var tipo : TipoMetodoPagamento.values()) {
			final Integer diaOriginal = tipo == TipoMetodoPagamento.CREDITO ? 10 : null;
			final var metodo = MetodoPagamento.criar(tipo, diaOriginal);
			metodo.setNome("Original");
			when(repository.findById(id)).thenReturn(Optional.of(metodo));
			final var diaInvalido = tipo == TipoMetodoPagamento.CREDITO ? "null" : "10";
			final var codigo = tipo == TipoMetodoPagamento.CREDITO
					? "DIA_FECHAMENTO_OBRIGATORIO_PARA_CREDITO"
					: "DIA_FECHAMENTO_PERMITIDO_APENAS_PARA_CREDITO";
			final var conteudo = """
					{"tipo":"%s","instituicaoFinanceira":"CAIXA","nome":"Alterado","diaFechamento":%s}
					""".formatted(tipo.name(), diaInvalido);
			mvc.perform(post("/metodos-pagamento").contentType(MediaType.APPLICATION_JSON).content(conteudo))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.dados.mensagem").value(codigo));
			mvc.perform(put("/metodos-pagamento/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
					{"instituicaoFinanceira":"CAIXA","nome":"Alterado","diaFechamento":%s}
					""".formatted(diaInvalido)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.dados.mensagem").value(codigo));
			assertEquals(diaOriginal, metodo.getDiaFechamento());
			assertEquals("Original", metodo.getNome());
		}
		verify(repository, never()).save(any());
	}

	@Test
	void rejeitaTipoAusenteOuInvalidoNaCriacao() throws Exception {
		for (final var valor : List.of("null", "\"\"", "\"desconhecido\"")) {
			final var conteudo = """
					{"tipo":%s,"instituicaoFinanceira":"CAIXA","nome":"Conta"}
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
		for (final var valor : List.of("null", "\"\"", "\"DESCONHECIDA\"")) {
			final var conteudo = """
					{"tipo":"PIX","instituicaoFinanceira":%s,"nome":"Conta"}
					""".formatted(valor);
			for (final var request : List.of(post("/metodos-pagamento"),
					put("/metodos-pagamento/{id}", UUID.randomUUID()))) {
				final var resultado = mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(conteudo))
						.andExpect(status().isBadRequest());
				if (valor.equals("null")) {
					resultado.andExpect(jsonPath(
							"$.dados.errosValidacao[?(@.campo == 'instituicaoFinanceira')].codigo",
							hasItem("CAMPO_OBRIGATORIO")));
				} else {
					resultado.andExpect(jsonPath("$.dados.mensagem").value("REQUISICAO_INVALIDA"));
				}
			}
		}
		verifyNoInteractions(repository);
	}
}
