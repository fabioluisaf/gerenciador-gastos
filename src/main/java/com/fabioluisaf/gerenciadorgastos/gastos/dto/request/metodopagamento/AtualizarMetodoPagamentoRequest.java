package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.metodopagamento;

import com.fabioluisaf.gerenciadorgastos.gastos.model.InstituicaoFinanceira;
import com.fabioluisaf.gerenciadorgastos.gastos.model.TipoMetodoPagamento;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AtualizarMetodoPagamentoRequest(
		@NotNull TipoMetodoPagamento tipo,
		@NotNull InstituicaoFinanceira instituicaoFinanceira,
		@NotBlank String nome,
		@Min(1) @Max(31) Integer diaFechamento
) {
}
