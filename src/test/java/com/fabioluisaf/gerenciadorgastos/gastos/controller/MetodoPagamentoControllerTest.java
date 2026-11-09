package com.fabioluisaf.gerenciadorgastos.gastos.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
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

import com.fabioluisaf.gerenciadorgastos.gastos.model.InstituicaoFinanceira;
import com.fabioluisaf.gerenciadorgastos.gastos.model.TipoMetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.FaturaRepository;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.GastoRepository;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.MetodoPagamentoRepository;
import com.fabioluisaf.gerenciadorgastos.gastos.service.MetodoPagamentoService;
import com.fabioluisaf.gerenciadorgastos.shared.config.ApiProperties;
import com.fabioluisaf.gerenciadorgastos.shared.exception.GlobalExceptionHandler;
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
				.setControllerAdvice(new GlobalExceptionHandler(presenter))
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
		final var metodo = new MetodoPagamento();
		when(repository.findById(id)).thenReturn(Optional.of(metodo));

		for (final var instituicao : InstituicaoFinanceira.values()) {
			final var conteudo = """
					{"tipo":"PIX","instituicaoFinanceira":"%s","nome":"Conta"}
					""".formatted(instituicao.name());
			mvc.perform(post("/metodos-pagamento").contentType(MediaType.APPLICATION_JSON).content(conteudo))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.dados.instituicaoFinanceira").value(instituicao.name()));
			mvc.perform(put("/metodos-pagamento/{id}", id)
					.contentType(MediaType.APPLICATION_JSON).content(conteudo))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.dados.instituicaoFinanceira").value(instituicao.name()));
			assertEquals(instituicao, metodo.getInstituicaoFinanceira());
		}
	}

	@Test
	void aceitaTipoEmQualquerCaseNaCriacaoEAtualizacao() throws Exception {
		final var id = UUID.randomUUID();
		when(repository.save(any(MetodoPagamento.class))).thenAnswer(invocacao -> {
			final MetodoPagamento metodo = invocacao.getArgument(0);
			metodo.setId(id);

			return metodo;
		});
		final var metodo = new MetodoPagamento();
		when(repository.findById(id)).thenReturn(Optional.of(metodo));

		for (final var tipo : List.of("PIX", "Pix", "pix", "PiX",
				"CREDITO", "Credito", "credito", "cReDiTo",
				"DEBITO", "Debito", "debito", "dEbItO")) {
			final var esperado = TipoMetodoPagamento.valueOf(tipo.toUpperCase(Locale.ROOT));
			final var fechamento = esperado == TipoMetodoPagamento.CREDITO ? "10" : "null";
			final var conteudo = """
					{"tipo":"%s","instituicaoFinanceira":"CAIXA","nome":"Conta","diaFechamento":%s}
					""".formatted(tipo, fechamento);
			mvc.perform(post("/metodos-pagamento").contentType(MediaType.APPLICATION_JSON).content(conteudo))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.dados.tipo").value(esperado.name()));
			mvc.perform(put("/metodos-pagamento/{id}", id)
					.contentType(MediaType.APPLICATION_JSON).content(conteudo))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.dados.tipo").value(esperado.name()));
			assertEquals(esperado, metodo.getTipo());
		}
	}

	@Test
	void rejeitaTipoAusenteOuInvalidoNaCriacaoEAtualizacao() throws Exception {
		for (final var valor : List.of("null", "\"\"", "\"desconhecido\"")) {
			final var conteudo = """
					{"tipo":%s,"instituicaoFinanceira":"CAIXA","nome":"Conta"}
					""".formatted(valor);
			for (final var request : List.of(post("/metodos-pagamento"),
					put("/metodos-pagamento/{id}", UUID.randomUUID()))) {
				final var resultado = mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(conteudo))
						.andExpect(status().isBadRequest());
				if (valor.equals("null")) {
					resultado.andExpect(jsonPath(
							"$.dados.errosValidacao[?(@.campo == 'tipo')].codigo",
							hasItem("CAMPO_OBRIGATORIO")));
				} else {
					resultado.andExpect(jsonPath("$.dados.mensagem").value("REQUISICAO_INVALIDA"));
				}
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
