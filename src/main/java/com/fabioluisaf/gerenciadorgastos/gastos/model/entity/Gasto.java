package com.fabioluisaf.gerenciadorgastos.gastos.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.gastos.model.MetodoPagamento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "gastos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Gasto {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private LocalDate data;

	@Column(nullable = false)
	private String nome;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal valor;

	@Column(nullable = false)
	private String categoria;

	@Column(name = "metodo_pagamento", nullable = false)
	@Enumerated(EnumType.STRING)
	private MetodoPagamento metodoPagamento;

	@Column(columnDefinition = "text")
	private String descricao;

	@Column(nullable = false)
	private boolean realizado;

	@Override
	public boolean equals(Object objeto) {
		if (this == objeto) {
			return true;
		}
		if (!(objeto instanceof Gasto outro)) {
			return false;
		}
		final var identificador = getId();
		return identificador != null && identificador.equals(outro.getId());
	}

	@Override
	public int hashCode() {
		return Gasto.class.hashCode();
	}
}
