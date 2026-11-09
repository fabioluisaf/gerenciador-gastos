package com.fabioluisaf.gerenciadorgastos.shared.exception;

import java.time.Instant;
import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fabioluisaf.gerenciadorgastos.gastos.exception.GastoRealizadoSemDataException;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.MetodoPagamentoEmUsoException;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.PagamentoInvalidoException;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.ParcelaOperacaoInvalidaException;
import com.fabioluisaf.gerenciadorgastos.gastos.exception.ValoresParcelasInvalidosException;
import com.fabioluisaf.gerenciadorgastos.shared.dto.ApiResponse;
import com.fabioluisaf.gerenciadorgastos.shared.dto.ErroResponse;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class NegocioExceptionHandler {

	private final ApiResponsePresenter presenter;

	@ExceptionHandler(ParcelaOperacaoInvalidaException.class)
	public ResponseEntity<ApiResponse<ErroResponse>> tratarOperacaoEmParcela(
			ParcelaOperacaoInvalidaException excecao, HttpServletRequest request
	) {
		return responder(HttpStatus.CONFLICT, excecao.getMessage(), request);
	}

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<ApiResponse<ErroResponse>> tratarNaoEncontrado(
			EntityNotFoundException excecao, HttpServletRequest request
	) {
		return responder(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO", request);
	}

	@ExceptionHandler(GastoRealizadoSemDataException.class)
	public ResponseEntity<ApiResponse<ErroResponse>> tratarGastoRealizadoSemData(
			GastoRealizadoSemDataException excecao, HttpServletRequest request
	) {
		return responder(HttpStatus.BAD_REQUEST, "DATA_OBRIGATORIA_PARA_GASTO_REALIZADO", request);
	}

	@ExceptionHandler({PagamentoInvalidoException.class, ValoresParcelasInvalidosException.class})
	public ResponseEntity<ApiResponse<ErroResponse>> tratarRegraInvalida(
			RuntimeException excecao, HttpServletRequest request
	) {
		return responder(HttpStatus.BAD_REQUEST, excecao.getMessage(), request);
	}

	@ExceptionHandler(MetodoPagamentoEmUsoException.class)
	public ResponseEntity<ApiResponse<ErroResponse>> tratarMetodoPagamentoEmUso(
			MetodoPagamentoEmUsoException excecao, HttpServletRequest request
	) {
		return responder(HttpStatus.CONFLICT, "METODO_PAGAMENTO_EM_USO", request);
	}

	private ResponseEntity<ApiResponse<ErroResponse>> responder(
			HttpStatus status, String mensagem, HttpServletRequest request
	) {
		final var erro = new ErroResponse(
				status.value(), mensagem, request.getRequestURI(), Instant.now(), List.of()
		);

		return ResponseEntity.status(status).body(presenter.apresentar(erro));
	}
}
