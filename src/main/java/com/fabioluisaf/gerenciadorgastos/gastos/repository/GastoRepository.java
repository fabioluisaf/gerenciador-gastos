package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.gasto.TotalCategoriaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.gasto.TotalMetodoPagamentoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.model.entity.Gasto;

public interface GastoRepository extends JpaRepository<Gasto, UUID>{

	@EntityGraph(attributePaths = {"compraParcelada", "metodoPagamento", "fatura"})
	@Query("""
            select g from Gasto g
            where g.data between :inicio and :fim
            and (:metodoPagamentoId is null or g.metodoPagamento.id = :metodoPagamentoId)
            and (:categoria is null or g.categoria = :categoria)
            """)
	Page<Gasto> listarPorPeriodo(LocalDate inicio, LocalDate fim, UUID metodoPagamentoId,
			String categoria, Pageable pageable);

	@EntityGraph(attributePaths = { "metodoPagamento" })
	@Query("""
            select g from Gasto g
            where g.fatura.id = :faturaId
            and (:metodoPagamentoId is null or g.metodoPagamento.id = :metodoPagamentoId)
            and (:categoria is null or g.categoria = :categoria)
            """)
	Page<Gasto> listarPorFatura(UUID faturaId, UUID metodoPagamentoId, String categoria, Pageable pageable);

	@Query(value = """
            select new com.fabioluisaf.gerenciadorgastos.gastos.dto.response.gasto.TotalCategoriaResponse(
                g.categoria,
                sum(case when g.realizado = true then g.valor else 0 end),
                sum(case when g.realizado = false then g.valor else 0 end)
            )
            from Gasto g left join g.fatura f
            where (:faturaId is null and g.data between :inicio and :fim) or f.id = :faturaId
            group by g.categoria
            """, countQuery = """
            select count(distinct g.categoria) from Gasto g left join g.fatura f
            where (:faturaId is null and g.data between :inicio and :fim) or f.id = :faturaId
            """)
	Page<TotalCategoriaResponse> totalizarPorCategoria(LocalDate inicio, LocalDate fim,
			UUID faturaId, Pageable pageable);

	@Query(value = """
            select new com.fabioluisaf.gerenciadorgastos.gastos.dto.response.gasto.TotalMetodoPagamentoResponse(
                m.id, m.instituicaoFinanceira, m.tipo, m.nome,
                sum(case when g.realizado = true then g.valor else 0 end),
                sum(case when g.realizado = false then g.valor else 0 end)
            )
            from Gasto g join g.metodoPagamento m left join g.fatura f
            where (:faturaId is null and g.data between :inicio and :fim) or f.id = :faturaId
            group by m.id, m.instituicaoFinanceira, m.tipo, m.nome
            """, countQuery = """
            select count(distinct g.metodoPagamento.id) from Gasto g left join g.fatura f
            where (:faturaId is null and g.data between :inicio and :fim) or f.id = :faturaId
            """)
	Page<TotalMetodoPagamentoResponse> totalizarPorMetodoPagamento(LocalDate inicio, LocalDate fim,
			UUID faturaId, Pageable pageable);

	boolean existsByMetodoPagamentoId(UUID metodoPagamentoId);

	@Override
	@EntityGraph(attributePaths = {"compraParcelada", "metodoPagamento", "fatura"})
	Optional<Gasto> findById(UUID id);
}
