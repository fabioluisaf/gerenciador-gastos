package com.fabioluisaf.gerenciadorgastos.shared.serialization;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.fabioluisaf.gerenciadorgastos.shared.constantes.FormatoData;

@Component
public class DataConverter implements Converter<String, LocalDate> {

	private static final List<DateTimeFormatter> FORMATOS = Arrays.stream(FormatoData.values())
			.map(formato -> DateTimeFormatter.ofPattern(formato.getFormato()).withResolverStyle(ResolverStyle.STRICT))
			.toList();

	@Override
	public LocalDate convert(String valor) {
		for (final var formato : FORMATOS) {
			try {
				return LocalDate.parse(valor, formato);
			} catch (DateTimeParseException _) {
				// Try the next supported format before reporting invalid input.
			}
		}
		throw new IllegalArgumentException("DATA_INVALIDA");
	}
}
