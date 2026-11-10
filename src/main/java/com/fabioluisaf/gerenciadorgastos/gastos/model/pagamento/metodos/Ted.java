package com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.metodos;

import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.MetodoPagamento;
import com.fabioluisaf.gerenciadorgastos.gastos.model.pagamento.TipoMetodoPagamento;


import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("TED")
public class Ted extends MetodoPagamento {

	public Ted() {
		super(TipoMetodoPagamento.TED);
	}
}
