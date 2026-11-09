package com.fabioluisaf.gerenciadorgastos.gastos.model.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.gastos.model.TipoMetodoPagamento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "faturas")
@Getter
@Setter
@NoArgsConstructor
public class Fatura {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "metodo_pagamento_id", nullable = false)
	private MetodoPagamento metodoPagamento;

	@Column(name = "data_fechamento", nullable = false)
	private LocalDate dataFechamento;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_metodo_pagamento", nullable = false)
	private TipoMetodoPagamento tipoMetodoPagamento;

	@OneToMany(mappedBy = "fatura")
	private List<Gasto> gastos = new ArrayList<>();

	@PrePersist
	void definirTipoMetodoPagamento() {
		tipoMetodoPagamento = TipoMetodoPagamento.CREDITO;
	}

	@Override
	public boolean equals(Object objeto) {
		if (this == objeto) {
			return true;
		}
		if (!(objeto instanceof Fatura outra)) {
			return false;
		}
		final var identificador = getId();

		return identificador != null && identificador.equals(outra.getId());
	}

	@Override
	public int hashCode() {
		return Fatura.class.hashCode();
	}
}
