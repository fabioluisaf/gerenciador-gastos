# gerenciador-gastos

Confira o [changelog](CHANGELOG.md) para acompanhar as alterações do projeto.

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
