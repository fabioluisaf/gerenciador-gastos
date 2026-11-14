package com.fabioluisaf.gerenciadorgastos.shared.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ErroResponse(
    int status,
    String mensagem,
    String caminho,
    Instant instante,

    @JsonInclude(JsonInclude.Include.NON_NULL)
    UUID recursoId,

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<ErroValidacao> errosValidacao
) {

	public ErroResponse(
			int status, String mensagem, String caminho, Instant instante, List<ErroValidacao> errosValidacao
	) {
		this(status, mensagem, caminho, instante, null, errosValidacao);
	}
}
