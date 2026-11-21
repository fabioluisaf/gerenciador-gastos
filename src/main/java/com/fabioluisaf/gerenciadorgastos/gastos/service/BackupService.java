package com.fabioluisaf.gerenciadorgastos.gastos.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.sql.DataSource;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BackupService {

	private static final String TABELA_FLYWAY = "flyway_schema_history";
	private static final String FIM_LINHA = "\r\n";

	private final DataSource dataSource;

	public byte[] gerar() {
		final var saida = new ByteArrayOutputStream();
		try (final var conexao = dataSource.getConnection();
				final var zip = new ZipOutputStream(saida, StandardCharsets.UTF_8)) {
			conexao.setReadOnly(true);
			conexao.setAutoCommit(false);
			conexao.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			try {
				for (final var tabela : tabelas(conexao)) {
					zip.putNextEntry(new ZipEntry(tabela.toLowerCase(Locale.ROOT) + ".csv"));
					final var escritor = new OutputStreamWriter(zip, StandardCharsets.UTF_8);
					escreverCsv(conexao, tabela, escritor);
					escritor.flush();
					zip.closeEntry();
				}
			} finally {
				conexao.rollback();
			}
		} catch (SQLException | IOException excecao) {
			throw new IllegalStateException("Falha ao gerar o backup", excecao);
		}
		return saida.toByteArray();
	}

	private static List<String> tabelas(Connection conexao) throws SQLException {
		final var tabelas = new ArrayList<String>();
		try (final var resultado = conexao.getMetaData()
				.getTables(conexao.getCatalog(), conexao.getSchema(), "%", new String[] { "TABLE" })) {
			while (resultado.next()) {
				final var nome = resultado.getString("TABLE_NAME");
				if (!TABELA_FLYWAY.equalsIgnoreCase(nome)) {
					tabelas.add(nome);
				}
			}
		}
		tabelas.sort(String.CASE_INSENSITIVE_ORDER);
		return tabelas;
	}

	private static void escreverCsv(Connection conexao, String tabela, Writer escritor)
			throws SQLException, IOException {
		final var aspas = conexao.getMetaData().getIdentifierQuoteString().trim();
		final var identificador = aspas + tabela.replace(aspas, aspas + aspas) + aspas;
		try (final var comando = conexao.createStatement();
				final var resultado = comando.executeQuery("SELECT * FROM " + identificador)) {
			final var metadados = resultado.getMetaData();
			final var colunas = metadados.getColumnCount();
			final var cabecalho = new ArrayList<String>(colunas);
			for (int coluna = 1; coluna <= colunas; coluna++) {
				cabecalho.add(metadados.getColumnLabel(coluna).toLowerCase(Locale.ROOT));
			}
			escreverLinha(escritor, cabecalho);
			while (resultado.next()) {
				final var valores = new ArrayList<String>(colunas);
				for (int coluna = 1; coluna <= colunas; coluna++) {
					valores.add(formatar(resultado.getObject(coluna)));
				}
				escreverLinha(escritor, valores);
			}
		}
	}

	private static String formatar(Object valor) {
		return switch (valor) {
			case null -> "";
			case byte[] bytes -> Base64.getEncoder().encodeToString(bytes);
			case BigDecimal numero -> numero.toPlainString();
			default -> valor.toString();
		};
	}

	private static void escreverLinha(Writer escritor, List<String> valores) throws IOException {
		for (int indice = 0; indice < valores.size(); indice++) {
			if (indice > 0) {
				escritor.write(',');
			}
			escritor.write(escapar(valores.get(indice)));
		}
		escritor.write(FIM_LINHA);
	}

	private static String escapar(String valor) {
		if (valor.contains(",") || valor.contains("\"") || valor.contains("\n") || valor.contains("\r")) {
			return "\"" + valor.replace("\"", "\"\"") + "\"";
		}
		return valor;
	}
}
