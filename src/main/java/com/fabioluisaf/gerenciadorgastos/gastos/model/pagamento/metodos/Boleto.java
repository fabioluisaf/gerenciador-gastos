package com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos;

import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;


import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("BOLETO")
public class Boleto extends MetodoPagamento {

	public Boleto() {
		super(TipoMetodoPagamento.BOLETO);
	}
}
