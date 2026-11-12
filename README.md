# gerenciador-gastos

Confira o [changelog](CHANGELOG.md) para acompanhar as alterações do projeto.

## Vencimento das faturas

Cartões de crédito exigem `diaFechamento` e `diaVencimento` (de 1 a 31) na
criação e atualização. Outros métodos não aceitam esses campos.
O vencimento da fatura é a próxima ocorrência do dia configurado após o
fechamento, limitado ao último dia do mês. Alterar o cartão não modifica
datas de faturas já criadas. A sugestão de período continua baseada apenas
no fechamento.

A listagem de faturas retorna `dataFechamento`, `dataVencimento` e `valorTotal`,
além do identificador. A migração preenche faturas existentes com vencimento
sete dias após o fechamento; para cartões existentes, usa o dia de vencimento
da fatura mais recente. Cartões sem faturas recebem fechamento + 7 em um
ciclo de 31 dias.

## Resposta de compra parcelada

Os campos `nome`, `categoria`, `metodoPagamento` e `descricao` ficam na compra,
sem repetição nas parcelas. Cada parcela contém `id`, `numeroParcela`, `data`,
`valor`, `fatura` e `realizado`.

## Organização dos DTOs

Os DTOs mantêm a separação entre `request` e `response`, agrupados por uso:

- `request.criacao`, `request.atualizacao` e `request.consulta`.
- `response.detalhe`, `response.listagem` e `response.totalizacao`.
- `response.comum` para DTOs compartilhados entre grupos.

DTOs auxiliares exclusivos ficam no mesmo pacote do DTO que os utiliza.
