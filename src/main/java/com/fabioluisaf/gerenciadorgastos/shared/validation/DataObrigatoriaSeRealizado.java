package com.fabioluisaf.gerenciadorgastos.shared.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = DataObrigatoriaSeRealizadoValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataObrigatoriaSeRealizado {

	String message() default "DATA_OBRIGATORIA_PARA_GASTO_REALIZADO";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
