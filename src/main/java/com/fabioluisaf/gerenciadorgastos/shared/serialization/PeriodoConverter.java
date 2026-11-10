package com.fabioluisaf.gerenciadorgastos.shared.serialization;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.fabioluisaf.gerenciadorgastos.shared.constantes.FormatoPeriodo;

@Component
public class PeriodoConverter implements Converter<String, YearMonth> {

	private static final List<DateTimeFormatter> FORMATOS = Arrays.stream(FormatoPeriodo.values())
			.filter(formato -> !formato.getFormato().contains("/"))
			.map(formato -> DateTimeFormatter.ofPattern(formato.getFormato()).withResolverStyle(ResolverStyle.STRICT))
			.toList();

	@Override
	public YearMonth convert(String valor) {
		for (final var formato : FORMATOS) {
			try {
				return YearMonth.parse(valor, formato);
			} catch (DateTimeParseException _) {
				// Try the next URL format before reporting invalid input.
			}
		}
		throw new IllegalArgumentException("PERIODO_INVALIDO");
	}
}
