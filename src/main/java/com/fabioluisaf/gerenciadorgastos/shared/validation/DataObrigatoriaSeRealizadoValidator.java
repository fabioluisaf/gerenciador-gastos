package com.fabioluisaf.gerenciadorgastos.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DataObrigatoriaSeRealizadoValidator
		implements ConstraintValidator<DataObrigatoriaSeRealizado, GastoComDataERealizacao> {

	@Override
	public boolean isValid(GastoComDataERealizacao gasto, ConstraintValidatorContext context) {
		if (gasto == null || !Boolean.TRUE.equals(gasto.realizado()) || gasto.data() != null) {
			return true;
		}

		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
				.addPropertyNode("data")
				.addConstraintViolation();

		return false;
	}
}
