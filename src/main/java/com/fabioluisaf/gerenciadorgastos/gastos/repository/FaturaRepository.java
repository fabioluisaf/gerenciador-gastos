package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem.FaturaResumidaResponse;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.Fatura;

public interface FaturaRepository extends JpaRepository<Fatura, UUID> {

	@Query(value = """
        select new com.fabioluisaf.gerenciadorgastos.gastos.dto.response.listagem.FaturaResumidaResponse(
            f.id, f.dataFechamento, f.dataVencimento, coalesce(sum(g.valor), 0)
        )
        from Fatura f
        left join f.gastos g on g.realizado = true
        where f.metodoPagamento.id = :metodoPagamentoId
        group by f.id, f.dataFechamento, f.dataVencimento
    """, countQuery = """
        select count(f) from Fatura f where f.metodoPagamento.id = :metodoPagamentoId
    """)
	Page<FaturaResumidaResponse> listarResumosPorMetodoPagamentoId(UUID metodoPagamentoId, Pageable pageable);

	boolean existsByMetodoPagamentoId(UUID metodoPagamentoId);

	@EntityGraph(attributePaths = { 
        "metodoPagamento", "gastos", "gastos.metodoPagamento", 
        "gastos.fatura", "gastos.compraParcelada" 
    })
	Optional<Fatura> findByMetodoPagamentoIdAndDataFechamentoBetween(
			UUID metodoPagamentoId, LocalDate inicio, LocalDate fim
	);
}
