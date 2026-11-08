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

import com.fabioluisaf.gerenciadorgastos.gastos.controller.GastoController;
import com.fabioluisaf.gerenciadorgastos.gastos.service.GastoService;
import com.fabioluisaf.gerenciadorgastos.shared.config.ApiProperties;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;

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
		mvc = MockMvcBuilders.standaloneSetup(new GastoController(service, presenter))
				.setControllerAdvice(new GlobalExceptionHandler(presenter))
				.setValidator(validator)
				.setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
				.build();
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
				.andExpect(jsonPath("$.dados.errosValidacao[?(@.campo == 'nome')].codigo", hasItem("NotBlank")))
				.andExpect(jsonPath("$.dados.errosValidacao[?(@.campo == 'valor')].codigo", hasItem("PositiveOrZero")))
				.andExpect(jsonPath("$.dados.errosValidacao[?(@.campo == 'data')].codigo", hasItem("NotNull")))
				.andExpect(jsonPath("$.dados.errosValidacao[?(@.campo == 'realizado')].codigo", hasItem("NotNull")));
		mvc.perform(get("/gastos").param("mes", "13").param("ano", "1999"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.dados.errosValidacao[?(@.campo == 'mes')].codigo", hasItem("Max")))
				.andExpect(jsonPath("$.dados.errosValidacao[?(@.campo == 'ano')].codigo", hasItem("Min")));
		mvc.perform(get("/gastos"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.dados.errosValidacao[?(@.campo == 'mes')].codigo", hasItem("NotNull")))
				.andExpect(jsonPath("$.dados.errosValidacao[?(@.campo == 'ano')].codigo", hasItem("NotNull")));
		mvc.perform(put("/gastos/{id}", id).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.dados.errosValidacao[?(@.campo == 'nome')].codigo", hasItem("NotBlank")));
	}

	@Test
	void trataErrosHttpERecursosAusentes() throws Exception {
		when(service.buscarPorId(id)).thenThrow(new EntityNotFoundException());
		doThrow(new EntityNotFoundException()).when(service).excluir(id);
		when(service.atualizar(eq(id), any())).thenThrow(new EntityNotFoundException());
		mvc.perform(get("/gastos/{id}", id))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.dados.status").value(404))
				.andExpect(jsonPath("$.dados.errosValidacao").doesNotExist());
		mvc.perform(delete("/gastos/{id}", id)).andExpect(status().isNotFound());
		mvc.perform(put("/gastos/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"data":"15/02/2026","nome":"Mercado","valor":25.50,"categoria":"Alimentacao",
				"metodoPagamento":"PIX_CAIXA","realizado":true}
				""")).andExpect(status().isNotFound());
		mvc.perform(get("/gastos/uuid-invalido")).andExpect(status().isBadRequest());
		mvc.perform(post("/gastos").contentType(MediaType.APPLICATION_JSON).content("{"))
				.andExpect(status().isBadRequest());
		mvc.perform(patch("/gastos/{id}", id)).andExpect(status().isMethodNotAllowed())
				.andExpect(header().exists("Allow"));
		mvc.perform(post("/gastos").contentType(MediaType.TEXT_PLAIN).content("texto"))
				.andExpect(status().isUnsupportedMediaType());
	}

	@Test
	void erroInesperadoNaoExpoeDetalhesInternos() throws Exception {
		when(service.buscarPorId(id)).thenThrow(new IllegalStateException("detalhe sensivel"));
		mvc.perform(get("/gastos/{id}", id))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.dados.status").value(500))
				.andExpect(jsonPath("$.dados.mensagem").value("Erro interno do servidor."));
	}
}
