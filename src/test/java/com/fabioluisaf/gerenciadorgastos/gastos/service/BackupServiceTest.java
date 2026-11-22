package com.fabioluisaf.gerenciadorgastos.gastos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BackupServiceTest {

	private JdbcDataSource dataSource;

	@BeforeEach
	void configurar() throws SQLException {
		dataSource = new JdbcDataSource();
		dataSource.setURL("jdbc:h2:mem:backup_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
		try (final var conexao = dataSource.getConnection(); final var comando = conexao.createStatement()) {
			comando.execute("CREATE TABLE flyway_schema_history (versao VARCHAR(10))");
			comando.execute("INSERT INTO flyway_schema_history VALUES ('1')");
			comando.execute("CREATE TABLE instituicoes (nome VARCHAR(100), icone VARBINARY(10))");
			comando.execute("INSERT INTO instituicoes VALUES ('Banco, \"Inter\"', X'0102')");
			comando.execute("INSERT INTO instituicoes VALUES ('Linha' || CHAR(10) || 'nova', NULL)");
			comando.execute("CREATE TABLE gastos (nome VARCHAR(100), valor DECIMAL(10, 2))");
			comando.execute("INSERT INTO gastos VALUES ('Mercado', 10.50)");
		}
	}

	@Test
	void geraUmCsvPorTabelaSemOHistoricoDoFlyway() throws IOException {
		final var arquivos = descompactar(new BackupService(dataSource).gerar());
		final var icone = Base64.getEncoder().encodeToString(new byte[] { 1, 2 });

		assertEquals(2, arquivos.size());
		assertEquals("nome,valor\r\nMercado,10.50\r\n", arquivos.get("gastos.csv"));
		assertEquals("nome,icone\r\n\"Banco, \"\"Inter\"\"\"," + icone + "\r\n\"Linha\nnova\",\r\n",
				arquivos.get("instituicoes.csv"));
	}

	private static Map<String, String> descompactar(byte[] conteudo) throws IOException {
		final var arquivos = new LinkedHashMap<String, String>();
		try (final var zip = new ZipInputStream(new ByteArrayInputStream(conteudo), StandardCharsets.UTF_8)) {
			for (var entrada = zip.getNextEntry(); entrada != null; entrada = zip.getNextEntry()) {
				arquivos.put(entrada.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
			}
		}
		return arquivos;
	}
}
