# Changelog

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
