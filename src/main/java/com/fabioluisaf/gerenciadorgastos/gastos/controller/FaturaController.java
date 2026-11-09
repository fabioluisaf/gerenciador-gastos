package com.fabioluisaf.gerenciadorgastos.gastos.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.fatura.BuscarFaturasRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.fatura.FaturaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.fatura.FaturaResumidaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.service.FaturaService;
import com.fabioluisaf.gerenciadorgastos.shared.dto.ApiResponse;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/faturas")
@RequiredArgsConstructor
@Validated
public class FaturaController {

	private final FaturaService service;
	private final ApiResponsePresenter presenter;

	@GetMapping("/metodo-pagamento/{metodoPagamentoId}")
	public ApiResponse<List<FaturaResumidaResponse>> listarPorMetodoPagamento(
			@PathVariable UUID metodoPagamentoId,
			@PageableDefault(sort = { "dataFechamento", "id" }) Pageable pageable
	) {
		final var faturas = service.listarPorMetodoPagamento(metodoPagamentoId, pageable);

		return presenter.apresentarPagina(faturas);
	}

	@GetMapping
	public ApiResponse<FaturaResponse> buscar(
			@Valid @ModelAttribute BuscarFaturasRequest request
	) {
		final var fatura = service.buscar(request);

		return presenter.apresentar(FaturaResponse.from(fatura));
	}
}
