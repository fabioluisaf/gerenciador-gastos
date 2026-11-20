package com.fabioluisaf.gerenciadorgastos.gastos.controller;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.atualizacao.AtualizarInstituicaoFinanceiraBody;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.criacao.CriarInstituicaoFinanceiraBody;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.comum.InstituicaoFinanceiraResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.service.InstituicaoFinanceiraService;
import com.fabioluisaf.gerenciadorgastos.shared.dto.ApiResponse;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/instituicoes-financeiras")
@RequiredArgsConstructor
public class InstituicaoFinanceiraController {

	private final InstituicaoFinanceiraService service;
	private final ApiResponsePresenter presenter;

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<InstituicaoFinanceiraResponse>> criar(
			@Valid @ModelAttribute CriarInstituicaoFinanceiraBody request) {

		final var instituicao = service.criar(request);
		final var localizacao = ServletUriComponentsBuilder.fromCurrentRequestUri()
				.path("/{id}")
				.buildAndExpand(instituicao.getId())
				.toUri();

		return ResponseEntity.created(localizacao)
				.body(presenter.apresentar(InstituicaoFinanceiraResponse.from(instituicao)));
	}

	@GetMapping
	public ApiResponse<List<InstituicaoFinanceiraResponse>> listar(
			@PageableDefault(size = 100, sort = "nome") Pageable pageable) {

		return presenter.apresentarPagina(service.listar(pageable).map(InstituicaoFinanceiraResponse::from));
	}

	@GetMapping("/{id}/icone")
	public ResponseEntity<byte[]> buscarIcone(@PathVariable UUID id, WebRequest request) {
		final var instituicao = service.buscarPorId(id);
		if (!instituicao.possuiIcone()) {
			throw new EntityNotFoundException("Instituição financeira sem ícone: " + id);
		}

		final var etag = calcularEtag(instituicao.getIcone());
		if (request.checkNotModified(etag)) {
			return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
		}

		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(instituicao.getIconeTipo()))
				.cacheControl(CacheControl.noCache())
				.eTag(etag)
				.header("X-Content-Type-Options", "nosniff")
				.body(instituicao.getIcone());
	}

	private static String calcularEtag(byte[] conteudo) {
		try {
			final var hash = MessageDigest.getInstance("SHA-256").digest(conteudo);
			return "\"" + HexFormat.of().formatHex(hash) + "\"";
		} catch (NoSuchAlgorithmException excecao) {
			throw new IllegalStateException("SHA-256 indisponível", excecao);
		}
	}

	@PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<InstituicaoFinanceiraResponse> atualizar(
			@PathVariable UUID id, @Valid @ModelAttribute AtualizarInstituicaoFinanceiraBody request) {

		return presenter.apresentar(InstituicaoFinanceiraResponse.from(service.atualizar(id, request)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> excluir(@PathVariable UUID id) {
		service.excluir(id);
		return ResponseEntity.noContent().build();
	}
}
