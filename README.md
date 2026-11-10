# gerenciador-gastos

Confira o [changelog](CHANGELOG.md) para acompanhar as alterações do projeto.

## Métodos de pagamento

As entidades `Gasto` e `CompraParcelada` ficam em `gastos.model`. O pacote
`gastos.model.pagamento` agrupa os métodos de pagamento, seus enums e `Fatura`.
As subclasses de `MetodoPagamento` ficam em `gastos.model.pagamento.metodos`.

`MetodoPagamento` é uma entidade abstrata com as subclasses `Credito`, `Debito`,
`Pix`, `Boleto`, `Doc` e `Ted`. A herança usa `SINGLE_TABLE`: todos permanecem na tabela `metodos_pagamento`,
com a coluna `tipo` como discriminador.

O tipo é definido na criação e não pode ser alterado. O corpo da atualização
recebe apenas `instituicaoFinanceira`, `nome` e `diaFechamento`, sem o campo `tipo`.
Somente `Credito` armazena dia de fechamento, obrigatório entre 1 e 31.
Débito, Pix, boleto, DOC e TED rejeitam dia de fechamento informado e não possuem fatura.
