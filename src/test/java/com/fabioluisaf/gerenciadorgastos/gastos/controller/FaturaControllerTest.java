package com.fabioluisaf.gerenciadorgastos.gastos.controller;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.fatura.FaturaResumidaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.FaturaRepository;
import com.fabioluisaf.gerenciadorgastos.gastos.service.FaturaService;
import com.fabioluisaf.gerenciadorgastos.gastos.service.MetodoPagamentoService;
import com.fabioluisaf.gerenciadorgastos.shared.config.ApiProperties;
import com.fabioluisaf.gerenciadorgastos.shared.exception.GlobalExceptionHandler;
import com.fabioluisaf.gerenciadorgastos.shared.exception.NegocioExceptionHandler;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;

import jakarta.persistence.EntityNotFoundException;

class FaturaControllerTest {

	private FaturaRepository repository;
	private MetodoPagamentoService metodoService;
	private MockMvc mvc;
	private UUID metodoId;

	@BeforeEach
	void configurar() {
		repository = mock(FaturaRepository.class);
		metodoId = UUID.randomUUID();
		metodoService = mock(MetodoPagamentoService.class);
		final var service = new FaturaService(repository, metodoService);
		final var presenter = new ApiResponsePresenter(new ApiProperties("v1"));
		final var validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mvc = MockMvcBuilders.standaloneSetup(new FaturaController(service, presenter))
				.setControllerAdvice(new GlobalExceptionHandler(presenter), new NegocioExceptionHandler(presenter))
				.setValidator(validator)
				.setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
				.build();
	}

	@Test
	void listaResumosComPaginacao() throws Exception {
		final var id = UUID.randomUUID();
		final var pageable = PageRequest.of(1, 2, Sort.by("dataFechamento", "id"));
		when(repository.listarResumosPorMetodoPagamentoId(metodoId, pageable)).thenReturn(new PageImpl<>(List.of(
				new FaturaResumidaResponse(id, LocalDate.of(2026, 11, 10), new BigDecimal("125.50")),
				new FaturaResumidaResponse(UUID.randomUUID(), LocalDate.of(2026, 12, 10), BigDecimal.ZERO)
		), pageable, 5));

		mvc.perform(get("/faturas/cartao/{id}", metodoId).param("page", "1").param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados.length()").value(2))
				.andExpect(jsonPath("$.dados[0]").value(aMapWithSize(3)))
				.andExpect(jsonPath("$.dados[0].id").value(id.toString()))
				.andExpect(jsonPath("$.dados[0].dataFechamento").value("2026-11-10"))
				.andExpect(jsonPath("$.dados[0].valorTotal").value(125.50))
				.andExpect(jsonPath("$.dados[1].valorTotal").value(0))
				.andExpect(jsonPath("$.versao").value("v1"))
				.andExpect(jsonPath("$.paginacao.pagina").value(1))
				.andExpect(jsonPath("$.paginacao.tamanho").value(2))
				.andExpect(jsonPath("$.paginacao.totalElementos").value(5))
				.andExpect(jsonPath("$.paginacao.totalPaginas").value(3));
		verify(metodoService).buscarPorId(metodoId);
		verify(repository).listarResumosPorMetodoPagamentoId(metodoId, pageable);
	}

	@Test
	void listaVaziaQuandoMetodoNaoTemFaturas() throws Exception {
		final var pageable = PageRequest.of(0, 10, Sort.by("dataFechamento", "id"));
		when(repository.listarResumosPorMetodoPagamentoId(metodoId, pageable)).thenReturn(Page.empty(pageable));
		mvc.perform(get("/faturas/cartao/{id}", metodoId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dados").isArray())
				.andExpect(jsonPath("$.dados").isEmpty())
				.andExpect(jsonPath("$.paginacao.pagina").value(0))
				.andExpect(jsonPath("$.paginacao.tamanho").value(10))
				.andExpect(jsonPath("$.paginacao.totalElementos").value(0))
				.andExpect(jsonPath("$.paginacao.totalPaginas").value(0));
		verify(metodoService).buscarPorId(metodoId);
	}

	@Test
	void rejeitaMetodoInexistenteOuIdentificadorInvalido() throws Exception {
		when(metodoService.buscarPorId(metodoId)).thenThrow(new EntityNotFoundException());

		mvc.perform(get("/faturas/cartao/{id}", metodoId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.dados.mensagem").value("RECURSO_NAO_ENCONTRADO"));
		mvc.perform(get("/faturas/cartao/invalid"))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(repository);
	}

	@Test
	void consultaUnicaNaoEstaMaisDisponivel() throws Exception {
		mvc.perform(get("/faturas").param("metodoPagamentoId", metodoId.toString())
				.param("mes", "12").param("ano", "2026"))
				.andExpect(status().isNotFound());

		verifyNoInteractions(repository, metodoService);
	}
}
