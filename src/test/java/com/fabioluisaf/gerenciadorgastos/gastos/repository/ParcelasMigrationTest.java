package com.fabioluisaf.gerenciadorgastos.gastos.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "test.postgres.url", matches = "jdbc:postgresql:.*")
class ParcelasMigrationTest {

    private final String url = System.getProperty("test.postgres.url");
    private final String schema = "parcelas_" + UUID.randomUUID().toString().replace("-", "");
    private final UUID compraId = UUID.randomUUID();
    private final UUID metodoId = UUID.randomUUID();
    private final UUID faturaId = UUID.randomUUID();
    private final LocalDate dataCompra = LocalDate.of(2026, 11, 9);
    private Connection connection;

    @BeforeEach
    void configurar() throws SQLException {
        migration("8").migrate();
        connection = conectar();
        executar(connection, """
                INSERT INTO metodos_pagamento
                    (id, tipo, instituicao_financeira, nome, dia_fechamento, dia_vencimento)
                VALUES (?, 'CREDITO', 'NUBANK', 'Cartao', 14, 21)
                """, metodoId);
        executar(connection, "INSERT INTO compras_parceladas (id, valor_total) VALUES (?, 30)", compraId);
        for (int numero = 1; numero <= 3; numero++) {
            final var fatura = numero == 1 ? faturaId : UUID.randomUUID();
            final var fechamento = LocalDate.of(2026, 11, 14).plusMonths(numero - 1L);
            executar(connection, """
                    INSERT INTO faturas
                        (id, metodo_pagamento_id, data_fechamento, data_vencimento)
                    VALUES (?, ?, ?, ?)
                    """, fatura, metodoId, fechamento, fechamento.plusDays(7));
            inserir(connection, compraId, numero, numero == 1
                    ? dataCompra : dataCompra.plusMonths(numero - 1L).withDayOfMonth(1), fatura);
        }
        inserir(connection, null, null, dataCompra.plusDays(2), faturaId);
    }

    @AfterEach
    void limpar() throws SQLException {
        if (connection != null) {
            if (!connection.getAutoCommit()) {
                connection.rollback();
            }
            connection.close();
        }
        try (final var cleanup = DriverManager.getConnection(url)) {
            executar(cleanup, "DROP SCHEMA " + schema + " CASCADE");
        }
    }

    @Test
    void normalizaDatasAntigasSemAlterarFaturasOuGastosAvulsos() throws SQLException {
        final var faturas = consultar(connection, """
                SELECT string_agg(fatura_id::text, ',' ORDER BY numero_parcela)
                FROM gastos WHERE compra_parcelada_id = ?
                """, compraId);

        migration(null).migrate();

        assertEquals("{2026-11-09,2026-11-09,2026-11-09}", datas(connection, compraId));
        assertEquals(faturas, consultar(connection, """
                SELECT string_agg(fatura_id::text, ',' ORDER BY numero_parcela)
                FROM gastos WHERE compra_parcelada_id = ?
                """, compraId));
        assertEquals("2026-11-11", consultar(connection, "SELECT data FROM gastos WHERE compra_parcelada_id IS NULL"));
        inserir(connection, null, null, null, faturaId);
        inserir(connection, null, null, dataCompra.plusMonths(4), faturaId);
    }

    @Test
    void validaEstadoFinalDaTransacaoEBloqueiaInsercaoOuAtualizacaoDivergente() throws SQLException {
        migration(null).migrate();
        connection.setAutoCommit(false);
        executar(connection, "UPDATE gastos SET data = ? WHERE compra_parcelada_id = ? AND numero_parcela = 1",
                dataCompra.plusDays(1), compraId);
        executar(connection, "UPDATE gastos SET data = ? WHERE compra_parcelada_id = ? AND numero_parcela > 1",
                dataCompra.plusDays(1), compraId);
        connection.commit();
        assertEquals("{2026-11-10,2026-11-10,2026-11-10}", datas(connection, compraId));

        for (final var data : new LocalDate[] { dataCompra, null }) {
            executar(connection, "UPDATE gastos SET data = ? WHERE compra_parcelada_id = ? AND numero_parcela = 2",
                    data, compraId);
            assertEquals("23514", assertThrows(SQLException.class, connection::commit).getSQLState());
            connection.rollback();
            assertEquals("{2026-11-10,2026-11-10,2026-11-10}", datas(connection, compraId));
        }

        inserir(connection, compraId, 4, dataCompra.plusDays(1), faturaId);
        connection.commit();
        inserir(connection, compraId, 5, dataCompra, faturaId);
        assertEquals("23514", assertThrows(SQLException.class, connection::commit).getSQLState());
        connection.rollback();
        assertEquals("{2026-11-10,2026-11-10,2026-11-10,2026-11-10}", datas(connection, compraId));
        final var outraCompra = UUID.randomUUID();
        executar(connection, "INSERT INTO compras_parceladas (id, valor_total) VALUES (?, 10)", outraCompra);
        inserir(connection, outraCompra, 1, dataCompra, faturaId);
        connection.commit();
        assertEquals("{2026-11-09}", datas(connection, outraCompra));
    }

    @Test
    void serializaValidacaoDeParcelasInseridasSimultaneamente() throws Exception {
        migration(null).migrate();
        final var novaCompra = UUID.randomUUID();
        executar(connection, "INSERT INTO compras_parceladas (id, valor_total) VALUES (?, 20)", novaCompra);
        try (final var primeira = conectar(); final var segunda = conectar();
                final var executor = Executors.newSingleThreadExecutor()) {
            primeira.setAutoCommit(false);
            segunda.setAutoCommit(false);
            executar(primeira, "SELECT id FROM compras_parceladas WHERE id = ? FOR NO KEY UPDATE", novaCompra);
            inserir(primeira, novaCompra, 1, dataCompra, faturaId);
            inserir(segunda, novaCompra, 2, dataCompra.plusDays(1), faturaId);
            final var inicio = new CountDownLatch(1);
            final var resultado = executor.submit(() -> {
                inicio.countDown();
                return assertThrows(SQLException.class, segunda::commit).getSQLState();
            });
            assertTrue(inicio.await(5, TimeUnit.SECONDS));
            try {
                assertThrows(TimeoutException.class, () -> resultado.get(200, TimeUnit.MILLISECONDS));
            } finally {
                primeira.commit();
            }
            assertEquals("23514", resultado.get(5, TimeUnit.SECONDS));
            segunda.rollback();
            assertEquals("{2026-11-09}", datas(connection, novaCompra));
        }
    }

    private Flyway migration(String target) {
        final var config = Flyway.configure().dataSource(url, null, null)
                .schemas(schema).defaultSchema(schema);
        if (target != null) {
            config.target(target);
        }
        return config.load();
    }

    private Connection conectar() throws SQLException {
        final var result = DriverManager.getConnection(url);
        result.setSchema(schema);
        executar(result, "SET lock_timeout = '5s'");
        return result;
    }

    private void inserir(Connection target, UUID compra, Integer numero, LocalDate data, UUID fatura)
            throws SQLException {
        executar(target, """
                INSERT INTO gastos
                    (id, data, nome, valor, categoria, descricao, realizado, compra_parcelada_id,
                    numero_parcela, metodo_pagamento_id, tipo_metodo_pagamento, fatura_id)
                VALUES (?, ?, 'Compra', 10, 'Casa', '', true, ?, ?, ?, 'CREDITO', ?)
                """, UUID.randomUUID(), data, compra, numero, metodoId, fatura);
    }

    private String datas(Connection target, UUID compra) throws SQLException {
        return consultar(target, """
                SELECT array_agg(data ORDER BY numero_parcela)::text
                FROM gastos WHERE compra_parcelada_id = ?
                """, compra);
    }

    private void executar(Connection target, String sql, Object... parameters) throws SQLException {
        try (final var statement = target.prepareStatement(sql)) {
            for (int index = 0; index < parameters.length; index++) {
                statement.setObject(index + 1, parameters[index]);
            }
            statement.execute();
        }
    }

    private String consultar(Connection target, String sql, Object... parameters) throws SQLException {
        try (final var statement = target.prepareStatement(sql)) {
            for (int index = 0; index < parameters.length; index++) {
                statement.setObject(index + 1, parameters[index]);
            }
            try (final var result = statement.executeQuery()) {
                result.next();
                return result.getString(1);
            }
        }
    }
}
