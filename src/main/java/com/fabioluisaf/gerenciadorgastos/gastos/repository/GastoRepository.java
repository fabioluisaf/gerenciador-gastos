package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem.PeriodoComGastosResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalCategoriaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalMetodoPagamentoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.model.Gasto;

public interface GastoRepository extends JpaRepository<Gasto, UUID>{

	String CONDICAO_INTERVALO = """
        ((g.tipoMetodoPagamento = com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento.CREDITO
          and f.dataFechamento between :inicio and :fim)
         or (g.tipoMetodoPagamento <> com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento.CREDITO
          and g.data between :inicio and :fim))
        """;

	@Query("""
        select distinct new com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem.PeriodoComGastosResponse(
            year(g.data), month(g.data)
        )
        from Gasto g
        where g.data is not null
        order by year(g.data) desc, month(g.data) desc
    """)
	List<PeriodoComGastosResponse> listarPeriodosComGastos();

	String DATA_REFERENCIA = """
        (case when g.tipoMetodoPagamento = com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento.CREDITO
            then f.dataFechamento else g.data end)
        """;

	@EntityGraph(attributePaths = { "compraParcelada", "metodoPagamento", "metodoPagamento.instituicaoFinanceira", "fatura" })
	@Query("""
        select g from Gasto g left join g.fatura f
        where g.realizado = :realizado
        and (cast(:inicio as LocalDate) is null or """ + DATA_REFERENCIA + """
         >= :inicio)
        and (cast(:fim as LocalDate) is null or """ + DATA_REFERENCIA + """
         <= :fim)
        and (:metodoPagamentoId is null or g.metodoPagamento.id = :metodoPagamentoId)
        and (:categoria is null or g.categoria = :categoria)
        and (:faturaId is null or f.id = :faturaId)
    """)
	Page<Gasto> listar(
			boolean realizado, LocalDate inicio, LocalDate fim, UUID metodoPagamentoId, String categoria,
			UUID faturaId, Pageable pageable);

	@Query(value = """
        select new com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalCategoriaResponse(
            g.categoria,
            sum(case when g.realizado = true then g.valor else 0 end),
            sum(g.valor)
        )
        from Gasto g left join g.fatura f
        where
        """ + CONDICAO_INTERVALO + """
        group by g.categoria
    """, countQuery = """
        select count(distinct g.categoria) from Gasto g left join g.fatura f
        where
    """ + CONDICAO_INTERVALO)
	Page<TotalCategoriaResponse> totalizarPorCategoria(LocalDate inicio, LocalDate fim, Pageable pageable);

	@Query(value = """
        select new com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalMetodoPagamentoResponse(
            m.id, i.id, i.nome, case when i.iconeTipo is null then false else true end, m.tipo, m.nome,
            sum(case when g.realizado = true then g.valor else 0 end),
            sum(g.valor)
        )
        from Gasto g join g.metodoPagamento m join m.instituicaoFinanceira i left join g.fatura f
        where
        """ + CONDICAO_INTERVALO + """
        group by m.id, i.id, i.nome, i.iconeTipo, m.tipo, m.nome
    """, countQuery = """
        select count(distinct g.metodoPagamento.id) from Gasto g left join g.fatura f
        where
    """ + CONDICAO_INTERVALO)
	Page<TotalMetodoPagamentoResponse> totalizarPorMetodoPagamento(
        LocalDate inicio, LocalDate fim, Pageable pageable
    );

	boolean existsByMetodoPagamentoId(UUID metodoPagamentoId);

	@Override
	@EntityGraph(attributePaths = { "compraParcelada", "metodoPagamento", "metodoPagamento.instituicaoFinanceira", "fatura" })
	Optional<Gasto> findById(UUID id);
}
