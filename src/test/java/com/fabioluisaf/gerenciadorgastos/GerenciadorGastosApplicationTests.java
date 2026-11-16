package com.fabioluisaf.gerenciadorgastos;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.InstituicaoFinanceira;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos.Credito;
import com.fabioluisaf.gerenciadorgastos.gastos.repository.MetodoPagamentoRepository;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:contexto;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"server.port=0"
})
class GerenciadorGastosApplicationTests {

	@Autowired
	private MetodoPagamentoRepository metodoPagamentoRepository;

	@Test
	void contextLoads() {
	}

	@Test
	@Transactional
	void bloqueiaMetodoPagamentoParaCriacaoDeFatura() {
		final MetodoPagamento metodo = new Credito(10, 17);
		metodo.setNome("Cartao de credito");
		metodo.setInstituicaoFinanceira(InstituicaoFinanceira.CAIXA);
		final var salvo = metodoPagamentoRepository.saveAndFlush(metodo);

		assertSame(salvo, metodoPagamentoRepository.findByIdForUpdate(salvo.getId()).orElseThrow());
	}

}
