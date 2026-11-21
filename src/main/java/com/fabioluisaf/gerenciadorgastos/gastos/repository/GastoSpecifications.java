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
import jakarta.persistence.criteria.Nulls;
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

			final var dataReferencia = dataReferencia(root, fatura, cb);

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

			if (!realizado && !Long.class.equals(query.getResultType())) {
				final Expression<LocalDate> referencia =
						cb.coalesce(root.<LocalDate>get("data"), fatura.<LocalDate>get("dataVencimento"));
				query.orderBy(cb.asc(referencia, Nulls.LAST), cb.asc(root.get("id")));
			}

			return cb.and(predicados.toArray(Predicate[]::new));
		};
	}

	private static final String COM_ACENTO = "áàâãäéèêëíìîïóòôõöúùûüçñ";
	private static final String SEM_ACENTO = "aaaaaeeeeiiiiooooouuuucn";

	static List<Predicate> porMetodosECategorias(From<?, Gasto> root, CriteriaBuilder cb, FiltroGastos filtro) {
		final var predicados = new ArrayList<Predicate>();

		if (!filtro.metodoPagamentoIds().isEmpty()) {
			predicados.add(root.get("metodoPagamento").get("id").in(filtro.metodoPagamentoIds()));
		}
		if (!filtro.categorias().isEmpty()) {
			predicados.add(root.get("categoria").in(filtro.categorias()));
		}
		if (filtro.nome() != null) {
			final var nomeNormalizado = cb.function("translate", String.class, cb.lower(root.get("nome")),
					cb.literal(COM_ACENTO), cb.literal(SEM_ACENTO));
			predicados.add(cb.like(nomeNormalizado, "%" + escaparLike(filtro.nome()) + "%", '!'));
		}

		return predicados;
	}

	private static String escaparLike(String valor) {
		return valor.replace("!", "!!").replace("%", "!%").replace("_", "!_");
	}

	static Expression<LocalDate> dataReferencia(From<?, Gasto> root, From<?, Fatura> fatura, CriteriaBuilder cb) {
		return cb.<LocalDate>selectCase()
				.when(cb.equal(root.get("tipoMetodoPagamento"), TipoMetodoPagamento.CREDITO),
						fatura.<LocalDate>get("dataVencimento"))
				.otherwise(root.<LocalDate>get("data"));
	}

	static Predicate umaLinhaPorCompra(From<?, Gasto> root, CriteriaBuilder cb) {
		return cb.or(cb.isNull(root.get("compraParcelada")), cb.equal(root.get("numeroParcela"), 1));
	}
}
