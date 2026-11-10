package com.fabioluisaf.gerenciadorgastos.gastos.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem.FaturaResumidaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.service.FaturaService;
import com.fabioluisaf.gerenciadorgastos.shared.dto.ApiResponse;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/faturas")
@RequiredArgsConstructor
public class FaturaController {

	private final FaturaService service;
	private final ApiResponsePresenter presenter;

	@GetMapping("/cartao/{id}")
	public ApiResponse<List<FaturaResumidaResponse>> listarPorMetodoPagamento(
			@PathVariable("id") UUID metodoPagamentoId,
			@PageableDefault(sort = { "dataFechamento", "id" }) Pageable pageable
	) {
		final var faturas = service.listarPorMetodoPagamento(metodoPagamentoId, pageable);

		return presenter.apresentarPagina(faturas);
	}
}
