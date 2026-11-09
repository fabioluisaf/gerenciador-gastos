package com.fabioluisaf.gerenciadorgastos.gastos.model.entity;

import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.gastos.model.InstituicaoFinanceira;
import com.fabioluisaf.gerenciadorgastos.gastos.model.TipoMetodoPagamento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "metodos_pagamento")
@Getter
@Setter
@NoArgsConstructor
public class MetodoPagamento {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private TipoMetodoPagamento tipo;

	@Column(name = "instituicao_financeira", nullable = false)
	@Enumerated(EnumType.STRING)
	private InstituicaoFinanceira instituicaoFinanceira;

	@Column(nullable = false)
	private String nome;

	@Column(name = "dia_fechamento")
	private Integer diaFechamento;

	@Override
	public boolean equals(Object objeto) {
		if (this == objeto) {
			return true;
		}
		if (!(objeto instanceof MetodoPagamento outro)) {
			return false;
		}
		final var identificador = getId();

		return identificador != null && identificador.equals(outro.getId());
	}

	@Override
	public int hashCode() {
		return MetodoPagamento.class.hashCode();
	}
}
