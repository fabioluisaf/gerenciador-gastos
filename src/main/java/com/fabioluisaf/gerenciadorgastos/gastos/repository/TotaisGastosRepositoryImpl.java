package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import java.math.BigDecimal;
import java.util.ArrayList;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalGastosResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.model.Gasto;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.Fatura;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TotaisGastosRepositoryImpl implements TotaisGastosRepository {

	private final EntityManager entityManager;

	@Override
	public TotalGastosResponse totalizar(FiltroGastos filtro) {
		final var cb = entityManager.getCriteriaBuilder();
		final var consulta = cb.createQuery(TotalGastosResponse.class);
		final var root = consulta.from(Gasto.class);
		final var fatura = root.<Gasto, Fatura>join("fatura", JoinType.LEFT);
		final var valor = root.<BigDecimal>get("valor");
		final var dataReferencia = GastoSpecifications.dataReferencia(root, fatura, cb);

		final var predicados = new ArrayList<Predicate>();
		predicados.add(cb.between(dataReferencia, filtro.inicio(), filtro.fim()));
		predicados.addAll(GastoSpecifications.porMetodosECategorias(root, cb, filtro));

		final var zero = cb.literal(BigDecimal.ZERO);
		final var realizado = cb.<BigDecimal>selectCase()
				.when(cb.isTrue(root.get("realizado")), valor)
				.otherwise(zero);

		consulta.select(cb.construct(TotalGastosResponse.class,
				cb.coalesce(cb.sum(realizado), zero),
				cb.coalesce(cb.sum(valor), zero)))
				.where(predicados.toArray(Predicate[]::new));

		return entityManager.createQuery(consulta).getSingleResult();
	}
}
