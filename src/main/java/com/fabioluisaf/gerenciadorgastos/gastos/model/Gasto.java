package com.fabioluisaf.gerenciadorgastos.gastos.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface Gasto {

	LocalDate getData();

	String getNome();

	BigDecimal getValor();

	String getCategoria();

	MetodoPagamento getMetodoPagamento();

	String getDescricao();
}
