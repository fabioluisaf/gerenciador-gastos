package com.fabioluisaf.gerenciadorgastos.gastos.controller;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.gasto.AtualizarGastoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.gasto.CriarGastoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.model.InstituicaoFinanceira;
import com.fabioluisaf.gerenciadorgastos.gastos.model.TipoMetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.CompraParcelada;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.Fatura;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.Gasto;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.service.GastoService;
import com.fabioluisaf.gerenciadorgastos.shared.config.ApiProperties;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;

class GastoControllerTest {

	private GastoService service;
	private MockMvc mvc;
	private UUID id;
	private UUID metodoPagamentoId;
	private MetodoPagamento metodoPagamento;
	private Gasto gasto;

	@BeforeEach
	void configurar() {
		service = mock(GastoService.class);
		id = UUID.randomUUID();
		metodoPagamentoId = UUID.randomUUID();
		metodoPagamento = new MetodoPagamento();
		metodoPagamento.setId(metodoPagamentoId);
		metodoPagamento.setTipo(TipoMetodoPagamento.DEBITO);
		metodoPagamento.setInstituicaoFinanceira(InstituicaoFinanceira.CAIXA);
		metodoPagamento.setNome("Debito Caixa");
		gasto = new Gasto();
		gasto.setId(id);
		gasto.setData(LocalDate.of(2026, 2, 15));
		gasto.setNome("Mercado");
		gasto.setValor(new BigDecimal("25.50"));
		gasto.setCategoria("Alimentacao");
		gasto.setMetodoPagamento(metodoPagamento);
		gasto.setDescricao(null);
		gasto.setRealizado(false);
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
			entidade.setMetodoPagamento(metodoPagamento);

			return entidade;
		});
		mvc.perform(post("/gastos").contentType(MediaType.APPLICATION_JSON).content("""
				{"data":"15-02-2026","nome":"Mercado","valor":25.50,"categoria":"Alimentacao",
				"metodoPagamentoId":"%s","descricao":"compra","realizado":false}
				""".formatted(metodoPagamentoId)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/gastos/" + id))
				.andExpect(jsonPath("$.versao").value("v1"))
				.andExpect(jsonPath("$.paginacao").doesNotHaveJsonPath())
				.andExpect(jsonPath("$.dados").value(aMapWithSize(5)))
				.andExpect(jsonPath("$.dados.data").value("15/02/2026"))
				.andExpect(jsonPath("$.dados.nome").value("Mercado"))
				.andExpect(jsonPath("$.dados.valor").value(25.50))
				.andExpect(jsonPath("$.dados.categoria").value("Alimentacao"))
				.andExpect(jsonPath("$.dados.metodoPagamento.id").value(metodoPagamentoId.toString()))
				.andExpect(jsonPath("$.dados.metodoPagamento.tipo").value("DEBITO"))
				.andExpect(jsonPath("$.dados.metodoPagamento.instituicaoFinanceira").value("CAIXA"));
	}

	@Test
	void criaGastoPendenteSemData() throws Exception {
		when(service.criar(any())).thenAnswer(invocation -> {
			final var request = invocation.getArgument(0, CriarGastoRequest.class);
			final var entidade = request.toEntity();
			entidade.setId(id);
			entidade.setMetodoPagamento(metodoPagamento);
			return entidade;
		});

		mvc.perform(post("/gastos").contentType(MediaType.APPLICATION_JSON).content("""
				{"data":null,"nome":"Conta de luz","valor":80,"categoria":"Casa",
				"metodoPagamentoId":"%s","realizado":false}
				""".formatted(metodoPagamentoId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.dados").value(aMapWithSize(5)))
				.andExpect(jsonPath("$.dados.data").value(nullValue()))
				.andExpect(jsonPath("$.dados.nome").value("Conta de luz"))
				.andExpect(jsonPath("$.dados.valor").value(80))
				.andExpect(jsonPath("$.dados.categoria").value("Casa"))
				.andExpect(jsonPath("$.dados.metodoPagamento.id").value(metodoPagamentoId.toString()));
	}

	@Test
	void exigeDataParaCriarGastoRealizado() throws Exception {
		mvc.perform(post("/gastos").contentType(MediaType.APPLICATION_JSON).content("""
				{"data":null,"nome":"Conta de luz","valor":80,"categoria":"Casa",
				"metodoPagamentoId":"%s","realizado":true}
				""".formatted(metodoPagamentoId)))
				.andExpect(status().isBadRequest());

		verify(service, org.mockito.Mockito.never()).criar(any());
	}

	@Test
	void listaComPaginacao() throws Exception {
		when(service.listarPorMesEAno(any(), any()))
				.thenReturn(new PageImpl<>(List.of(gasto), PageRequest.of(0, 10), 12));
		mvc.perform(get("/gastos").param("mes", "2").param("ano", "2024").param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados[0]").value(aMapWithSize(5)))
				.andExpect(jsonPath("$.dados[0].data").value("15/02/2026"))
				.andExpect(jsonPath("$.dados[0].nome").value("Mercado"))
				.andExpect(jsonPath("$.dados[0].valor").value(25.50))
				.andExpect(jsonPath("$.dados[0].categoria").value("Alimentacao"))
				.andExpect(jsonPath("$.dados[0].metodoPagamento.id").value(metodoPagamentoId.toString()))
				.andExpect(jsonPath("$.paginacao.pagina").value(0))
				.andExpect(jsonPath("$.paginacao.tamanho").value(10))
				.andExpect(jsonPath("$.paginacao.totalElementos").value(12))
				.andExpect(jsonPath("$.paginacao.totalPaginas").value(2))
				.andExpect(jsonPath("$.versao").value("v1"));
	}

	@Test
	void listaVaziaMantemDadosEPaginacao() throws Exception {
		when(service.listarPorMesEAno(any(), any()))
				.thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

		mvc.perform(get("/gastos").param("mes", "2").param("ano", "2024").param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados").isArray())
				.andExpect(jsonPath("$.dados").isEmpty())
				.andExpect(jsonPath("$.paginacao.pagina").value(0))
				.andExpect(jsonPath("$.paginacao.tamanho").value(10))
				.andExpect(jsonPath("$.paginacao.totalElementos").value(0))
				.andExpect(jsonPath("$.paginacao.totalPaginas").value(0))
				.andExpect(jsonPath("$.versao").value("v1"));
	}

	@Test
	void exibeDetalhesSomenteNaConsultaPorId() throws Exception {
		final var compraId = UUID.randomUUID();
		final var compra = mock(CompraParcelada.class);
		when(compra.getId()).thenReturn(compraId);
		when(compra.getQuantidadeParcelas()).thenReturn(3L);
		gasto.setCompraParcelada(compra);
		gasto.setNumeroParcela(2);
		gasto.setDescricao("Compra parcelada");
		final var fatura = new Fatura();
		final var faturaId = UUID.randomUUID();
		fatura.setId(faturaId);
		fatura.setMetodoPagamento(metodoPagamento);
		fatura.setDataFechamento(LocalDate.of(2026, 3, 10));
		gasto.setFatura(fatura);
		when(service.buscarPorId(id)).thenReturn(gasto);
		when(service.listarPorMesEAno(any(), any()))
				.thenReturn(new PageImpl<>(List.of(gasto), PageRequest.of(0, 10), 1));

		mvc.perform(get("/gastos/{id}", id)).andExpect(status().isOk())
				.andExpect(jsonPath("$.dados").value(aMapWithSize(10)))
				.andExpect(jsonPath("$.dados.id").value(id.toString()))
				.andExpect(jsonPath("$.dados.data").value("15/02/2026"))
				.andExpect(jsonPath("$.dados.nome").value("Mercado"))
				.andExpect(jsonPath("$.dados.valor").value(25.50))
				.andExpect(jsonPath("$.dados.categoria").value("Alimentacao"))
				.andExpect(jsonPath("$.dados.metodoPagamento.id").value(metodoPagamentoId.toString()))
				.andExpect(jsonPath("$.dados.descricao").value("Compra parcelada"))
				.andExpect(jsonPath("$.dados.realizado").value(false))
				.andExpect(jsonPath("$.dados.fatura.id").value(faturaId.toString()))
				.andExpect(jsonPath("$.dados.parcela.numero").value(2))
				.andExpect(jsonPath("$.dados.parcela.quantidade").value(3))
				.andExpect(jsonPath("$.dados.parcela.compraParceladaId").value(compraId.toString()));
		mvc.perform(get("/gastos").param("mes", "2").param("ano", "2026"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados[0]").value(aMapWithSize(5)))
				.andExpect(jsonPath("$.dados[0].data").value("15/02/2026"))
				.andExpect(jsonPath("$.dados[0].nome").value("Mercado"))
				.andExpect(jsonPath("$.dados[0].valor").value(25.50))
				.andExpect(jsonPath("$.dados[0].categoria").value("Alimentacao"))
				.andExpect(jsonPath("$.dados[0].metodoPagamento.id").value(metodoPagamentoId.toString()));
	}

	@Test
	void consultaAtualizaEExcluiGasto() throws Exception {
		when(service.buscarPorId(id)).thenReturn(gasto);
		when(service.atualizar(eq(id), any())).thenAnswer(invocation -> {
			final var request = invocation.getArgument(1, AtualizarGastoRequest.class);
			final var atualizado = new Gasto();
			atualizado.setId(id);
			atualizado.setData(request.data());
			atualizado.setNome(request.nome());
			atualizado.setValor(request.valor());
			atualizado.setCategoria(request.categoria());
			atualizado.setMetodoPagamento(metodoPagamento);
			atualizado.setDescricao(request.descricao());
			atualizado.setRealizado(request.realizado());

			return atualizado;
		});
		mvc.perform(get("/gastos/{id}", id)).andExpect(status().isOk())
				.andExpect(jsonPath("$.dados.id").value(id.toString()));
		mvc.perform(put("/gastos/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"data":"16/02/2026","nome":"Novo","valor":0,"categoria":"Casa",
				"metodoPagamentoId":"%s","descricao":"atualizado","realizado":true}
				""".formatted(metodoPagamentoId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados").value(aMapWithSize(5)))
				.andExpect(jsonPath("$.dados.data").value("16/02/2026"))
				.andExpect(jsonPath("$.dados.nome").value("Novo"))
				.andExpect(jsonPath("$.dados.valor").value(0))
				.andExpect(jsonPath("$.dados.categoria").value("Casa"))
				.andExpect(jsonPath("$.dados.metodoPagamento.id").value(metodoPagamentoId.toString()));
		mvc.perform(put("/gastos/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"data":null,"nome":"Pendente","valor":0,"categoria":"Casa",
				"metodoPagamentoId":"%s","realizado":false}
				""".formatted(metodoPagamentoId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados").value(aMapWithSize(5)))
				.andExpect(jsonPath("$.dados.data").value(nullValue()))
				.andExpect(jsonPath("$.dados.nome").value("Pendente"));
		mvc.perform(delete("/gastos/{id}", id)).andExpect(status().isNoContent());
		verify(service).excluir(id);
	}

	@Test
	void atualizaSomenteSeGastoFoiRealizado() throws Exception {
		gasto.setData(LocalDate.of(2026, 2, 15));
		when(service.atualizarRealizado(id, true)).thenAnswer(invocation -> {
			gasto.setRealizado(invocation.getArgument(1, Boolean.class));

			return gasto;
		});

		mvc.perform(patch("/gastos/{id}/realizado", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						true
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados").value(aMapWithSize(5)))
				.andExpect(jsonPath("$.dados.data").value("15/02/2026"))
				.andExpect(jsonPath("$.dados.nome").value("Mercado"))
				.andExpect(jsonPath("$.dados.valor").value(25.50))
				.andExpect(jsonPath("$.dados.categoria").value("Alimentacao"))
				.andExpect(jsonPath("$.dados.metodoPagamento.id").value(metodoPagamentoId.toString()));

		verify(service).atualizarRealizado(id, true);
		assertTrue(gasto.isRealizado());
	}

	@Test
	void rejeitaEstadoDeRealizacaoNulo() throws Exception {
		mvc.perform(patch("/gastos/{id}/realizado", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("null"))
				.andExpect(status().isBadRequest());

		verify(service, org.mockito.Mockito.never()).atualizarRealizado(id, true);
	}
}
