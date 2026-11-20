package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalCategoriaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.TotalMetodoPagamentoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.UsoCategoriaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.UsoMetodoPagamentoResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.model.Gasto;

public interface GastoRepository
		extends JpaRepository<Gasto, UUID>, JpaSpecificationExecutor<Gasto>, PeriodosGastosRepository {

	String CONDICAO_INTERVALO = """
        ((g.tipoMetodoPagamento = com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento.CREDITO
          and f.dataVencimento between :inicio and :fim)
         or (g.tipoMetodoPagamento <> com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento.CREDITO
          and g.data between :inicio and :fim))
        """;

	@Override
	@EntityGraph(attributePaths = { "compraParcelada", "metodoPagamento", "metodoPagamento.instituicaoFinanceira", "fatura" })
	Page<Gasto> findAll(Specification<Gasto> specification, Pageable pageable);

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
            m.id, i.id, i.nome, case when i.iconeTipo is null then false else true end, i.cor, m.tipo, m.nome,
            sum(case when g.realizado = true then g.valor else 0 end),
            sum(g.valor)
        )
        from Gasto g join g.metodoPagamento m join m.instituicaoFinanceira i left join g.fatura f
        where
        """ + CONDICAO_INTERVALO + """
        group by m.id, i.id, i.nome, i.iconeTipo, i.cor, m.tipo, m.nome
    """, countQuery = """
        select count(distinct g.metodoPagamento.id) from Gasto g left join g.fatura f
        where
    """ + CONDICAO_INTERVALO)
	Page<TotalMetodoPagamentoResponse> totalizarPorMetodoPagamento(
        LocalDate inicio, LocalDate fim, Pageable pageable
    );

	@Query("""
        select new com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.UsoMetodoPagamentoResponse(
            g.metodoPagamento.id, count(g)
        )
        from Gasto g
        where g.compraParcelada is null or g.numeroParcela = 1
        group by g.metodoPagamento.id
    """)
	List<UsoMetodoPagamentoResponse> contarGastosPorMetodoPagamento();

	@Query("""
        select new com.fabioluisaf.gerenciadorgastos.gastos.dto.response.totalizacao.UsoCategoriaResponse(
            g.categoria, count(g)
        )
        from Gasto g
        where g.compraParcelada is null or g.numeroParcela = 1
        group by g.categoria
        order by count(g) desc, g.categoria
    """)
	List<UsoCategoriaResponse> contarGastosPorCategoria();

	boolean existsByMetodoPagamentoId(UUID metodoPagamentoId);

	@Override
	@EntityGraph(attributePaths = { "compraParcelada", "metodoPagamento", "metodoPagamento.instituicaoFinanceira", "fatura" })
	Optional<Gasto> findById(UUID id);
}
