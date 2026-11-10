package com.fabioluisaf.gerenciadorgastos.gastos.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.Fatura;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "gastos")
@Getter
@Setter
@NoArgsConstructor
public class Gasto {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column
	private LocalDate data;

	@Column(nullable = false)
	private String nome;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal valor;

	@Column(nullable = false)
	private String categoria;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "metodo_pagamento_id", nullable = false)
	private MetodoPagamento metodoPagamento;

	@Column(name = "tipo_metodo_pagamento", nullable = false)
	@Enumerated(EnumType.STRING)
	private TipoMetodoPagamento tipoMetodoPagamento;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fatura_id")
	private Fatura fatura;

	@Column(columnDefinition = "text")
	private String descricao;

	@Column(nullable = false)
	private boolean realizado;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "compra_parcelada_id")
	private CompraParcelada compraParcelada;

	@Column(name = "numero_parcela")
	private Integer numeroParcela;

	@PrePersist
	@PreUpdate
	void definirTipoMetodoPagamento() {
		tipoMetodoPagamento = metodoPagamento.getTipo();
	}

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
