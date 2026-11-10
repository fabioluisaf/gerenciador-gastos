package com.fabioluisaf.gerenciadorgastos.shared.exception;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.format.support.DefaultFormattingConversionService;

import com.fabioluisaf.gerenciadorgastos.gastos.controller.GastoController;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.ParcelaOperacaoInvalidaException;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.GastoRealizadoSemDataException;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.MetodoPagamentoEmUsoException;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.PagamentoInvalidoException;
import com.fabioluisaf.gerenciadorgastos.gastos.service.GastoService;
import com.fabioluisaf.gerenciadorgastos.gastos.service.GastoConsultaService;
import com.fabioluisaf.gerenciadorgastos.shared.config.ApiProperties;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;
import com.fabioluisaf.gerenciadorgastos.shared.serialization.PeriodoConverter;

import jakarta.persistence.EntityNotFoundException;

class GlobalExceptionHandlerTest {

	private GastoService service;
	private MockMvc mvc;
	private UUID id;

	@BeforeEach
	void configurar() {
		service = mock(GastoService.class);
		id = UUID.randomUUID();
		final var presenter = new ApiResponsePresenter(new ApiProperties("v1"));
		final var validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		final var conversionService = new DefaultFormattingConversionService();
		conversionService.addConverter(new PeriodoConverter());
		mvc = MockMvcBuilders.standaloneSetup(new GastoController(service, mock(GastoConsultaService.class), presenter))
				.setControllerAdvice(new GlobalExceptionHandler(presenter), new NegocioExceptionHandler(presenter))
				.setValidator(validator)
				.setConversionService(conversionService)
				.setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
				.build();
	}

	@Test
	void priorizaErrosDeNegocioSobreTratamentoGenerico() throws Exception {
		when(service.buscarPorId(id)).thenThrow(new PagamentoInvalidoException("FATURA_PERMITIDA_APENAS_PARA_CREDITO"));

		mvc.perform(get("/gastos/{id}", id))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.dados.mensagem").value("FATURA_PERMITIDA_APENAS_PARA_CREDITO"))
				.andExpect(jsonPath("$.dados.caminho").value("/gastos/" + id))
				.andExpect(jsonPath("$.dados.instante").isNotEmpty())
				.andExpect(jsonPath("$.versao").value("v1"));

		doThrow(new MetodoPagamentoEmUsoException()).when(service).buscarPorId(id);

		mvc.perform(get("/gastos/{id}", id))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.dados.mensagem").value("METODO_PAGAMENTO_EM_USO"));
	}

	@Test
	void reportaErrosEstruturadosParaCamposInvalidos() throws Exception {
		mvc.perform(post("/gastos").contentType(MediaType.APPLICATION_JSON).content("""
				{"nome":"  ","valor":-1,"categoria":"","realizado":null}
				"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.dados.status").value(400))
				.andExpect(jsonPath("$.dados.caminho").value("/gastos"))
				.andExpect(jsonPath("$.dados.instante").isNotEmpty())
				.andExpect(jsonPath("$.versao").value("v1"))
				.andExpect(jsonPath("$.paginacao").doesNotHaveJsonPath())
				.andExpect(jsonPath("$.dados.mensagem").value("ERRO_DE_VALIDACAO"))
				.andExpect(jsonPath("$.dados.errosValidacao[?(@.campo == 'nome')].codigo",
						hasItem("CAMPO_NAO_PODE_FICAR_VAZIO")))
				.andExpect(jsonPath("$.dados.errosValidacao[?(@.campo == 'valor')].codigo",
						hasItem("DEVE_SER_ZERO_OU_POSITIVO")))
				.andExpect(jsonPath("$.dados.errosValidacao[?(@.campo == 'realizado')].codigo",
						hasItem("CAMPO_OBRIGATORIO")));
		mvc.perform(get("/gastos").param("periodo", "1999-02"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.dados.errosValidacao[?(@.campo == 'periodo')].codigo",
						hasItem("ABAIXO_DO_MINIMO")));
		mvc.perform(get("/gastos"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.dados.errosValidacao[?(@.campo == 'periodo')].codigo",
						hasItem("CAMPO_OBRIGATORIO")));
		mvc.perform(put("/gastos/{id}", id).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.dados.errosValidacao[?(@.campo == 'nome')].codigo",
						hasItem("CAMPO_NAO_PODE_FICAR_VAZIO")));
		mvc.perform(post("/gastos").contentType(MediaType.APPLICATION_JSON).content("""
				{"data":"data-invalida","nome":"Mercado","valor":25.50,"categoria":"Alimentacao",
				"metodoPagamentoId":"%s","realizado":true}
				""".formatted(UUID.randomUUID())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.dados.mensagem").value("ERRO_DE_VALIDACAO"))
				.andExpect(jsonPath("$.dados.errosValidacao[?(@.campo == 'data')].codigo",
						hasItem("FORMATO_INVALIDO")));
		mvc.perform(post("/gastos").contentType(MediaType.APPLICATION_JSON).content("""
				{"data":null,"nome":"Conta de luz","valor":80,"categoria":"Casa",
				"metodoPagamentoId":"%s","realizado":true}
				""".formatted(UUID.randomUUID())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.dados.errosValidacao[?(@.campo == 'data')].codigo",
						hasItem("DATA_OBRIGATORIA_PARA_GASTO_REALIZADO")));
	}

	@Test
	void trataErrosHttpERecursosAusentes() throws Exception {
		when(service.buscarPorId(id)).thenThrow(new EntityNotFoundException());
		doThrow(new EntityNotFoundException()).when(service).excluir(id);
		when(service.atualizar(eq(id), any())).thenThrow(new EntityNotFoundException());
		mvc.perform(get("/gastos/{id}", id))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.dados.status").value(404))
				.andExpect(jsonPath("$.dados.mensagem").value("RECURSO_NAO_ENCONTRADO"))
				.andExpect(jsonPath("$.dados.errosValidacao").doesNotExist());
		mvc.perform(delete("/gastos/{id}", id)).andExpect(status().isNotFound());
		mvc.perform(put("/gastos/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"data":"15/02/2026","nome":"Mercado","valor":25.50,"categoria":"Alimentacao",
				"metodoPagamentoId":"%s","realizado":true}
				""".formatted(UUID.randomUUID()))).andExpect(status().isNotFound());
		mvc.perform(get("/gastos/uuid-invalido")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.dados.mensagem").value("REQUISICAO_INVALIDA"));
		mvc.perform(post("/gastos").contentType(MediaType.APPLICATION_JSON).content("{"))
				.andExpect(status().isBadRequest());
		mvc.perform(patch("/gastos/{id}", id)).andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.dados.mensagem").value("METODO_HTTP_NAO_PERMITIDO"))
				.andExpect(header().exists("Allow"));
		mvc.perform(post("/gastos").contentType(MediaType.TEXT_PLAIN).content("texto"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.dados.mensagem").value("TIPO_DE_CONTEUDO_NAO_SUPORTADO"));
	}

	@Test
	void retornaConflitoParaOperacoesDiretasEmParcelas() throws Exception {
		doThrow(new ParcelaOperacaoInvalidaException()).when(service).excluir(id);
		when(service.atualizar(eq(id), any())).thenThrow(new ParcelaOperacaoInvalidaException());
		mvc.perform(delete("/gastos/{id}", id)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.dados.status").value(409))
				.andExpect(jsonPath("$.dados.mensagem")
						.value("PARCELA_DEVE_SER_ALTERADA_PELA_COMPRA"));
		mvc.perform(put("/gastos/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"data":"15/02/2026","nome":"Mercado","valor":25.50,"categoria":"Alimentacao",
				"metodoPagamentoId":"%s","realizado":true}
				""".formatted(UUID.randomUUID()))).andExpect(status().isConflict());
	}

	@Test
	void retornaErroAoMarcarComoRealizadoGastoSemData() throws Exception {
		when(service.atualizarRealizado(id, true)).thenThrow(new GastoRealizadoSemDataException());

		mvc.perform(patch("/gastos/{id}/realizado", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("true"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.dados.mensagem").value("DATA_OBRIGATORIA_PARA_GASTO_REALIZADO"));
	}

	@Test
	void erroInesperadoNaoExpoeDetalhesInternos() throws Exception {
		when(service.buscarPorId(id)).thenThrow(new IllegalStateException("detalhe sensivel"));
		mvc.perform(get("/gastos/{id}", id))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.dados.status").value(500))
				.andExpect(jsonPath("$.dados.mensagem").value("ERRO_INTERNO_DO_SERVIDOR"));
	}
}
