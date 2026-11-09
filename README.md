# gerenciador-gastos

Confira o [changelog](CHANGELOG.md) para acompanhar as alterações do projeto.

A porta da aplicação é definida pela variável de ambiente `PORTA_APP`. Na
configuração de execução local do VS Code, ela está definida como `8080`.

## API de gastos

- `POST /gastos`: cria um gasto.
- `GET /gastos?mes=11&ano=2026`: lista os gastos do mês, com paginação via
  `page`, `size` e `sort`. A numeração das páginas começa em zero.
- `GET /gastos/{id}`: consulta um gasto.
- `PUT /gastos/{id}`: atualiza todos os campos de um gasto.
- `DELETE /gastos/{id}`: exclui um gasto.

Quando um gasto é uma parcela, as respostas de gastos incluem `parcela`, com
`numero`, `quantidade` e `compraParceladaId`. O campo é omitido para gastos
avulsos. Ao alterar o valor total ou a quantidade de uma compra parcelada, as
parcelas são recriadas e seus IDs mudam.

## API de compras parceladas

- `POST /compras-parceladas`: cria uma compra e seus gastos.
- `GET /compras-parceladas/{id}`: consulta uma compra e suas parcelas.
- `PUT /compras-parceladas/{id}`: atualiza os dados de todas as parcelas; se o
  valor total ou a quantidade mudar, recria as parcelas e seus identificadores.
- `DELETE /compras-parceladas/{id}`: exclui a compra e suas parcelas.

As parcelas são registradas no primeiro dia de cada mês, sempre como
realizadas. A data enviada na criação determina o mês inicial. As parcelas não
podem ser alteradas ou excluídas individualmente pela API de gastos.

Exemplo de corpo para criar uma compra:

```json
{
  "data": "2026-01-31",
  "nome": "Compra",
  "valorTotal": 100.00,
  "quantidadeParcelas": 3,
  "categoria": "Casa",
  "metodoPagamento": "CREDITO_NUBANK",
  "descricao": "Compra parcelada"
}
```

Para atualizar, envie os mesmos campos, exceto `data`. A descrição é opcional.
As respostas incluem o valor total e as parcelas ordenadas pelo número,
com seus identificadores, datas e valores.

As respostas de consulta e alteração incluem a versão da aplicação; a exclusão
retorna HTTP 204 sem corpo. A documentação interativa está em `/swagger`, exceto
no perfil de produção. Para desativar a interface Swagger e os endpoints OpenAPI
em produção, inicie a aplicação com `SPRING_PROFILES_ACTIVE=prod`.

Erros tratados pela API retornam `ApiResponse` com `dados` contendo `status`,
`mensagem`, `caminho` e `instante`. Erros inesperados são registrados no log e
retornam uma mensagem genérica ao cliente.

Para erros de validação, `dados.errosValidacao` lista `campo` e `codigo` (por
exemplo, `NotBlank`, `NotNull`, `Min` ou `Max`), permitindo que o cliente
produza suas próprias mensagens. Em erros globais de validação, `campo` é nulo.