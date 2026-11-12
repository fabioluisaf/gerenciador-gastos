package com.fabioluisaf.gerenciadorgastos.gastos.controller;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.core.TypedPropertyPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.format.support.DefaultFormattingConversionService;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.atualizacao.AtualizarGastoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao.CriarGastoRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.BuscarGastosRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.FiltrarGastosRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.TotalizarGastosRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalCategoriaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalMetodoPagamentoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.InstituicaoFinanceira;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.CompraParcelada;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.Fatura;
import com.fabioluisaf.gerenciadorgastos.gastos.model.Gasto;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.service.GastoService;
import com.fabioluisaf.gerenciadorgastos.gastos.service.GastoConsultaService;
import com.fabioluisaf.gerenciadorgastos.shared.config.ApiProperties;
import com.fabioluisaf.gerenciadorgastos.shared.exception.GlobalExceptionHandler;
import com.fabioluisaf.gerenciadorgastos.shared.exception.NegocioExceptionHandler;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;
import com.fabioluisaf.gerenciadorgastos.shared.serialization.PeriodoConverter;

import jakarta.persistence.EntityNotFoundException;

// Eclipse cannot infer the non-null receiver of Spring Data property references.
@SuppressWarnings("null")
class GastoControllerTest {

	private GastoService service;
	private GastoConsultaService consultaService;
	private MockMvc mvc;
	private UUID id;
	private UUID metodoPagamentoId;
	private MetodoPagamento metodoPagamento;
	private Gasto gasto;

	@BeforeEach
	void configurar() {
		service = mock(GastoService.class);
		consultaService = mock(GastoConsultaService.class);
		id = UUID.randomUUID();
		metodoPagamentoId = UUID.randomUUID();
		metodoPagamento = MetodoPagamento.criar(TipoMetodoPagamento.DEBITO, null, null);
		metodoPagamento.setId(metodoPagamentoId);
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
		final var presenter = new ApiResponsePresenter(new ApiProperties("v1"));
		final var conversionService = new DefaultFormattingConversionService();
		conversionService.addConverter(new PeriodoConverter());
		mvc = MockMvcBuilders.standaloneSetup(
				new GastoController(service, consultaService, presenter)
		).setControllerAdvice(new GlobalExceptionHandler(presenter), new NegocioExceptionHandler(presenter))
				.setValidator(validator)
				.setConversionService(conversionService)
				.setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver()).build();
	}

	@ParameterizedTest
	@ValueSource(strings = { "2026-11", "11-2026", "11/2026", "2026.11", "11.2026" })
	void recebePeriodoDaFaturaNaCriacaoEAtualizacao(String periodo) throws Exception {
		when(service.criar(any())).thenReturn(gasto);
		when(service.atualizar(eq(id), any())).thenReturn(gasto);
		final var conteudo = """
				{"nome":"Compra","valor":25.50,"categoria":"Casa","realizado":false,
				"metodoPagamentoId":"%s","faturaPeriodo":"%s"}
				""".formatted(metodoPagamentoId, periodo);
		mvc.perform(post("/gastos").contentType(MediaType.APPLICATION_JSON).content(conteudo))
				.andExpect(status().isCreated());
		mvc.perform(put("/gastos/{id}", id).contentType(MediaType.APPLICATION_JSON).content(conteudo))
				.andExpect(status().isOk());
		final var criacao = org.mockito.ArgumentCaptor.forClass(CriarGastoRequest.class);
		final var atualizacao = org.mockito.ArgumentCaptor.forClass(AtualizarGastoRequest.class);
		verify(service).criar(criacao.capture());
		verify(service).atualizar(eq(id), atualizacao.capture());
		org.junit.jupiter.api.Assertions.assertEquals(YearMonth.of(2026, 11), criacao.getValue().faturaPeriodo());
		org.junit.jupiter.api.Assertions.assertEquals(YearMonth.of(2026, 11), atualizacao.getValue().faturaPeriodo());
	}

	@Test
	void rejeitaPeriodoDeFaturaInvalido() throws Exception {
		for (final var periodo : List.of("2026-13", "2026-00", "13-2026", "00/2026",
				"10/11/2026", "2026-11-10", "2026-1111/2026")) {
			final var conteudo = """
					{"nome":"Compra","valor":25.50,"categoria":"Casa","realizado":false,
					"metodoPagamentoId":"%s","faturaPeriodo":"%s"}
					""".formatted(metodoPagamentoId, periodo);
			mvc.perform(post("/gastos").contentType(MediaType.APPLICATION_JSON).content(conteudo))
					.andExpect(status().isBadRequest());
			mvc.perform(put("/gastos/{id}", id).contentType(MediaType.APPLICATION_JSON).content(conteudo))
					.andExpect(status().isBadRequest());
		}
		for (final var periodo : List.of("123", "true", "{}", "[]")) {
			final var conteudo = """
					{"nome":"Compra","valor":25.50,"categoria":"Casa","realizado":false,
					"metodoPagamentoId":"%s","faturaPeriodo":%s}
					""".formatted(metodoPagamentoId, periodo);
			mvc.perform(post("/gastos").contentType(MediaType.APPLICATION_JSON).content(conteudo))
					.andExpect(status().isBadRequest());
			mvc.perform(put("/gastos/{id}", id).contentType(MediaType.APPLICATION_JSON).content(conteudo))
					.andExpect(status().isBadRequest());
		}
		verifyNoInteractions(service, consultaService);
	}

	@Test
	void aceitaSomenteHifenOuPontoNoPeriodoDaUrl() throws Exception {
		for (final var endpoint : List.of("/gastos", "/gastos/totais/categoria", "/gastos/totais/metodo-pagamento")) {
			for (final var periodo : List.of("2024-02", "02-2024", "2024.02", "02.2024", "2000-01")) {
				final var esperado = periodo.equals("2000-01") ? YearMonth.of(2000, 1) : YearMonth.of(2024, 2);
				when(consultaService.listarPorMesEAno(eq(new BuscarGastosRequest(esperado)), any(), any()))
						.thenReturn(Page.empty());
				when(consultaService.totalizarPorCategoria(eq(new TotalizarGastosRequest(esperado, null)), any()))
						.thenReturn(Page.empty());
				when(consultaService.totalizarPorMetodoPagamento(eq(new TotalizarGastosRequest(esperado, null)), any()))
						.thenReturn(Page.empty());
				mvc.perform(get(endpoint).param("periodo", periodo)).andExpect(status().isOk());
			}
		}
	}

	@Test
	void rejeitaPeriodoInvalidoOuComBarraNaUrl() throws Exception {
		for (final var endpoint : List.of("/gastos", "/gastos/totais/categoria", "/gastos/totais/metodo-pagamento")) {
			for (final var periodo : List.of("02/2024", "2024/02", "2024-13", "00.2024",
					"2024.02.29", "2024-0202.2024", "1999-12", "invalido")) {
				mvc.perform(get(endpoint).param("periodo", periodo)).andExpect(status().isBadRequest());
			}
		}
		verifyNoInteractions(service, consultaService);
	}

	@Test
	void filtraGastosDoMesEDaFaturaPorCategoriaEMetodo() throws Exception {
		final var faturaId = UUID.randomUUID();
		final var pageable = PageRequest.of(0, 10,
				Sort.by(TypedPropertyPath.path(Gasto::getData), TypedPropertyPath.path(Gasto::getId)));
		final var request = new BuscarGastosRequest(YearMonth.of(2024, 2));
		for (final var filtros : List.of(
				new FiltrarGastosRequest(metodoPagamentoId, null),
				new FiltrarGastosRequest(null, "Alimentacao"),
				new FiltrarGastosRequest(metodoPagamentoId, "Alimentacao")
		)) {
			when(consultaService.listarPorMesEAno(request, filtros, pageable)).thenReturn(Page.empty(pageable));
			final var mensal = get("/gastos").param("periodo", "2024-02");
			if (filtros.metodoPagamentoId() != null) {
				mensal.param("metodoPagamentoId", filtros.metodoPagamentoId().toString());
			}
			if (filtros.categoria() != null) {
				mensal.param("categoria", filtros.categoria());
			}

			mvc.perform(mensal).andExpect(status().isOk()).andExpect(jsonPath("$.dados").isEmpty());

			verify(consultaService).listarPorMesEAno(request, filtros, pageable);
		}
		when(consultaService.listarPorFatura(faturaId, "Alimentacao", pageable)).thenReturn(Page.empty(pageable));
		mvc.perform(get("/gastos/fatura/{id}", faturaId).param("categoria", "Alimentacao")
				.param("metodoPagamentoId", "invalido"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.dados").isEmpty());
		verify(consultaService).listarPorFatura(faturaId, "Alimentacao", pageable);
	}

	@Test
	void rejeitaFiltroDeMetodoInvalido() throws Exception {
		mvc.perform(get("/gastos").param("periodo", "2024-02")
				.param("metodoPagamentoId", "invalido")).andExpect(status().isBadRequest());

		verifyNoInteractions(service, consultaService);
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void totalizaCategoriasDoMesOuFaturaSemAplicarFiltrosDaLista(boolean porFatura) throws Exception {
		final var faturaId = porFatura ? UUID.randomUUID() : null;
		final var request = porFatura ? new TotalizarGastosRequest(null, faturaId)
				: new TotalizarGastosRequest(YearMonth.of(2024, 2), null);
		final var pageable = PageRequest.of(1, 2,
				Sort.by(TypedPropertyPath.path(Gasto::getCategoria)));
		when(consultaService.totalizarPorCategoria(request, pageable)).thenReturn(new PageImpl<>(List.of(
				new TotalCategoriaResponse("Alimentacao", new BigDecimal("125.50"), new BigDecimal("165.50"))
		), pageable, 3));
		final var consulta = get("/gastos/totais/categoria").param("page", "1").param("size", "2")
				.param("categoria", "Outra").param("metodoPagamentoId", metodoPagamentoId.toString());
		if (faturaId != null) {
			consulta.param("faturaId", faturaId.toString());
		} else {
			consulta.param("periodo", "2024-02");
		}

		mvc.perform(consulta).andExpect(status().isOk())
				.andExpect(jsonPath("$.dados[0]").value(aMapWithSize(3)))
				.andExpect(jsonPath("$.dados[0].categoria").value("Alimentacao"))
				.andExpect(jsonPath("$.dados[0].realizado").value(125.50))
				.andExpect(jsonPath("$.dados[0].planejado").value(165.50))
				.andExpect(jsonPath("$.paginacao.pagina").value(1))
				.andExpect(jsonPath("$.paginacao.tamanho").value(2))
				.andExpect(jsonPath("$.paginacao.totalElementos").value(3))
				.andExpect(jsonPath("$.paginacao.totalPaginas").value(2));

		verify(consultaService).totalizarPorCategoria(request, pageable);
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void totalizaMetodosDoMesOuFaturaSemAplicarFiltrosDaLista(boolean porFatura) throws Exception {
		final var faturaId = porFatura ? UUID.randomUUID() : null;
		final var request = porFatura ? new TotalizarGastosRequest(null, faturaId)
				: new TotalizarGastosRequest(YearMonth.of(2024, 2), null);
		final var pageable = PageRequest.of(0, 10,
				Sort.by(TypedPropertyPath.path(Gasto::getMetodoPagamento)
						.then(MetodoPagamento::getId)));
		when(consultaService.totalizarPorMetodoPagamento(request, pageable)).thenReturn(new PageImpl<>(List.of(
				new TotalMetodoPagamentoResponse(metodoPagamentoId, InstituicaoFinanceira.CAIXA,
						TipoMetodoPagamento.DEBITO, "Debito Caixa", new BigDecimal("10.00"), new BigDecimal("25.50"))
		), pageable, 1));
		final var consulta = get("/gastos/totais/metodo-pagamento")
				.param("categoria", "Outra").param("metodoPagamentoId", UUID.randomUUID().toString());
		if (faturaId != null) {
			consulta.param("faturaId", faturaId.toString());
		} else {
			consulta.param("periodo", "2024-02");
		}

		mvc.perform(consulta).andExpect(status().isOk())
				.andExpect(jsonPath("$.dados[0]").value(aMapWithSize(6)))
				.andExpect(jsonPath("$.dados[0].metodoPagamentoId").value(metodoPagamentoId.toString()))
				.andExpect(jsonPath("$.dados[0].instituicaoFinanceira").value("CAIXA"))
				.andExpect(jsonPath("$.dados[0].tipo").value("DEBITO"))
				.andExpect(jsonPath("$.dados[0].nome").value("Debito Caixa"))
				.andExpect(jsonPath("$.dados[0].realizado").value(10.00))
				.andExpect(jsonPath("$.dados[0].planejado").value(25.50))
				.andExpect(jsonPath("$.paginacao.totalElementos").value(1));

		verify(consultaService).totalizarPorMetodoPagamento(request, pageable);
	}

	@ParameterizedTest
	@ValueSource(strings = { "categoria", "metodo-pagamento" })
	void validaEscopoDosTotaisERetornaPaginaVaziaOuFaturaInexistente(String agrupamento) throws Exception {
		final var endpoint = "/gastos/totais/" + agrupamento;
		final var faturaId = UUID.randomUUID();
		for (final var consulta : List.of(
				get(endpoint),
				get(endpoint).param("mes", "2").param("ano", "2024"),
				get(endpoint).param("periodo", "2024-02").param("faturaId", faturaId.toString())
		)) {
			mvc.perform(consulta).andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.dados.errosValidacao.length()").value(1))
					.andExpect(jsonPath("$.dados.errosValidacao[0].campo").value(nullValue()))
					.andExpect(jsonPath("$.dados.errosValidacao[0].codigo").value("INFORME_PERIODO_OU_FATURA"));
		}
		mvc.perform(get(endpoint).param("periodo", "2024-00")).andExpect(status().isBadRequest());
		mvc.perform(get(endpoint).param("periodo", "2024-13")).andExpect(status().isBadRequest());
		mvc.perform(get(endpoint).param("periodo", "1999-02")).andExpect(status().isBadRequest());
		mvc.perform(get(endpoint).param("faturaId", "invalido")).andExpect(status().isBadRequest());
		verifyNoInteractions(service, consultaService);
		final var request = new TotalizarGastosRequest(YearMonth.of(2024, 2), null);
		final var faturaRequest = new TotalizarGastosRequest(null, faturaId);
		when(consultaService.totalizarPorCategoria(eq(request), any())).thenReturn(Page.empty());
		when(consultaService.totalizarPorMetodoPagamento(eq(request), any())).thenReturn(Page.empty());
		when(consultaService.totalizarPorCategoria(eq(faturaRequest), any())).thenThrow(new EntityNotFoundException());
		when(consultaService.totalizarPorMetodoPagamento(eq(faturaRequest), any())).thenThrow(new EntityNotFoundException());

		mvc.perform(get(endpoint).param("periodo", "2024-02"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.dados").isEmpty())
				.andExpect(jsonPath("$.paginacao.totalElementos").value(0));
		mvc.perform(get(endpoint).param("faturaId", faturaId.toString()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.dados.mensagem").value("RECURSO_NAO_ENCONTRADO"));
	}

	@Test
	void listaGastosResumidosPorFaturaComPaginacao() throws Exception {
		final var faturaId = UUID.randomUUID();
		final var pageable = PageRequest.of(1, 2,
				Sort.by(TypedPropertyPath.path(Gasto::getData), TypedPropertyPath.path(Gasto::getId)));
		final var credito = MetodoPagamento.criar(TipoMetodoPagamento.CREDITO, 10, 17);
		credito.setInstituicaoFinanceira(InstituicaoFinanceira.CAIXA);
		gasto.setMetodoPagamento(credito);
		when(consultaService.listarPorFatura(faturaId, null, pageable))
				.thenReturn(new PageImpl<>(List.of(gasto), pageable, 3));

		mvc.perform(get("/gastos/fatura/{id}", faturaId).param("page", "1").param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados.length()").value(1))
				.andExpect(jsonPath("$.dados[0]").value(aMapWithSize(7)))
				.andExpect(jsonPath("$.dados[0].realizado").value(false))
				.andExpect(jsonPath("$.dados[0].id").value(id.toString()))
				.andExpect(jsonPath("$.dados[0].data").value("15/02/2026"))
				.andExpect(jsonPath("$.dados[0].nome").value("Mercado"))
				.andExpect(jsonPath("$.dados[0].valor").value(25.50))
				.andExpect(jsonPath("$.dados[0].categoria").value("Alimentacao"))
				.andExpect(jsonPath("$.dados[0].metodoPagamento").value(aMapWithSize(2)))
				.andExpect(jsonPath("$.dados[0].metodoPagamento.tipo").value("CREDITO"))
				.andExpect(jsonPath("$.dados[0].metodoPagamento.instituicaoFinanceira").value("CAIXA"))
				.andExpect(jsonPath("$.versao").value("v1"))
				.andExpect(jsonPath("$.paginacao.pagina").value(1))
				.andExpect(jsonPath("$.paginacao.tamanho").value(2))
				.andExpect(jsonPath("$.paginacao.totalElementos").value(3))
				.andExpect(jsonPath("$.paginacao.totalPaginas").value(2));

		verify(consultaService).listarPorFatura(faturaId, null, pageable);
	}

	@Test
	void listaFaturaSemGastos() throws Exception {
		final var faturaId = UUID.randomUUID();
		final var pageable = PageRequest.of(0, 10,
				Sort.by(TypedPropertyPath.path(Gasto::getData), TypedPropertyPath.path(Gasto::getId)));
		when(consultaService.listarPorFatura(faturaId, null, pageable))
				.thenReturn(Page.empty(pageable));

		mvc.perform(get("/gastos/fatura/{id}", faturaId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados").isArray())
				.andExpect(jsonPath("$.dados").isEmpty())
				.andExpect(jsonPath("$.paginacao.pagina").value(0))
				.andExpect(jsonPath("$.paginacao.tamanho").value(10))
				.andExpect(jsonPath("$.paginacao.totalElementos").value(0))
				.andExpect(jsonPath("$.paginacao.totalPaginas").value(0));
	}

	@Test
	void rejeitaIdentificadorDeFaturaInvalido() throws Exception {
		mvc.perform(get("/gastos/fatura/invalido")).andExpect(status().isBadRequest());

		verifyNoInteractions(service, consultaService);
	}

	@Test
	void retornaNaoEncontradoParaFaturaInexistente() throws Exception {
		final var faturaId = UUID.randomUUID();
		when(consultaService.listarPorFatura(eq(faturaId), any(), any())).thenThrow(new EntityNotFoundException());

		mvc.perform(get("/gastos/fatura/{id}", faturaId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.dados.mensagem").value("RECURSO_NAO_ENCONTRADO"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "15-02-2026", "15/02/2026", "2026-02-15" })
	void criaGastoComDataAlternativaERetornaResposta(String data) throws Exception {
		when(service.criar(any())).thenAnswer(invocation -> {
			final var request = invocation.getArgument(0, CriarGastoRequest.class);
			final var entidade = request.toEntity();
			entidade.setId(id);
			entidade.setMetodoPagamento(metodoPagamento);

			return entidade;
		});
		mvc.perform(post("/gastos").contentType(MediaType.APPLICATION_JSON).content("""
				{"data":"%s","nome":"Mercado","valor":25.50,"categoria":"Alimentacao",
				"metodoPagamentoId":"%s","descricao":"compra","realizado":false}
				""".formatted(data, metodoPagamentoId)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/gastos/" + id))
				.andExpect(jsonPath("$.versao").value("v1"))
				.andExpect(jsonPath("$.paginacao").doesNotHaveJsonPath())
				.andExpect(jsonPath("$.dados").value(aMapWithSize(7)))
				.andExpect(jsonPath("$.dados.realizado").value(false))
				.andExpect(jsonPath("$.dados.id").value(id.toString()))
				.andExpect(jsonPath("$.dados.data").value("15/02/2026"))
				.andExpect(jsonPath("$.dados.nome").value("Mercado"))
				.andExpect(jsonPath("$.dados.valor").value(25.50))
				.andExpect(jsonPath("$.dados.categoria").value("Alimentacao"))
				.andExpect(jsonPath("$.dados.metodoPagamento").value(aMapWithSize(2)))
				.andExpect(jsonPath("$.dados.metodoPagamento.tipo").value("DEBITO"))
				.andExpect(jsonPath("$.dados.metodoPagamento.instituicaoFinanceira").value("CAIXA"));
		when(service.atualizar(eq(id), any())).thenReturn(gasto);
		mvc.perform(put("/gastos/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"data":"%s","nome":"Mercado","valor":25.50,"categoria":"Alimentacao",
				"metodoPagamentoId":"%s","realizado":false}
				""".formatted(data, metodoPagamentoId))).andExpect(status().isOk());
		final var captor = org.mockito.ArgumentCaptor.forClass(AtualizarGastoRequest.class);
		verify(service).atualizar(eq(id), captor.capture());
		org.junit.jupiter.api.Assertions.assertEquals(LocalDate.of(2026, 2, 15), captor.getValue().data());
	}

	@Test
	void aceitaDataDeAnoBissexto() throws Exception {
		when(service.criar(any())).thenAnswer(invocation -> {
			final var request = invocation.getArgument(0, CriarGastoRequest.class);
			final var entidade = request.toEntity();
			entidade.setId(id);
			entidade.setMetodoPagamento(metodoPagamento);
			return entidade;
		});
		mvc.perform(post("/gastos").contentType(MediaType.APPLICATION_JSON).content("""
				{"data":"29/02/2024","nome":"Compra","valor":10,"categoria":"Casa","realizado":false,
				"metodoPagamentoId":"%s"}
				""".formatted(metodoPagamentoId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.dados.data").value("29/02/2024"));
	}

	@Test
	void rejeitaDatasInvalidasNaCriacaoEAtualizacao() throws Exception {
		for (final var data : List.of("\"31/02/2026\"", "\"29-02-2026\"", "\"2026-04-31\"",
				"\"15/02/202615-02-2026\"", "\"\"", "123", "true", "{}", "[]")) {
			final var conteudo = """
					{"data":%s,"nome":"Compra","valor":10,"categoria":"Casa","realizado":false,
					"metodoPagamentoId":"%s"}
					""".formatted(data, metodoPagamentoId);
			mvc.perform(post("/gastos").contentType(MediaType.APPLICATION_JSON).content(conteudo))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.dados.errosValidacao[0].campo").value("data"))
					.andExpect(jsonPath("$.dados.errosValidacao[0].codigo").value("FORMATO_INVALIDO"));
			mvc.perform(put("/gastos/{id}", id).contentType(MediaType.APPLICATION_JSON).content(conteudo))
					.andExpect(status().isBadRequest());
		}
		verifyNoInteractions(service, consultaService);
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
				.andExpect(jsonPath("$.dados").value(aMapWithSize(7)))
				.andExpect(jsonPath("$.dados.realizado").value(false))
				.andExpect(jsonPath("$.dados.id").value(id.toString()))
				.andExpect(jsonPath("$.dados.data").value(nullValue()))
				.andExpect(jsonPath("$.dados.nome").value("Conta de luz"))
				.andExpect(jsonPath("$.dados.valor").value(80))
				.andExpect(jsonPath("$.dados.categoria").value("Casa"))
				.andExpect(jsonPath("$.dados.metodoPagamento").value(aMapWithSize(2)))
				.andExpect(jsonPath("$.dados.metodoPagamento.tipo").value("DEBITO"))
				.andExpect(jsonPath("$.dados.metodoPagamento.instituicaoFinanceira").value("CAIXA"));
	}

	@Test
	void exigeDataParaCriarGastoRealizado() throws Exception {
		mvc.perform(post("/gastos").contentType(MediaType.APPLICATION_JSON).content("""
				{"data":null,"nome":"Conta de luz","valor":80,"categoria":"Casa",
				"metodoPagamentoId":"%s","realizado":true}
				""".formatted(metodoPagamentoId)))
				.andExpect(status().isBadRequest());

		verify(service, never()).criar(any());
	}

	@Test
	void listaComPaginacao() throws Exception {
		when(consultaService.listarPorMesEAno(any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of(gasto), PageRequest.of(0, 10), 12));
		mvc.perform(get("/gastos").param("periodo", "2024-02").param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados[0]").value(aMapWithSize(7)))
				.andExpect(jsonPath("$.dados[0].realizado").value(false))
				.andExpect(jsonPath("$.dados[0].id").value(id.toString()))
				.andExpect(jsonPath("$.dados[0].data").value("15/02/2026"))
				.andExpect(jsonPath("$.dados[0].nome").value("Mercado"))
				.andExpect(jsonPath("$.dados[0].valor").value(25.50))
				.andExpect(jsonPath("$.dados[0].categoria").value("Alimentacao"))
				.andExpect(jsonPath("$.dados[0].metodoPagamento").value(aMapWithSize(2)))
				.andExpect(jsonPath("$.dados[0].metodoPagamento.tipo").value("DEBITO"))
				.andExpect(jsonPath("$.dados[0].metodoPagamento.instituicaoFinanceira").value("CAIXA"))
				.andExpect(jsonPath("$.paginacao.pagina").value(0))
				.andExpect(jsonPath("$.paginacao.tamanho").value(10))
				.andExpect(jsonPath("$.paginacao.totalElementos").value(12))
				.andExpect(jsonPath("$.paginacao.totalPaginas").value(2))
				.andExpect(jsonPath("$.versao").value("v1"));
	}

	@Test
	void listaVaziaMantemDadosEPaginacao() throws Exception {
		when(consultaService.listarPorMesEAno(any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

		mvc.perform(get("/gastos").param("periodo", "2024-02").param("size", "10"))
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
		when(consultaService.listarPorMesEAno(any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of(gasto), PageRequest.of(0, 10), 1));

		when(consultaService.listarPorFatura(eq(faturaId), any(), any()))
				.thenReturn(new PageImpl<>(List.of(gasto), PageRequest.of(0, 10), 1));

		mvc.perform(get("/gastos/{id}", id)).andExpect(status().isOk())
				.andExpect(jsonPath("$.dados").value(aMapWithSize(10)))
				.andExpect(jsonPath("$.dados.id").value(id.toString()))
				.andExpect(jsonPath("$.dados.data").value("15/02/2026"))
				.andExpect(jsonPath("$.dados.nome").value("Mercado"))
				.andExpect(jsonPath("$.dados.valor").value(25.50))
				.andExpect(jsonPath("$.dados.categoria").value("Alimentacao"))
				.andExpect(jsonPath("$.dados.metodoPagamento.id").value(metodoPagamentoId.toString()))
				.andExpect(jsonPath("$.dados.metodoPagamento.nome").value("Debito Caixa"))
				.andExpect(jsonPath("$.dados.descricao").value("Compra parcelada"))
				.andExpect(jsonPath("$.dados.realizado").value(false))
				.andExpect(jsonPath("$.dados.fatura.id").value(faturaId.toString()))
				.andExpect(jsonPath("$.dados.parcela.numero").value(2))
				.andExpect(jsonPath("$.dados.parcela.quantidade").value(3))
				.andExpect(jsonPath("$.dados.parcela.compraParceladaId").value(compraId.toString()));
		mvc.perform(get("/gastos").param("periodo", "2026-02"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados[0]").value(aMapWithSize(8)))
				.andExpect(jsonPath("$.dados[0].realizado").value(false))
				.andExpect(jsonPath("$.dados[0].parcela").value(aMapWithSize(2)))
				.andExpect(jsonPath("$.dados[0].parcela.numero").value(2))
				.andExpect(jsonPath("$.dados[0].parcela.quantidade").value(3))
				.andExpect(jsonPath("$.dados[0].id").value(id.toString()))
				.andExpect(jsonPath("$.dados[0].data").value("15/02/2026"))
				.andExpect(jsonPath("$.dados[0].nome").value("Mercado"))
				.andExpect(jsonPath("$.dados[0].valor").value(25.50))
				.andExpect(jsonPath("$.dados[0].categoria").value("Alimentacao"))
				.andExpect(jsonPath("$.dados[0].metodoPagamento").value(aMapWithSize(2)))
				.andExpect(jsonPath("$.dados[0].metodoPagamento.tipo").value("DEBITO"))
				.andExpect(jsonPath("$.dados[0].metodoPagamento.instituicaoFinanceira").value("CAIXA"));
		mvc.perform(get("/gastos/fatura/{id}", faturaId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados[0]").value(aMapWithSize(8)))
				.andExpect(jsonPath("$.dados[0].realizado").value(false))
				.andExpect(jsonPath("$.dados[0].parcela").value(aMapWithSize(2)))
				.andExpect(jsonPath("$.dados[0].parcela.numero").value(2))
				.andExpect(jsonPath("$.dados[0].parcela.quantidade").value(3));
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
				.andExpect(jsonPath("$.dados").value(aMapWithSize(7)))
				.andExpect(jsonPath("$.dados.realizado").value(true))
				.andExpect(jsonPath("$.dados.id").value(id.toString()))
				.andExpect(jsonPath("$.dados.data").value("16/02/2026"))
				.andExpect(jsonPath("$.dados.nome").value("Novo"))
				.andExpect(jsonPath("$.dados.valor").value(0))
				.andExpect(jsonPath("$.dados.categoria").value("Casa"))
				.andExpect(jsonPath("$.dados.metodoPagamento").value(aMapWithSize(2)))
				.andExpect(jsonPath("$.dados.metodoPagamento.tipo").value("DEBITO"))
				.andExpect(jsonPath("$.dados.metodoPagamento.instituicaoFinanceira").value("CAIXA"));
		mvc.perform(put("/gastos/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"data":null,"nome":"Pendente","valor":0,"categoria":"Casa",
				"metodoPagamentoId":"%s","realizado":false}
				""".formatted(metodoPagamentoId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados").value(aMapWithSize(7)))
				.andExpect(jsonPath("$.dados.realizado").value(false))
				.andExpect(jsonPath("$.dados.id").value(id.toString()))
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
				.andExpect(jsonPath("$.dados").value(aMapWithSize(7)))
				.andExpect(jsonPath("$.dados.realizado").value(true))
				.andExpect(jsonPath("$.dados.id").value(id.toString()))
				.andExpect(jsonPath("$.dados.data").value("15/02/2026"))
				.andExpect(jsonPath("$.dados.nome").value("Mercado"))
				.andExpect(jsonPath("$.dados.valor").value(25.50))
				.andExpect(jsonPath("$.dados.categoria").value("Alimentacao"))
				.andExpect(jsonPath("$.dados.metodoPagamento").value(aMapWithSize(2)))
				.andExpect(jsonPath("$.dados.metodoPagamento.tipo").value("DEBITO"))
				.andExpect(jsonPath("$.dados.metodoPagamento.instituicaoFinanceira").value("CAIXA"));

		verify(service).atualizarRealizado(id, true);
		assertTrue(gasto.isRealizado());
	}

	@Test
	void rejeitaEstadoDeRealizacaoNulo() throws Exception {
		mvc.perform(patch("/gastos/{id}/realizado", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("null"))
				.andExpect(status().isBadRequest());

		verify(service, never()).atualizarRealizado(id, true);
	}
}
