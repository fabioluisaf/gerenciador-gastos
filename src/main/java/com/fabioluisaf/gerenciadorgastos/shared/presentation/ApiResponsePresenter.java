package com.fabioluisaf.gerenciadorgastos.shared.presentation;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.fabioluisaf.gerenciadorgastos.shared.config.ApiProperties;
import com.fabioluisaf.gerenciadorgastos.shared.dto.ApiResponse;
import com.fabioluisaf.gerenciadorgastos.shared.dto.DadosPage;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApiResponsePresenter {

	private final ApiProperties apiProperties;

	public <T> ApiResponse<T> apresentar(T dados) {
		return new ApiResponse<>(dados, apiProperties.versao(), null);
	}

	public <T> ApiResponse<List<T>> apresentarPagina(Page<T> pagina) {
		final var paginacao = new DadosPage(
            pagina.getNumber(),
            pagina.getSize(),
            pagina.getTotalElements(),
            pagina.getTotalPages()
        );

		return new ApiResponse<>(pagina.getContent(), apiProperties.versao(), paginacao);
	}
}
