package com.fabioluisaf.gerenciadorgastos.gastos.controller;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.comum.InstituicaoFinanceiraResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.atualizacao.AtualizarGastoBody;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.BuscarGastosPorIntervaloUrlParams;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.FiltrarGastosUrlParams;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.FiltrarPeriodosUrlParams;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao.CriarGastoBody;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem.PeriodoComGastosResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalCategoriaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalMetodoPagamentoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.model.Gasto;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.InstituicoesTeste;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.service.GastoConsultaService;
import com.fabioluisaf.gerenciadorgastos.gastos.service.GastoService;
import com.fabioluisaf.gerenciadorgastos.shared.config.ApiProperties;
import com.fabioluisaf.gerenciadorgastos.shared.exception.GlobalExceptionHandler;
import com.fabioluisaf.gerenciadorgastos.shared.exception.NegocioExceptionHandler;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;
import com.fabioluisaf.gerenciadorgastos.shared.serialization.DataConverter;
import com.fabioluisaf.gerenciadorgastos.shared.serialization.PeriodoConverter;

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
		metodoPagamento.setInstituicaoFinanceira(InstituicoesTeste.caixa());
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
		conversionService.addConverter(new DataConverter());
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
		final var criacao = org.mockito.ArgumentCaptor.forClass(CriarGastoBody.class);
		final var atualizacao = org.mockito.ArgumentCaptor.forClass(AtualizarGastoBody.class);
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
	void listaPeriodosDosGastosRealizadosComQuantidade() throws Exception {
		when(consultaService.listarPeriodosRealizados(any())).thenReturn(List.of(
				new PeriodoComGastosResponse(2025, 12, 31),
				new PeriodoComGastosResponse(2024, 2, 4)
		));

		mvc.perform(get("/gastos/periodos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados[0].ano").value(2025))
				.andExpect(jsonPath("$.dados[0].mes").value(12))
				.andExpect(jsonPath("$.dados[0].quantidade").value(31))
				.andExpect(jsonPath("$.dados[1].ano").value(2024))
				.andExpect(jsonPath("$.dados[1].mes").value(2))
				.andExpect(jsonPath("$.dados[1].quantidade").value(4))
				.andExpect(jsonPath("$.paginacao").doesNotHaveJsonPath());

		verify(consultaService).listarPeriodosRealizados(new FiltrarPeriodosUrlParams(null, null));

		final var metodoPagamentoId = UUID.randomUUID();
		mvc.perform(get("/gastos/periodos").param("metodoPagamentoId", metodoPagamentoId.toString())
				.param("categoria", "Casa", "Lazer"))
				.andExpect(status().isOk());
		verify(consultaService).listarPeriodosRealizados(
				new FiltrarPeriodosUrlParams(List.of(metodoPagamentoId), List.of("Casa", "Lazer")));
	}

	@Test
	void listaPeriodosDosGastosPlanejadosComGrupoSemData() throws Exception {
		when(consultaService.listarPeriodosPlanejados(any())).thenReturn(List.of(
				new PeriodoComGastosResponse(2026, 12, 3),
				new PeriodoComGastosResponse(null, null, 5)
		));

		mvc.perform(get("/gastos/planejados/periodos").param("categoria", "Casa"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados[0].ano").value(2026))
				.andExpect(jsonPath("$.dados[1].ano").doesNotExist())
				.andExpect(jsonPath("$.dados[1].quantidade").value(5));

		verify(consultaService).listarPeriodosPlanejados(new FiltrarPeriodosUrlParams(null, List.of("Casa")));
	}

	@Test
	void listaTodosOsGastosComPaginacaoEFiltrosOpcionais() throws Exception {
		final var inicio = LocalDate.of(2026, 11, 1);
		final var fim = LocalDate.of(2026, 11, 30);
		final var ordenacao = Sort.by(TypedPropertyPath.path(Gasto::getData), TypedPropertyPath.path(Gasto::getId))
				.descending();
		final var pageable = PageRequest.of(1, 1, ordenacao);
		final var semFiltros = new FiltrarGastosUrlParams(null, null, null, null, null);
		when(consultaService.listarRealizados(any(), any())).thenReturn(Page.empty());
		when(consultaService.listarRealizados(semFiltros, pageable))
				.thenReturn(new PageImpl<>(List.of(gasto), pageable, 2));

		mvc.perform(get("/gastos").param("page", "1").param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados[0].id").value(id.toString()))
				.andExpect(jsonPath("$.paginacao.totalElementos").value(2));
		verify(consultaService).listarRealizados(semFiltros, pageable);

		final var pix = UUID.randomUUID();
		final var cartao = UUID.randomUUID();
		final var faturaId = UUID.randomUUID();
		mvc.perform(get("/gastos").param("inicio", "2026-11-01").param("fim", "2026-11-30")
				.param("metodoPagamentoId", pix.toString(), cartao.toString()).param("categoria", "Casa", "Lazer")
				.param("faturaId", faturaId.toString()))
				.andExpect(status().isOk());
		verify(consultaService).listarRealizados(
				new FiltrarGastosUrlParams(inicio, fim, List.of(pix, cartao), List.of("Casa", "Lazer"), faturaId),
				PageRequest.of(0, 10, ordenacao));

		mvc.perform(get("/gastos").param("inicio", "2026-11-01")).andExpect(status().isOk());
		mvc.perform(get("/gastos").param("fim", "2026-11-30")).andExpect(status().isOk());
		mvc.perform(get("/gastos").param("inicio", "2026-11-01").param("fim", "2026-11-01"))
				.andExpect(status().isOk());
		mvc.perform(get("/gastos").param("inicio", "2026-11-30").param("fim", "2026-11-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.dados.errosValidacao[0].codigo").value("INTERVALO_INVALIDO"));
		mvc.perform(get("/gastos").param("inicio", "invalido"))
				.andExpect(status().isBadRequest());
		verify(consultaService, never()).listarPlanejados(any(), any());
	}

	@Test
	void listaPlanejadosEmEndpointProprioComOsMesmosFiltros() throws Exception {
		final var ordenacao = Sort.by(TypedPropertyPath.path(Gasto::getData), TypedPropertyPath.path(Gasto::getId));
		final var faturaId = UUID.randomUUID();
		final var filtros = new FiltrarGastosUrlParams(null, LocalDate.of(2026, 12, 31), null, List.of("Casa"), faturaId);
		when(consultaService.listarPlanejados(any(), any())).thenReturn(Page.empty());
		when(consultaService.listarPlanejados(filtros, PageRequest.of(0, 10, ordenacao)))
				.thenReturn(new PageImpl<>(List.of(gasto), PageRequest.of(0, 10, ordenacao), 1));

		mvc.perform(get("/gastos/planejados").param("fim", "2026-12-31").param("categoria", "Casa")
				.param("faturaId", faturaId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados[0].id").value(id.toString()));
		mvc.perform(get("/gastos/planejados").param("inicio", "2026-12-31").param("fim", "2026-12-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.dados.errosValidacao[0].codigo").value("INTERVALO_INVALIDO"));
		verify(consultaService, never()).listarRealizados(any(), any());
	}

	@ParameterizedTest
	@ValueSource(strings = { "01/11/2026", "01-11-2026", "2026-11-01" })
	void aceitaMultiplosFormatosDeDataNaConsulta(String data) throws Exception {
		final var dataEsperada = LocalDate.of(2026, 11, 1);
		final var filtros = new FiltrarGastosUrlParams(dataEsperada, dataEsperada, null, null, null);
		when(consultaService.listarRealizados(eq(filtros), any())).thenReturn(Page.empty());

		mvc.perform(get("/gastos").param("inicio", data).param("fim", data))
				.andExpect(status().isOk());
	}

	@Test
	void consultasRemovidasNaoEstaoMaisDisponiveis() throws Exception {
		mvc.perform(get("/gastos/intervalo").param("inicio", "2024-02-01").param("fim", "2024-02-29"))
				.andExpect(status().isBadRequest());
		mvc.perform(get("/gastos/fatura/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
		verifyNoInteractions(service, consultaService);
	}

	@Test
	void rejeitaFiltrosDeMetodoEFaturaInvalidos() throws Exception {
		mvc.perform(get("/gastos").param("metodoPagamentoId", "invalido")).andExpect(status().isBadRequest());
		mvc.perform(get("/gastos").param("faturaId", "invalido")).andExpect(status().isBadRequest());

		verifyNoInteractions(service, consultaService);
	}

	@Test
	void totalizaCategoriasDoIntervaloSemAplicarFiltrosDaLista() throws Exception {
		final var request = new BuscarGastosPorIntervaloUrlParams(
				LocalDate.of(2024, 2, 15), LocalDate.of(2024, 3, 5));
		final var pageable = PageRequest.of(1, 2,
				Sort.by(TypedPropertyPath.path(Gasto::getCategoria)));
		when(consultaService.totalizarPorCategoria(request, pageable)).thenReturn(new PageImpl<>(List.of(
				new TotalCategoriaResponse("Alimentacao", new BigDecimal("125.50"), new BigDecimal("165.50"))
		), pageable, 3));
		final var consulta = get("/gastos/totais/categoria").param("page", "1").param("size", "2")
				.param("categoria", "Outra").param("metodoPagamentoId", metodoPagamentoId.toString())
				.param("inicio", "2024-02-15").param("fim", "2024-03-05");

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

	@Test
	void totalizaMetodosDoIntervaloSemAplicarFiltrosDaLista() throws Exception {
		final var request = new BuscarGastosPorIntervaloUrlParams(
				LocalDate.of(2024, 2, 15), LocalDate.of(2024, 3, 5));
		final var pageable = PageRequest.of(0, 10,
				Sort.by(TypedPropertyPath.path(Gasto::getMetodoPagamento)
						.then(MetodoPagamento::getId)));
		when(consultaService.totalizarPorMetodoPagamento(request, pageable)).thenReturn(new PageImpl<>(List.of(
				new TotalMetodoPagamentoResponse(metodoPagamentoId, InstituicaoFinanceiraResponse.from(InstituicoesTeste.caixa()),
						TipoMetodoPagamento.DEBITO, "Debito Caixa", new BigDecimal("10.00"), new BigDecimal("25.50"))
		), pageable, 1));
		final var consulta = get("/gastos/totais/metodo-pagamento")
				.param("categoria", "Outra").param("metodoPagamentoId", UUID.randomUUID().toString())
				.param("inicio", "2024-02-15").param("fim", "2024-03-05");

		mvc.perform(consulta).andExpect(status().isOk())
				.andExpect(jsonPath("$.dados[0]").value(aMapWithSize(6)))
				.andExpect(jsonPath("$.dados[0].metodoPagamentoId").value(metodoPagamentoId.toString()))
				.andExpect(jsonPath("$.dados[0].instituicaoFinanceira.nome").value("Caixa"))
				.andExpect(jsonPath("$.dados[0].tipo").value("DEBITO"))
				.andExpect(jsonPath("$.dados[0].nome").value("Debito Caixa"))
				.andExpect(jsonPath("$.dados[0].realizado").value(10.00))
				.andExpect(jsonPath("$.dados[0].planejado").value(25.50))
				.andExpect(jsonPath("$.paginacao.totalElementos").value(1));

		verify(consultaService).totalizarPorMetodoPagamento(request, pageable);
	}

	@ParameterizedTest
	@ValueSource(strings = { "categoria", "metodo-pagamento" })
	void validaIntervaloDosTotaisERetornaPaginaVazia(String agrupamento) throws Exception {
		final var endpoint = "/gastos/totais/" + agrupamento;
		for (final var consulta : List.of(
				get(endpoint),
				get(endpoint).param("inicio", "2024-02-01"),
				get(endpoint).param("fim", "2024-02-29"),
				get(endpoint).param("periodo", "2024-02"),
				get(endpoint).param("faturaId", UUID.randomUUID().toString())
		)) {
			mvc.perform(consulta).andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.dados.errosValidacao[0].codigo").value("CAMPO_OBRIGATORIO"));
		}
		mvc.perform(get(endpoint).param("inicio", "2024-03-05").param("fim", "2024-02-15"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.dados.errosValidacao[0].codigo").value("INTERVALO_INVALIDO"));
		mvc.perform(get(endpoint).param("inicio", "invalido").param("fim", "2024-02-29"))
				.andExpect(status().isBadRequest());
		mvc.perform(get(endpoint).param("inicio", "2024-02-01").param("fim", "2024-02-30"))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(service, consultaService);
		final var request = new BuscarGastosPorIntervaloUrlParams(
				LocalDate.of(2024, 2, 29), LocalDate.of(2024, 2, 29));
		when(consultaService.totalizarPorCategoria(eq(request), any())).thenReturn(Page.empty());
		when(consultaService.totalizarPorMetodoPagamento(eq(request), any())).thenReturn(Page.empty());

		mvc.perform(get(endpoint).param("inicio", "2024-02-29").param("fim", "2024-02-29"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.dados").isEmpty())
				.andExpect(jsonPath("$.paginacao.totalElementos").value(0));
	}

	@ParameterizedTest
	@ValueSource(strings = { "15-02-2026", "15/02/2026", "2026-02-15" })
	void criaGastoComDataAlternativaERetornaResposta(String data) throws Exception {
		when(service.criar(any())).thenAnswer(invocation -> {
			final var request = invocation.getArgument(0, CriarGastoBody.class);
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
				.andExpect(jsonPath("$.dados").value(aMapWithSize(8)))
				.andExpect(jsonPath("$.dados.descricao").value("compra"))
				.andExpect(jsonPath("$.dados.realizado").value(false))
				.andExpect(jsonPath("$.dados.id").value(id.toString()))
				.andExpect(jsonPath("$.dados.data").value("15/02/2026"))
				.andExpect(jsonPath("$.dados.nome").value("Mercado"))
				.andExpect(jsonPath("$.dados.valor").value(25.50))
				.andExpect(jsonPath("$.dados.categoria").value("Alimentacao"))
				.andExpect(jsonPath("$.dados.metodoPagamento").value(aMapWithSize(2)))
				.andExpect(jsonPath("$.dados.metodoPagamento.tipo").value("DEBITO"))
				.andExpect(jsonPath("$.dados.metodoPagamento.instituicaoFinanceira.nome").value("Caixa"));
		when(service.atualizar(eq(id), any())).thenReturn(gasto);
		mvc.perform(put("/gastos/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"data":"%s","nome":"Mercado","valor":25.50,"categoria":"Alimentacao",
				"metodoPagamentoId":"%s","realizado":false}
				""".formatted(data, metodoPagamentoId))).andExpect(status().isOk());
		final var captor = org.mockito.ArgumentCaptor.forClass(AtualizarGastoBody.class);
		verify(service).atualizar(eq(id), captor.capture());
		org.junit.jupiter.api.Assertions.assertEquals(LocalDate.of(2026, 2, 15), captor.getValue().data());
	}

	@Test
	void aceitaDataDeAnoBissexto() throws Exception {
		when(service.criar(any())).thenAnswer(invocation -> {
			final var request = invocation.getArgument(0, CriarGastoBody.class);
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
			final var request = invocation.getArgument(0, CriarGastoBody.class);
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
				.andExpect(jsonPath("$.dados").value(aMapWithSize(8)))
				.andExpect(jsonPath("$.dados.descricao").value(nullValue()))
				.andExpect(jsonPath("$.dados.realizado").value(false))
				.andExpect(jsonPath("$.dados.id").value(id.toString()))
				.andExpect(jsonPath("$.dados.data").value(nullValue()))
				.andExpect(jsonPath("$.dados.nome").value("Conta de luz"))
				.andExpect(jsonPath("$.dados.valor").value(80))
				.andExpect(jsonPath("$.dados.categoria").value("Casa"))
				.andExpect(jsonPath("$.dados.metodoPagamento").value(aMapWithSize(2)))
				.andExpect(jsonPath("$.dados.metodoPagamento.tipo").value("DEBITO"))
				.andExpect(jsonPath("$.dados.metodoPagamento.instituicaoFinanceira.nome").value("Caixa"));
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
	void consultaAtualizaEExcluiGasto() throws Exception {
		when(service.buscarPorId(id)).thenReturn(gasto);
		when(service.atualizar(eq(id), any())).thenAnswer(invocation -> {
			final var request = invocation.getArgument(1, AtualizarGastoBody.class);
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
				.andExpect(jsonPath("$.dados").value(aMapWithSize(8)))
				.andExpect(jsonPath("$.dados.descricao").value("atualizado"))
				.andExpect(jsonPath("$.dados.realizado").value(true))
				.andExpect(jsonPath("$.dados.id").value(id.toString()))
				.andExpect(jsonPath("$.dados.data").value("16/02/2026"))
				.andExpect(jsonPath("$.dados.nome").value("Novo"))
				.andExpect(jsonPath("$.dados.valor").value(0))
				.andExpect(jsonPath("$.dados.categoria").value("Casa"))
				.andExpect(jsonPath("$.dados.metodoPagamento").value(aMapWithSize(2)))
				.andExpect(jsonPath("$.dados.metodoPagamento.tipo").value("DEBITO"))
				.andExpect(jsonPath("$.dados.metodoPagamento.instituicaoFinanceira.nome").value("Caixa"));
		mvc.perform(put("/gastos/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"data":null,"nome":"Pendente","valor":0,"categoria":"Casa",
				"metodoPagamentoId":"%s","realizado":false}
				""".formatted(metodoPagamentoId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados").value(aMapWithSize(8)))
				.andExpect(jsonPath("$.dados.realizado").value(false))
				.andExpect(jsonPath("$.dados.id").value(id.toString()))
				.andExpect(jsonPath("$.dados.data").value(nullValue()))
				.andExpect(jsonPath("$.dados.nome").value("Pendente"));
		mvc.perform(delete("/gastos/{id}", id)).andExpect(status().isNoContent());
		verify(service).excluir(id);
	}
}
