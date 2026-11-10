package com.fabioluisaf.gerenciadorgastos.gastos.dto.request.metodopagamento;

import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.InstituicaoFinanceira;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriarMetodoPagamentoRequest(
		@NotNull TipoMetodoPagamento tipo,
		@NotNull InstituicaoFinanceira instituicaoFinanceira,
		@NotBlank String nome,
		@Min(1) @Max(31) Integer diaFechamento
) {
}
