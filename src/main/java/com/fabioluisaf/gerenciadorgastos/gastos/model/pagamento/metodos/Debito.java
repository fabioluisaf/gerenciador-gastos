package com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos;

import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;


import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("DEBITO")
public class Debito extends MetodoPagamento {

	public Debito() {
		super(TipoMetodoPagamento.DEBITO);
	}
}
