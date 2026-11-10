package com.fabioluisaf.gerenciadorgastos.shared.validation;

import java.time.YearMonth;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AnoMinimoValidator implements ConstraintValidator<AnoMinimo, YearMonth> {

	private int anoMinimo;

	@Override
	public void initialize(AnoMinimo constraint) {
		anoMinimo = constraint.value();
	}

	@Override
	public boolean isValid(YearMonth periodo, ConstraintValidatorContext context) {
		return periodo == null || periodo.getYear() >= anoMinimo;
	}
}
