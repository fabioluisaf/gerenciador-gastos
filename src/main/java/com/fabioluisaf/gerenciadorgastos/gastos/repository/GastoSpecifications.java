package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.fabioluisaf.gerenciadorgastos.gastos.model.Gasto;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.Fatura;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public final class GastoSpecifications {

	private GastoSpecifications() {
	}

	public static Specification<Gasto> listagem(boolean realizado, FiltroGastos filtro) {
		return (root, query, cb) -> {
			final var fatura = root.<Gasto, Fatura>join("fatura", JoinType.LEFT);
			final var predicados = new ArrayList<Predicate>();
			predicados.add(cb.equal(root.get("realizado"), realizado));
			predicados.addAll(porMetodosECategorias(root, cb, filtro));

			final Expression<LocalDate> dataReferencia = cb.<LocalDate>selectCase()
					.when(cb.equal(root.get("tipoMetodoPagamento"), TipoMetodoPagamento.CREDITO),
							fatura.<LocalDate>get("dataVencimento"))
					.otherwise(root.<LocalDate>get("data"));
			if (filtro.inicio() != null) {
				predicados.add(cb.greaterThanOrEqualTo(dataReferencia, filtro.inicio()));
			}
			if (filtro.fim() != null) {
				predicados.add(cb.lessThanOrEqualTo(dataReferencia, filtro.fim()));
			}

			if (filtro.faturaId() != null) {
				predicados.add(cb.equal(fatura.get("id"), filtro.faturaId()));
			} else {
				predicados.add(umaLinhaPorCompra(root, cb));
			}
			return cb.and(predicados.toArray(Predicate[]::new));
		};
	}

	static List<Predicate> porMetodosECategorias(From<?, Gasto> root, CriteriaBuilder cb, FiltroGastos filtro) {
		final var predicados = new ArrayList<Predicate>();
		if (!filtro.metodoPagamentoIds().isEmpty()) {
			predicados.add(root.get("metodoPagamento").get("id").in(filtro.metodoPagamentoIds()));
		}
		if (!filtro.categorias().isEmpty()) {
			predicados.add(root.get("categoria").in(filtro.categorias()));
		}
		return predicados;
	}

	static Predicate umaLinhaPorCompra(From<?, Gasto> root, CriteriaBuilder cb) {
		return cb.or(cb.isNull(root.get("compraParcelada")), cb.equal(root.get("numeroParcela"), 1));
	}
}
