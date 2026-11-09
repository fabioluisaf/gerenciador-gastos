package com.fabioluisaf.gerenciadorgastos.gastos.dto.response.fatura;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.gasto.GastoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.metodopagamento.MetodoPagamentoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.Fatura;

public record FaturaResponse(
		UUID id,
		LocalDate dataFechamento,
		MetodoPagamentoResponse metodoPagamento,
		List<GastoResponse> gastos
) {

	public static FaturaResponse from(Fatura fatura) {
		final var gastos = fatura.getGastos().stream()
				.map(GastoResponse::from)
				.toList();

		return new FaturaResponse(
				fatura.getId(),
				fatura.getDataFechamento(),
				MetodoPagamentoResponse.from(fatura.getMetodoPagamento()),
				gastos
		);
	}
}
