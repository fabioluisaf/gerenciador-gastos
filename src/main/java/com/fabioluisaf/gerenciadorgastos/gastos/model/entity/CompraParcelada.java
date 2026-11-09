package com.fabioluisaf.gerenciadorgastos.gastos.model.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "compras_parceladas")
@Getter
@Setter
@NoArgsConstructor
public class CompraParcelada {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "valor_total", nullable = false, precision = 19, scale = 2)
	private BigDecimal valorTotal;

	@OneToMany(mappedBy = "compraParcelada", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Gasto> parcelas = new ArrayList<>();

	@Override
	public boolean equals(Object objeto) {
		if (this == objeto) {
			return true;
		}
		if (!(objeto instanceof CompraParcelada outra)) {
			return false;
		}
		final var identificador = getId();
		return identificador != null && identificador.equals(outra.getId());
	}

	@Override
	public int hashCode() {
		return CompraParcelada.class.hashCode();
	}
}
