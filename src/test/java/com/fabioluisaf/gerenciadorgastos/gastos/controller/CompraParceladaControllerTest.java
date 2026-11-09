package com.fabioluisaf.gerenciadorgastos.gastos.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.compraparcelada.AtualizarCompraParceladaRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.ValoresParcelasInvalidosException;
import com.fabioluisaf.gerenciadorgastos.gastos.model.InstituicaoFinanceira;
import com.fabioluisaf.gerenciadorgastos.gastos.model.TipoMetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.CompraParcelada;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.Fatura;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.Gasto;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.service.CompraParceladaService;
import com.fabioluisaf.gerenciadorgastos.shared.config.ApiProperties;
import com.fabioluisaf.gerenciadorgastos.shared.exception.GlobalExceptionHandler;
import com.fabioluisaf.gerenciadorgastos.shared.exception.NegocioExceptionHandler;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;

import jakarta.persistence.EntityNotFoundException;

class CompraParceladaControllerTest {

	private CompraParceladaService service;
	private MockMvc mvc;
	private UUID id;
	private UUID metodoPagamentoId;
	private MetodoPagamento metodoPagamento;
	private CompraParcelada compra;

	@BeforeEach
	void configurar() {
		service = mock(CompraParceladaService.class);
		id = UUID.randomUUID();
		metodoPagamentoId = UUID.randomUUID();
		metodoPagamento = new MetodoPagamento();
		metodoPagamento.setId(metodoPagamentoId);
		metodoPagamento.setTipo(TipoMetodoPagamento.CREDITO);
		metodoPagamento.setInstituicaoFinanceira(InstituicaoFinanceira.NUBANK);
		metodoPagamento.setNome("Cartao Nubank");
		metodoPagamento.setDiaFechamento(10);
		compra = new CompraParcelada();
		compra.setId(id);
		compra.setValorTotal(new BigDecimal("100.00"));
		for (int numero = 2; numero >= 1; numero--) {
			final var parcela = new Gasto();
			parcela.setId(UUID.randomUUID());
			parcela.setNumeroParcela(numero);
			parcela.setData(LocalDate.of(2026, numero, 1));
			parcela.setNome("Compra");
			parcela.setValor(new BigDecimal("50.00"));
			parcela.setCategoria("Casa");
			parcela.setMetodoPagamento(metodoPagamento);
			final var fatura = new Fatura();
			fatura.setId(UUID.randomUUID());
			fatura.setMetodoPagamento(metodoPagamento);
			fatura.setDataFechamento(LocalDate.of(2026, numero + 1, 10));
			parcela.setFatura(fatura);
			parcela.setDescricao("Descricao");
			parcela.setRealizado(true);
			compra.getParcelas().add(parcela);
		}
		final var presenter = new ApiResponsePresenter(new ApiProperties("v1"));
		final var validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mvc = MockMvcBuilders.standaloneSetup(new CompraParceladaController(service, presenter))
				.setControllerAdvice(new GlobalExceptionHandler(presenter), new NegocioExceptionHandler(presenter))
				.setValidator(validator)
				.build();
	}

	@Test
	void criaConsultaAtualizaEExcluiCompra() throws Exception {
		when(service.criar(any())).thenReturn(compra);
		when(service.buscarPorId(id)).thenReturn(compra);
		when(service.atualizar(eq(id), any())).thenReturn(compra);
		mvc.perform(post("/compras-parceladas").contentType(MediaType.APPLICATION_JSON).content("""
				{"data":"2026-01-31","nome":"Compra","valorTotal":100.00,"quantidadeParcelas":2,
				"categoria":"Casa","metodoPagamentoId":"%s","faturaDataFechamento":"10/02/2026",
				"descricao":"Descricao"}
				""".formatted(metodoPagamentoId)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/compras-parceladas/" + id))
				.andExpect(jsonPath("$.versao").value("v1"))
				.andExpect(jsonPath("$.dados.id").value(id.toString()))
				.andExpect(jsonPath("$.dados.valorTotal").value(100.00))
				.andExpect(jsonPath("$.dados.quantidadeParcelas").value(2))
				.andExpect(jsonPath("$.dados.parcelas[0].numeroParcela").value(1))
				.andExpect(jsonPath("$.dados.parcelas[0].id").value(compra.getParcelas().get(1).getId().toString()))
				.andExpect(jsonPath("$.dados.parcelas[0].data").value("01/01/2026"))
				.andExpect(jsonPath("$.dados.parcelas[0].nome").value("Compra"))
				.andExpect(jsonPath("$.dados.parcelas[0].valor").value(50.00))
				.andExpect(jsonPath("$.dados.parcelas[0].categoria").value("Casa"))
				.andExpect(jsonPath("$.dados.parcelas[0].metodoPagamento.id").value(metodoPagamentoId.toString()))
				.andExpect(jsonPath("$.dados.parcelas[0].fatura.dataFechamento").value("2026-02-10"))
				.andExpect(jsonPath("$.dados.parcelas[0].descricao").value("Descricao"))
				.andExpect(jsonPath("$.dados.parcelas[0].realizado").value(true))
				.andExpect(jsonPath("$.dados.parcelas[1].numeroParcela").value(2));
		mvc.perform(get("/compras-parceladas/{id}", id)).andExpect(status().isOk())
				.andExpect(jsonPath("$.dados.id").value(id.toString()));
		mvc.perform(put("/compras-parceladas/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"data":"2026-03-20","nome":"Compra","valorTotal":100.00,"quantidadeParcelas":2,
				"categoria":"Casa","metodoPagamentoId":"%s","faturaDataFechamento":"10/04/2026",
				"descricao":"Descricao","valoresParcelas":[30.25,69.75]}
				""".formatted(metodoPagamentoId))).andExpect(status().isOk())
				.andExpect(jsonPath("$.dados.quantidadeParcelas").value(2));
		mvc.perform(delete("/compras-parceladas/{id}", id)).andExpect(status().isNoContent());
		final var captor = org.mockito.ArgumentCaptor.forClass(AtualizarCompraParceladaRequest.class);
		verify(service).atualizar(eq(id), captor.capture());
		org.junit.jupiter.api.Assertions.assertEquals(LocalDate.of(2026, 3, 20), captor.getValue().data());
		org.junit.jupiter.api.Assertions.assertEquals(metodoPagamentoId, captor.getValue().metodoPagamentoId());
		org.junit.jupiter.api.Assertions.assertEquals(
				List.of(new BigDecimal("30.25"), new BigDecimal("69.75")),
				captor.getValue().valoresParcelas()
		);
		verify(service).excluir(id);
	}

	@Test
	void rejeitaValoresDeParcelasInvalidos() throws Exception {
		for (final var valores : List.of("[null,100]", "[-1,101]", "[0.001,99.999]",
				"[100000000000000000,0]")) {
			mvc.perform(put("/compras-parceladas/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
					{"data":"2026-01-31","nome":"Compra","valorTotal":100,"quantidadeParcelas":2,
					"categoria":"Casa","metodoPagamentoId":"%s","valoresParcelas":%s}
					""".formatted(metodoPagamentoId, valores)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.dados.mensagem").value("ERRO_DE_VALIDACAO"));
		}
		verify(service, org.mockito.Mockito.never()).atualizar(any(), any());
	}

	@Test
	void retornaErroDeNegocioParaSomaOuQuantidadeInvalida() throws Exception {
		for (final var mensagem : List.of(
				"SOMA_PARCELAS_DIFERENTE_DO_TOTAL", "QUANTIDADE_VALORES_PARCELAS_INVALIDA")) {
			when(service.atualizar(eq(id), any())).thenThrow(
					new ValoresParcelasInvalidosException(mensagem)
			);
			mvc.perform(put("/compras-parceladas/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
					{"data":"2026-01-31","nome":"Compra","valorTotal":100,"quantidadeParcelas":2,
					"categoria":"Casa","metodoPagamentoId":"%s","valoresParcelas":[10,20]}
					""".formatted(metodoPagamentoId)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.dados.mensagem").value(mensagem));
		}
	}

	@Test
	void rejeitaDadosInvalidosENotificaCompraInexistente() throws Exception {
		mvc.perform(post("/compras-parceladas").contentType(MediaType.APPLICATION_JSON).content("""
				{"data":"2026-01-31","nome":"","valorTotal":-1,"quantidadeParcelas":0}
				""")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.dados.mensagem").value("ERRO_DE_VALIDACAO"));
		mvc.perform(put("/compras-parceladas/{id}", id).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
		mvc.perform(put("/compras-parceladas/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"nome":"Compra","valorTotal":100.00,"quantidadeParcelas":2,
				"categoria":"Casa","metodoPagamentoId":"%s"}
				""".formatted(metodoPagamentoId))
				)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.dados.errosValidacao[?(@.campo == 'data')].codigo")
						.value(org.hamcrest.Matchers.hasItem("CAMPO_OBRIGATORIO")));
		when(service.buscarPorId(id)).thenThrow(new EntityNotFoundException());
		when(service.atualizar(eq(id), any())).thenThrow(new EntityNotFoundException());
		doThrow(new EntityNotFoundException()).when(service).excluir(id);
		mvc.perform(get("/compras-parceladas/{id}", id)).andExpect(status().isNotFound());
		mvc.perform(put("/compras-parceladas/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"data":"2026-01-31","nome":"Compra","valorTotal":100.00,"quantidadeParcelas":2,
				"categoria":"Casa","metodoPagamentoId":"%s"}
				""".formatted(metodoPagamentoId))).andExpect(status().isNotFound());
		mvc.perform(delete("/compras-parceladas/{id}", id)).andExpect(status().isNotFound());
	}
}
