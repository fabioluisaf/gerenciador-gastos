package com.fabioluisaf.gerenciadorgastos.shared.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.fabioluisaf.gerenciadorgastos.shared.dto.ApiResponse;
import com.fabioluisaf.gerenciadorgastos.shared.dto.ErroResponse;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private final ApiResponsePresenter presenter;

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<ApiResponse<ErroResponse>> tratarNaoEncontrado(
			EntityNotFoundException excecao, HttpServletRequest request) {
		final var status = HttpStatus.NOT_FOUND;
		return ResponseEntity.status(status)
				.body(criarResposta(status, "Recurso não encontrado.", request.getRequestURI()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<ErroResponse>> tratarErroInesperado(
			Exception excecao, HttpServletRequest request) {
		log.error("Erro inesperado ao processar requisição", excecao);
		final var status = HttpStatus.INTERNAL_SERVER_ERROR;
		return ResponseEntity.status(status)
				.body(criarResposta(status, "Erro interno do servidor.", request.getRequestURI()));
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception excecao, Object body, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		final var caminho = ((ServletWebRequest) request).getRequest().getRequestURI();
		String mensagem;
		if (status.is5xxServerError()) {
			log.error("Erro ao processar requisição", excecao);
			mensagem = "Erro interno do servidor.";
		} else if (excecao instanceof MethodArgumentNotValidException validacao) {
			mensagem = validacao.getBindingResult().getAllErrors().stream()
					.map(erro -> {
						if (erro instanceof org.springframework.validation.FieldError campo) {
							return campo.getField() + ": " + campo.getDefaultMessage();
						}
						return erro.getDefaultMessage();
					})
					.collect(Collectors.joining("; "));
		} else {
			mensagem = switch (status.value()) {
				case 400 -> "Requisição inválida.";
				case 404 -> "Recurso não encontrado.";
				case 405 -> "Método HTTP não permitido.";
				case 406 -> "Formato de resposta não aceito.";
				case 415 -> "Tipo de conteúdo não suportado.";
				default -> "Não foi possível processar a requisição.";
			};
		}
		return super.handleExceptionInternal(excecao, criarResposta(status, mensagem, caminho), headers, status, request);
	}

	private ApiResponse<ErroResponse> criarResposta(HttpStatusCode status, String mensagem, String caminho) {
		final var erro = new ErroResponse(status.value(), mensagem, caminho, Instant.now());
		return presenter.apresentar(erro);
	}
}
