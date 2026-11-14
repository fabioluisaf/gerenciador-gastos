package com.fabioluisaf.gerenciadorgastos.gastos.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem.FaturaResumidaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao.CriarFaturaRequest;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.comum.InformacoesFaturaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.service.FaturaService;
import com.fabioluisaf.gerenciadorgastos.shared.dto.ApiResponse;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;

import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/faturas")
@RequiredArgsConstructor
public class FaturaController {

	private final FaturaService service;
	private final ApiResponsePresenter presenter;

	@PostMapping
	public ResponseEntity<ApiResponse<InformacoesFaturaResponse>> criar(@Valid @RequestBody CriarFaturaRequest request) {
		final var fatura = service.criarFatura(request.metodoPagamentoId(), request.periodo());

		return ResponseEntity.status(HttpStatus.CREATED).body(presenter.apresentar(InformacoesFaturaResponse.from(fatura)));
	}

	@GetMapping("/cartao/{id}")
	public ApiResponse<List<FaturaResumidaResponse>> listarPorMetodoPagamento(
			@PathVariable("id") UUID metodoPagamentoId,
			@PageableDefault(sort = { "dataFechamento", "id" }) Pageable pageable
	) {
		final var faturas = service.listarPorMetodoPagamento(metodoPagamentoId, pageable);

		return presenter.apresentarPagina(faturas);
	}
}
