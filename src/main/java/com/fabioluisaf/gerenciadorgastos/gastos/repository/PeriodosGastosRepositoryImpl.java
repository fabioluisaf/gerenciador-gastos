package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem.PeriodoComGastosResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.model.Gasto;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.LocalDateField;
import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PeriodosGastosRepositoryImpl implements PeriodosGastosRepository {

	private final EntityManager entityManager;

	@Override
	public List<PeriodoComGastosResponse> listarPeriodosRealizados(FiltroGastos filtro) {
		return listarPeriodos(true, filtro);
	}

	@Override
	public List<PeriodoComGastosResponse> listarPeriodosPlanejados(FiltroGastos filtro) {
		return listarPeriodos(false, filtro);
	}

	private List<PeriodoComGastosResponse> listarPeriodos(boolean realizado, FiltroGastos filtro) {
		final var cb = entityManager.getCriteriaBuilder();
		final var consulta = cb.createQuery(PeriodoComGastosResponse.class);
		final var root = consulta.from(Gasto.class);
		final var data = root.<LocalDate>get("data");
		final var ano = cb.extract(LocalDateField.YEAR, data);
		final var mes = cb.extract(LocalDateField.MONTH, data);

		final var predicados = new ArrayList<Predicate>();
		predicados.add(cb.equal(root.get("realizado"), realizado));

		if (realizado) {
			predicados.add(cb.isNotNull(data));
		}

		predicados.add(GastoSpecifications.umaLinhaPorCompra(root, cb));
		predicados.addAll(GastoSpecifications.porMetodosECategorias(root, filtro));

		consulta.select(cb.construct(PeriodoComGastosResponse.class, ano, mes, cb.count(root)))
				.where(predicados.toArray(Predicate[]::new))
				.groupBy(ano, mes)
				.orderBy(realizado
						? List.of(cb.desc(ano), cb.desc(mes))
						: List.of(cb.asc(ano, Nulls.LAST), cb.asc(mes, Nulls.LAST)));
						
		return entityManager.createQuery(consulta).getResultList();
	}
}
