package com.fabioluisaf.gerenciadorgastos.gastos.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "grupos_planejados")
@Getter
@Setter
@NoArgsConstructor
public class GrupoPlanejado {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String nome;

	@ElementCollection
	@CollectionTable(name = "itens_grupos_planejados", joinColumns = @JoinColumn(name = "grupo_planejado_id"))
	@OrderColumn(name = "posicao")
	@BatchSize(size = 100)
	private List<ItemGrupoPlanejado> itens = new ArrayList<>();

	public void substituirItens(List<ItemGrupoPlanejado> novosItens) {
		itens.clear();
		itens.addAll(novosItens);
	}

	public BigDecimal getValorTotal() {
		var total = BigDecimal.ZERO;

		for (final var item : itens) {
			total = total.add(Objects.requireNonNull(item.getValor()));
		}

		return total;
	}

	@Override
	public boolean equals(Object objeto) {
		if (this == objeto) {
			return true;
		}
		if (!(objeto instanceof GrupoPlanejado outro)) {
			return false;
		}
		final var identificador = getId();

		return identificador != null && identificador.equals(outro.getId());
	}

	@Override
	public int hashCode() {
		return GrupoPlanejado.class.hashCode();
	}
}
