package com.fabioluisaf.gerenciadorgastos.gastos.validation;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.request.consulta.TotalizarGastosRequest;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EscopoTotalizacaoValidoValidator
		implements ConstraintValidator<EscopoTotalizacaoValido, TotalizarGastosRequest> {

	@Override
	public boolean isValid(TotalizarGastosRequest request, ConstraintValidatorContext context) {
		if (request == null) {
			return true;
		}
		if (request.faturaId() != null) {
			return request.mes() == null && request.ano() == null;
		}

		return request.mes() != null && request.ano() != null;
	}
}
