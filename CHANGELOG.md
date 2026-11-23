# Changelog

## 2.2.2
- Pipeline e Release com as actions do Docker nas versões que rodam em Node 24 (`setup-buildx-action@v4`, `login-action@v4`, `metadata-action@v6`, `build-push-action@v7`), sem o aviso de descontinuação do Node 20. Sem mudança no sistema.

## 2.2.1
- Removidos os avisos de nulidade em `GrupoPlanejado` e `GrupoPlanejadoService`, sem mudança de comportamento.

## 2.2.0
- Grupos de gastos planejados: modelos com vários gastos (nome, valor, categoria e descrição), em `/grupos-planejados` (listar, buscar, criar, atualizar e excluir). A migração V13 cria as tabelas dos grupos e dos seus itens.
- `POST /grupos-planejados/{id}/aplicacoes` cria um gasto planejado para cada item enviado, todos com o mesmo método de pagamento e na mesma data (`data`) ou fatura (`faturaPeriodo`), seguindo as regras da criação de gasto. Os itens vêm no corpo, então valores ajustados e itens deixados de fora não alteram o grupo; se um gasto falhar, nenhum é criado.
- A versão (`VERSAO_APP`) aparece no log de inicialização.

## 2.1.0
- Filtro `nome` em `GET /gastos`, `GET /gastos/planejados`, `GET /gastos/periodos` e `GET /gastos/planejados/periodos`: busca o texto em qualquer parte do nome, sem diferenciar maiúsculas nem acentos.
- `GET /gastos/totais` soma os gastos de um intervalo (`inicio` e `fim` obrigatórios), com os filtros opcionais `metodoPagamentoId`, `categoria` e `nome`; devolve `realizado` e `planejado` (realizado mais planejado). Crédito conta pelo vencimento da fatura e compras parceladas entram pela parcela do período.
- `GET /backup` baixa um zip com um CSV por tabela do banco (sem a tabela do Flyway), lido numa única transação somente leitura.

## 2.0.0
- Gasto precisa ter data ou mês da fatura (`DATA_OU_FATURA_OBRIGATORIA`, validado no corpo da requisição); como só o crédito aceita fatura, fora dele a data passa a ser obrigatória também nos planejados. O banco ganhou a mesma regra (V12).
- Gastos planejados sem data entram no mês do vencimento da fatura, tanto em `GET /gastos/planejados/periodos` quanto na ordem de `GET /gastos/planejados`; a listagem passa a devolver `dataVencimentoFatura` nos gastos no crédito.
- **Quebra de contrato:** removidos `GET /metodos-pagamento/{id}` e `GET /instituicoes-financeiras/{id}`, que só repetiam o que as listagens já devolvem (o ícone em `GET /instituicoes-financeiras/{id}/icone` continua).
- **Quebra de contrato:** instituição financeira deixa de ser um enum (`NUBANK`/`CAIXA`) e vira um cadastro próprio com nome, ícone e cor (CRUD em `/instituicoes-financeiras`, ícone em `GET /instituicoes-financeiras/{id}/icone`, cor opcional no formato `#RRGGBB`).
  - Métodos de pagamento passam a receber `instituicaoFinanceiraId` e a devolver `instituicaoFinanceira` como objeto `{ id, nome, possuiIcone, cor }`.
  - Migrações criam Nubank e Caixa com os ícones que estavam no frontend e as cores das marcas, e ligam os métodos de pagamento existentes a elas.
- **Quebra de contrato:** `GET /gastos/intervalo` foi substituído por duas listagens paginadas, com os mesmos filtros opcionais (`inicio`, `fim`, `metodoPagamentoId`, `categoria` e o novo `faturaId`). `metodoPagamentoId` e `categoria` aceitam vários valores (repetindo o parâmetro): valores do mesmo filtro se somam, filtros diferentes se combinam:
  - `GET /gastos`: gastos realizados, dos mais recentes para os mais antigos;
  - `GET /gastos/planejados`: gastos ainda não realizados, dos mais próximos para os mais distantes, com os sem data no fim.
  - Compras parceladas aparecem uma vez só, com a quantidade de parcelas e o valor total da compra; filtrando por fatura, aparece a parcela daquela fatura.
- **Quebra de contrato:** nos totais e no filtro de período da listagem, gastos no crédito passam a contar pela data de **vencimento** da fatura (antes era a de fechamento); os demais métodos continuam pela data do gasto.
- **Quebra de contrato:** `GET /gastos/periodos` passa a listar só os meses com gastos realizados, com a quantidade de cada mês (contando cada compra parcelada uma vez), e aceita os mesmos filtros de `metodoPagamentoId` e `categoria` da listagem.
- `GET /gastos/planejados/periodos` lista os meses com gastos planejados, dos mais próximos para os mais distantes, com os mesmos filtros; os gastos sem data vêm num grupo próprio no fim, com `ano` e `mes` nulos.
- `POST /compras-parceladas` aceita `realizado` (opcional, padrão `true`) para planejar uma compra parcelada; as parcelas são criadas como planejadas.
- `GET /gastos/categorias` lista as categorias já usadas, da mais usada para a menos usada, com a quantidade de gastos de cada uma (cada compra parcelada conta uma vez).
- `GET /metodos-pagamento/uso` devolve quantos gastos cada método de pagamento tem, contando cada compra parcelada uma vez; métodos sem gastos não aparecem.
- Gastos avulsos aceitam valor negativo, para registrar estornos; eles abatem dos totais e da fatura. Compras parceladas continuam só com valores positivos ou zero.

## 1.4.9
- Valores personalizados por parcela também na criação, com validação da soma.
- Recálculo automático das parcelas ao atualizar a compra sem informar valores individuais.
- Atualização de compras parceladas passa a exigir o estado de realização, aplicado a todas as parcelas.
- Todas as parcelas mantêm a data da compra; apenas as faturas avançam mensalmente.
- Migração das parcelas existentes para a data da primeira, preservando as faturas e impedindo datas divergentes na mesma compra.

## 1.4.8
- Compras parceladas limitadas a 1000 parcelas.
- Valores monetários limitados a duas casas decimais; nomes e descrições, a 255 caracteres.
- Proteção contra criação concorrente de faturas por método de pagamento.

## 1.4.7
- Padronização dos DTOs de requisição e da conversão de datas.

## 1.4.6
- Remoção de código não utilizado.

## 1.4.5
- Ajustes nos testes

## 1.4.4
- Removidos endpoints redundantes/não utilizados.

## 1.4.3

- Incluída a descrição nas respostas resumidas de gastos.

## 1.4.2

- Totalizações por categoria e método de pagamento passam a exigir `inicio` e `fim`, substituindo `periodo` e `faturaId`.
- Totalizações seguem a regra da listagem por intervalo: crédito pelo fechamento da fatura, demais métodos pela data do gasto, incluindo as datas-limite.

## 1.4.1

- Adicionados filtros por categoria e método de pagamento à listagem de gastos por intervalo de datas.

## 1.4.0

- Adicionado endpoint de listagem de gastos por intervalo de datas: crédito pelo fechamento da fatura, demais métodos pela data do gasto.
- Adicionado endpoint para criar faturas escolhendo o cartão e o mês de fechamento.
- Tentativa de criar fatura duplicada retorna conflito com o identificador da fatura existente em `recursoId`.
- Refatorações internas.

## 1.3.1

- Corrigida a associação de faturas às parcelas de compras parceladas.

## 1.3.0

- Adicionado endpoint para listar os meses e anos que possuem gastos.

## 1.2.5

- Data de vencimento da fatura incluída nas informações retornadas junto a gastos e parcelas.

## 1.2.4

- Dia de vencimento obrigatório no cadastro e atualização de cartões de crédito.
- Faturas incluem data de vencimento baseada no dia configurado no cartão, também retornada na listagem resumida.
- Faturas existentes recebem vencimento sete dias após o fechamento; a sugestão de período continua baseada no fechamento.

## 1.2.3

- Pequenos ajustes de organização no código.

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
