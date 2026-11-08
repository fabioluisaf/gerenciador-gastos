package com.fabioluisaf.gerenciadorgastos.gastos.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.AtualizarGastoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.CriarGastoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.model.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.Gasto;
import com.fabioluisaf.gerenciadorgastos.gastos.service.GastoService;
import com.fabioluisaf.gerenciadorgastos.shared.config.ApiProperties;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;

class GastoControllerTest {

	private GastoService service;
	private MockMvc mvc;
	private UUID id;
	private Gasto gasto;

	@BeforeEach
	void configurar() {
		service = mock(GastoService.class);
		id = UUID.randomUUID();
		gasto = new Gasto(
				id, LocalDate.of(2026, 2, 15), "Mercado", new BigDecimal("25.50"),
				"Alimentacao", MetodoPagamento.PIX_CAIXA, null, false
		);
		final var validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mvc = MockMvcBuilders.standaloneSetup(
				new GastoController(service, new ApiResponsePresenter(new ApiProperties("v1")))
		).setValidator(validator).setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver()).build();
	}

	@Test
	void criaGastoComDataAlternativaERetornaResposta() throws Exception {
		when(service.criar(any())).thenAnswer(invocation -> {
			final var request = invocation.getArgument(0, CriarGastoRequest.class);
			final var entidade = request.toEntity();
			entidade.setId(id);
			return entidade;
		});
		mvc.perform(post("/gastos").contentType(MediaType.APPLICATION_JSON).content("""
				{"data":"15-02-2026","nome":"Mercado","valor":25.50,"categoria":"Alimentacao",
				"metodoPagamento":"PIX_CAIXA","descricao":"compra","realizado":false}
				"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/gastos/" + id))
				.andExpect(jsonPath("$.versao").value("v1"))
				.andExpect(jsonPath("$.dados.id").value(id.toString()))
				.andExpect(jsonPath("$.dados.data").value("15/02/2026"))
				.andExpect(jsonPath("$.dados.nome").value("Mercado"))
				.andExpect(jsonPath("$.dados.valor").value(25.50))
				.andExpect(jsonPath("$.dados.categoria").value("Alimentacao"))
				.andExpect(jsonPath("$.dados.metodoPagamento").value("PIX_CAIXA"))
				.andExpect(jsonPath("$.dados.descricao").value("compra"))
				.andExpect(jsonPath("$.dados.realizado").value(false));
	}

	@Test
	void listaComPaginacao() throws Exception {
		when(service.listarPorMesEAno(any(), any()))
				.thenReturn(new PageImpl<>(List.of(gasto), PageRequest.of(0, 10), 12));
		mvc.perform(get("/gastos").param("mes", "2").param("ano", "2024").param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados[0].id").value(id.toString()))
				.andExpect(jsonPath("$.paginacao.pagina").value(0))
				.andExpect(jsonPath("$.paginacao.tamanho").value(10))
				.andExpect(jsonPath("$.paginacao.totalElementos").value(12))
				.andExpect(jsonPath("$.paginacao.totalPaginas").value(2))
				.andExpect(jsonPath("$.versao").value("v1"));
	}

	@Test
	void consultaAtualizaEExcluiGasto() throws Exception {
		when(service.buscarPorId(id)).thenReturn(gasto);
		when(service.atualizar(eq(id), any())).thenAnswer(invocation -> {
			final var request = invocation.getArgument(1, AtualizarGastoRequest.class);
			return new Gasto(
					id, request.data(), request.nome(), request.valor(), request.categoria(),
					request.metodoPagamento(), request.descricao(), request.realizado()
			);
		});
		mvc.perform(get("/gastos/{id}", id)).andExpect(status().isOk())
				.andExpect(jsonPath("$.dados.id").value(id.toString()));
		mvc.perform(put("/gastos/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"data":"16/02/2026","nome":"Novo","valor":0,"categoria":"Casa",
				"metodoPagamento":"DEBITO_NUBANK","descricao":"atualizado","realizado":true}
				"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados.id").value(id.toString()))
				.andExpect(jsonPath("$.dados.data").value("16/02/2026"))
				.andExpect(jsonPath("$.dados.nome").value("Novo"))
				.andExpect(jsonPath("$.dados.valor").value(0))
				.andExpect(jsonPath("$.dados.categoria").value("Casa"))
				.andExpect(jsonPath("$.dados.metodoPagamento").value("DEBITO_NUBANK"))
				.andExpect(jsonPath("$.dados.descricao").value("atualizado"))
				.andExpect(jsonPath("$.dados.realizado").value(true));
		mvc.perform(delete("/gastos/{id}", id)).andExpect(status().isNoContent());
		verify(service).excluir(id);
	}
}
