# Changelog

## 1.2.2

- Adicionados os métodos de pagamento boleto, DOC e TED; o tipo não pode ser alterado após o cadastro.
- Faturas selecionadas por mês e ano, com fechamento calculado pelo dia configurado no cartão.
- Consultas mensais e totalizações recebem `periodo` em vez de `mes` e `ano`, aceitando hífen ou ponto na URL.
- Datas e períodos aceitam múltiplos formatos, com validação estrita de valores inválidos.
- Nome, categoria, método de pagamento e descrição ficam na resposta da compra parcelada, sem repetição nas parcelas.
- Listagem por fatura mantém apenas o filtro de categoria e a paginação.
- Refatorações internas e reorganização dos DTOs por uso.

## 1.2.1

- Gastos resumidos incluem o estado de realização, a parcela atual e o total de parcelas.
- Totais planejados passam a incluir gastos realizados e não realizados.
- O valor total das faturas resumidas considera apenas os gastos realizados.
- Validação das totalizações informa a necessidade de fornecer mês e ano ou ID da fatura, sem expor campos internos.

## 1.2.0

- Cadastro e manutenção de métodos de pagamento com tipo e instituição financeira.
- Faturas mensais de crédito, com listagem por cartão e consulta paginada dos gastos por fatura.
- Gastos resumidos nas listagens e alterações; informações completas apenas na consulta por ID.
- Filtros por categoria e método de pagamento nas listagens do mês ou da fatura.
- Totais realizados e planejados por categoria e método de pagamento, considerando todo o mês ou fatura.
- Edição de compras parceladas com valores personalizados por parcela, mantendo a soma igual ao total da compra.
- Gastos planejados podem ficar sem data; gastos realizados exigem data.
- Respostas da API unificadas, com paginação nas listagens.
- Refatorações internas e ajustes nos testes, sem uso de banco real.

## 1.1.4

- Mensagens de erro e códigos de validação padronizados em português no formato `UPPER_SNAKE_CASE`.

## 1.1.3

- Atualização do estado de realização de gastos, incluindo parcelas, sem alterar os demais dados.

## 1.1.2

- A primeira parcela usa a data informada; as seguintes ficam no primeiro dia dos meses subsequentes.
- A data passa a ser obrigatória na atualização de compras parceladas.

## 1.1.1

- Respostas de gastos incluem informações da parcela e da compra vinculada.
- Swagger e OpenAPI desativados no perfil de produção.

## 1.1.0

- Cadastro, consulta, atualização e exclusão de compras parceladas.
- Geração de parcelas como gastos realizados no primeiro dia de cada mês.
- Alterações e exclusões de parcelas feitas pela compra parcelada.

## 1.0.1

- Aceito o formato de data `yyyy-MM-dd` nas requisições de gastos.
- Datas inválidas retornam erro de validação com o código `InvalidFormat`.
- Encerramento adequado das requisições em andamento.
- Refatorações internas.

## 1.0.0

- Cadastro, consulta, atualização e exclusão de gastos.
- Listagem paginada de gastos por mês e ano.
- Identificação de gastos planejados e realizados e registro do método de pagamento.
- Validação dos dados e respostas de erro estruturadas para o aplicativo cliente.
