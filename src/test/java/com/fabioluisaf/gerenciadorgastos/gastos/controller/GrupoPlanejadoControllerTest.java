package com.fabioluisaf.gerenciadorgastos.gastos.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao.CriarGastoBody;
import com.fabioluisaf.gerenciadorgastos.gastos.model.GrupoPlanejado;
import com.fabioluisaf.gerenciadorgastos.gastos.model.ItemGrupoPlanejado;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.InstituicoesTeste;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.GrupoPlanejadoRepository;
import com.fabioluisaf.gerenciadorgastos.gastos.service.GastoService;
import com.fabioluisaf.gerenciadorgastos.gastos.service.GrupoPlanejadoService;
import com.fabioluisaf.gerenciadorgastos.shared.config.ApiProperties;
import com.fabioluisaf.gerenciadorgastos.shared.exception.GlobalExceptionHandler;
import com.fabioluisaf.gerenciadorgastos.shared.exception.NegocioExceptionHandler;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;

@SuppressWarnings("null")
class GrupoPlanejadoControllerTest {

	private static final String ITENS = """
			[{"nome":" Aluguel ","valor":1500.00,"categoria":"Casa","descricao":null},
			 {"nome":"Internet","valor":99.90,"categoria":" Casa ","descricao":"Fibra"}]
			""";

	private GrupoPlanejadoRepository repository;
	private GastoService gastoService;
	private MockMvc mvc;
	private final UUID id = UUID.randomUUID();

	@BeforeEach
	void configurar() {
		repository = mock(GrupoPlanejadoRepository.class);
		gastoService = mock(GastoService.class);
		when(repository.save(any(GrupoPlanejado.class))).thenAnswer(invocacao -> {
			final GrupoPlanejado grupo = invocacao.getArgument(0);
			grupo.setId(id);

			return grupo;
		});
		final var presenter = new ApiResponsePresenter(new ApiProperties("v1"));
		final var validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mvc = MockMvcBuilders
				.standaloneSetup(new GrupoPlanejadoController(new GrupoPlanejadoService(repository, gastoService), presenter))
				.setControllerAdvice(new GlobalExceptionHandler(presenter), new NegocioExceptionHandler(presenter))
				.setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
				.setValidator(validator)
				.build();
	}

	private GrupoPlanejado grupoExistente() {
		final var grupo = new GrupoPlanejado();
		grupo.setId(id);
		grupo.setNome("Contas fixas");
		grupo.substituirItens(List.of(new ItemGrupoPlanejado("Luz", new BigDecimal("120.00"), "Casa", null)));
		when(repository.findById(id)).thenReturn(Optional.of(grupo));

		return grupo;
	}

	@Test
	void criaGrupoComItensNaOrdemEValorTotal() throws Exception {
		mvc.perform(post("/grupos-planejados").contentType(MediaType.APPLICATION_JSON).content("""
				{"nome":" Contas fixas ","itens":%s}
				""".formatted(ITENS)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.dados.id").value(id.toString()))
				.andExpect(jsonPath("$.dados.nome").value("Contas fixas"))
				.andExpect(jsonPath("$.dados.valorTotal").value(1599.90))
				.andExpect(jsonPath("$.dados.itens[0].nome").value("Aluguel"))
				.andExpect(jsonPath("$.dados.itens[1].categoria").value("Casa"))
				.andExpect(jsonPath("$.dados.itens[1].descricao").value("Fibra"));
	}

	@Test
	void rejeitaGrupoSemNomeOuSemItensOuComItemInvalido() throws Exception {
		for (final var conteudo : List.of(
				"{\"nome\":\" \",\"itens\":" + ITENS + "}",
				"{\"nome\":\"Contas\",\"itens\":[]}",
				"{\"nome\":\"Contas\"}",
				"{\"nome\":\"Contas\",\"itens\":[{\"nome\":\"Luz\",\"categoria\":\"Casa\"}]}",
				"{\"nome\":\"Contas\",\"itens\":[{\"nome\":\"Luz\",\"valor\":1,\"categoria\":\"\"}]}")) {
			mvc.perform(post("/grupos-planejados").contentType(MediaType.APPLICATION_JSON).content(conteudo))
					.andExpect(status().isBadRequest());
		}
		verify(repository, never()).save(any());
	}

	@Test
	void listaBuscaAtualizaEExcluiGrupo() throws Exception {
		final var grupo = grupoExistente();
		when(repository.findAll(any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(grupo)));

		mvc.perform(get("/grupos-planejados"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados[0].nome").value("Contas fixas"))
				.andExpect(jsonPath("$.dados[0].valorTotal").value(120.00));
		mvc.perform(get("/grupos-planejados/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados.itens[0].nome").value("Luz"));
		mvc.perform(put("/grupos-planejados/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"nome":"Casa","itens":%s}
				""".formatted(ITENS)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados.nome").value("Casa"))
				.andExpect(jsonPath("$.dados.itens.length()").value(2));
		assertEquals(List.of("Aluguel", "Internet"), grupo.getItens().stream().map(ItemGrupoPlanejado::getNome).toList());

		mvc.perform(delete("/grupos-planejados/{id}", id)).andExpect(status().isNoContent());
		verify(repository).delete(grupo);
	}

	@Test
	void respondeNaoEncontradoParaGrupoInexistente() throws Exception {
		when(repository.findById(id)).thenReturn(Optional.empty());

		mvc.perform(get("/grupos-planejados/{id}", id)).andExpect(status().isNotFound());
		mvc.perform(post("/grupos-planejados/{id}/aplicacoes", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"metodoPagamentoId":"%s","data":"10/12/2026","itens":%s}
				""".formatted(UUID.randomUUID(), ITENS)))
				.andExpect(status().isNotFound());
		verify(gastoService, never()).criar(any());
	}

	@Test
	void aplicaGrupoNaFaturaCriandoUmGastoPlanejadoPorItem() throws Exception {
		grupoExistente();
		final var metodoId = UUID.randomUUID();
		final var metodo = MetodoPagamento.criar(TipoMetodoPagamento.CREDITO, 14, 21);
		metodo.setId(metodoId);
		metodo.setNome("Cartao");
		metodo.setInstituicaoFinanceira(InstituicoesTeste.nubank());
		when(gastoService.criar(any(CriarGastoBody.class))).thenAnswer(invocacao -> {
			final CriarGastoBody body = invocacao.getArgument(0);
			final var gasto = body.toEntity();
			gasto.setId(UUID.randomUUID());
			gasto.setMetodoPagamento(metodo);

			return gasto;
		});

		mvc.perform(post("/grupos-planejados/{id}/aplicacoes", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"metodoPagamentoId":"%s","faturaPeriodo":"12/2026","itens":%s}
				""".formatted(metodoId, ITENS)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.dados.length()").value(2))
				.andExpect(jsonPath("$.dados[0].nome").value("Aluguel"))
				.andExpect(jsonPath("$.dados[0].realizado").value(false))
				.andExpect(jsonPath("$.dados[1].valor").value(99.90));

		final var captor = ArgumentCaptor.forClass(CriarGastoBody.class);
		verify(gastoService, times(2)).criar(captor.capture());
		final var primeiro = captor.getAllValues().getFirst();
		assertEquals(metodoId, primeiro.metodoPagamentoId());
		assertEquals(YearMonth.of(2026, 12), primeiro.faturaPeriodo());
		assertNull(primeiro.data());
		assertFalse(primeiro.realizado());
		assertEquals("Casa", captor.getAllValues().get(1).categoria());
	}

	@Test
	void aplicaGrupoNumaData() throws Exception {
		grupoExistente();
		when(gastoService.criar(any(CriarGastoBody.class))).thenAnswer(invocacao -> {
			final CriarGastoBody body = invocacao.getArgument(0);
			final var gasto = body.toEntity();
			final var pix = MetodoPagamento.criar(TipoMetodoPagamento.PIX, null, null);
			pix.setInstituicaoFinanceira(InstituicoesTeste.caixa());
			gasto.setMetodoPagamento(pix);

			return gasto;
		});

		mvc.perform(post("/grupos-planejados/{id}/aplicacoes", id).contentType(MediaType.APPLICATION_JSON).content("""
				{"metodoPagamentoId":"%s","data":"05/01/2027","itens":%s}
				""".formatted(UUID.randomUUID(), ITENS)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.dados[0].data").value("05/01/2027"));

		final var captor = ArgumentCaptor.forClass(CriarGastoBody.class);
		verify(gastoService, times(2)).criar(captor.capture());
		assertEquals(LocalDate.of(2027, 1, 5), captor.getValue().data());
		assertNull(captor.getValue().faturaPeriodo());
	}

	@Test
	void rejeitaAplicacaoSemDestinoSemMetodoOuSemItens() throws Exception {
		grupoExistente();
		final var metodoId = UUID.randomUUID();
		for (final var conteudo : List.of(
				"{\"metodoPagamentoId\":\"" + metodoId + "\",\"itens\":" + ITENS + "}",
				"{\"data\":\"05/01/2027\",\"itens\":" + ITENS + "}",
				"{\"metodoPagamentoId\":\"" + metodoId + "\",\"data\":\"05/01/2027\",\"itens\":[]}")) {
			mvc.perform(post("/grupos-planejados/{id}/aplicacoes", id)
					.contentType(MediaType.APPLICATION_JSON).content(conteudo))
					.andExpect(status().isBadRequest());
		}
		verify(gastoService, never()).criar(any());
	}

}
