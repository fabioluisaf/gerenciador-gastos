# gerenciador-gastos

## API de gastos

- `POST /gastos`: cria um gasto.
- `GET /gastos?mes=11&ano=2026`: lista os gastos do mês, com paginação via
  `page`, `size` e `sort`. A numeração das páginas começa em zero.
- `GET /gastos/{id}`: consulta um gasto.
- `PUT /gastos/{id}`: atualiza todos os campos de um gasto.
- `DELETE /gastos/{id}`: exclui um gasto.

As respostas de consulta e alteração incluem a versão da aplicação; a exclusão
retorna HTTP 204 sem corpo. A documentação interativa está em `/swagger`.

Erros tratados pela API retornam `ApiResponse` com `dados` contendo `status`,
`mensagem`, `caminho` e `instante`. Erros inesperados são registrados no log e
retornam uma mensagem genérica ao cliente.