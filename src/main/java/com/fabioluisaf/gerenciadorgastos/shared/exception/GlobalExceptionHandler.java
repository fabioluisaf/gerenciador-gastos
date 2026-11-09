package com.fabioluisaf.gerenciadorgastos.shared.exception;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.fabioluisaf.gerenciadorgastos.shared.dto.ApiResponse;
import com.fabioluisaf.gerenciadorgastos.shared.dto.ErroResponse;
import com.fabioluisaf.gerenciadorgastos.shared.dto.ErroValidacao;
import com.fabioluisaf.gerenciadorgastos.shared.presentation.ApiResponsePresenter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.exc.MismatchedInputException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final String ERRO_DE_VALIDACAO = "ERRO_DE_VALIDACAO";

	private final ApiResponsePresenter presenter;

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<ErroResponse>> tratarErroInesperado(
        Exception excecao, HttpServletRequest request
    ) {
		log.error("Erro inesperado ao processar requisição", excecao);
		final var status = HttpStatus.INTERNAL_SERVER_ERROR;

		return ResponseEntity.status(status)
				.body(criarResposta(status, "ERRO_INTERNO_DO_SERVIDOR", request.getRequestURI()));
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException excecao, HttpHeaders headers,
        HttpStatusCode status, WebRequest request
    ) {
		final var erros = excecao.getBindingResult().getAllErrors().stream()
				.map(erro -> new ErroValidacao(
						erro instanceof FieldError campo ? campo.getField() : null,
						traduzirCodigoValidacao(erro.getCode())
				))
				.toList();

		return responderErroValidacao(excecao, erros, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
        HttpMessageNotReadableException excecao, HttpHeaders headers,
        HttpStatusCode status, WebRequest request
    ) {
		final var erroData = buscarErroFormatoData(excecao);
		if (erroData == null) {
			return super.handleHttpMessageNotReadable(excecao, headers, status, request);
		}

		final var caminho = erroData.getPath();
		final var campo = caminho.isEmpty() ? null : caminho.get(caminho.size() - 1).getPropertyName();

		return responderErroValidacao(
				excecao, List.of(new ErroValidacao(campo, "FORMATO_INVALIDO")), headers, status, request
		);
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(
        Exception excecao, Object body, HttpHeaders headers,
        HttpStatusCode status, WebRequest request
    ) {
		String mensagem;

		if (status.is5xxServerError()) {
			log.error("Erro ao processar requisição", excecao);
			mensagem = "ERRO_INTERNO_DO_SERVIDOR";
		} else {
			mensagem = switch (status.value()) {
				case 400 -> "REQUISICAO_INVALIDA";
				case 404 -> "RECURSO_NAO_ENCONTRADO";
				case 405 -> "METODO_HTTP_NAO_PERMITIDO";
				case 406 -> "FORMATO_DE_RESPOSTA_NAO_ACEITO";
				case 415 -> "TIPO_DE_CONTEUDO_NAO_SUPORTADO";
				default -> "NAO_FOI_POSSIVEL_PROCESSAR_A_REQUISICAO";
			};
		}

		return super.handleExceptionInternal(
				excecao, criarResposta(status, mensagem, obterCaminho(request)), headers, status, request
		);
	}

	private ResponseEntity<Object> responderErroValidacao(
			Exception excecao, List<ErroValidacao> erros, HttpHeaders headers,
			HttpStatusCode status, WebRequest request
	) {
		final var resposta = criarResposta(status, ERRO_DE_VALIDACAO, obterCaminho(request), erros);

		return super.handleExceptionInternal(excecao, resposta, headers, status, request);
	}

	private String traduzirCodigoValidacao(String codigo) {
		if (codigo == null) {
			return ERRO_DE_VALIDACAO;
		}
		return switch (codigo) {
			case "NotBlank" -> "CAMPO_NAO_PODE_FICAR_VAZIO";
			case "NotNull" -> "CAMPO_OBRIGATORIO";
			case "Positive" -> "DEVE_SER_POSITIVO";
			case "PositiveOrZero" -> "DEVE_SER_ZERO_OU_POSITIVO";
			case "Min" -> "ABAIXO_DO_MINIMO";
			case "Max" -> "ACIMA_DO_MAXIMO";
			case "Digits" -> "FORMATO_NUMERICO_INVALIDO";
			case "DataObrigatoriaSeRealizado" -> "DATA_OBRIGATORIA_PARA_GASTO_REALIZADO";
			case "EscopoTotalizacaoValido" -> "INFORME_MES_E_ANO_OU_FATURA";
			default -> ERRO_DE_VALIDACAO;
		};
	}

	private String obterCaminho(WebRequest request) {
		return ((ServletWebRequest) request).getRequest().getRequestURI();
	}

	private MismatchedInputException buscarErroFormatoData(Throwable excecao) {
		for (Throwable causa = excecao; causa != null; causa = causa.getCause()) {
			if (causa instanceof MismatchedInputException erro && LocalDate.class.equals(erro.getTargetType())) {
				return erro;
			}
		}

		return null;
	}

	private ApiResponse<ErroResponse> criarResposta(HttpStatusCode status, String mensagem, String caminho) {
		return criarResposta(status, mensagem, caminho, List.of());
	}

	private ApiResponse<ErroResponse> criarResposta(
			HttpStatusCode status, String mensagem, String caminho, List<ErroValidacao> errosValidacao
	) {
		final var erro = new ErroResponse(status.value(), mensagem, caminho, Instant.now(), errosValidacao);

		return presenter.apresentar(erro);
	}
}
