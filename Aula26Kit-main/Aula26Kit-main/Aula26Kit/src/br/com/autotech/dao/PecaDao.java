package br.com.autotech.dao;

import br.com.autotech.factory.ConexaoFactory;
import br.com.autotech.model.Peca;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * KIT DE DESENVOLVIMENTO DE LABORATÓRIO • PECADAO (AUTOTECH)
 * ============================================================================
 * Persistência local em SQLite para a oficina AutoTech funcionar sem internet.
 *
 * Padrões aplicados (Aulas 25 e 26):
 * - PreparedStatement com setters tipados (índice 1-based)
 * - try-with-resources em Connection, PreparedStatement e ResultSet
 * - Leitura do ResultSet por nome de coluna
 * - if (rs.next()) para registro único / while (rs.next()) para listagens
 * - UPDATE sempre com cláusula WHERE
 * ============================================================================
 */
public class PecaDao {

    /**
     * MISSÃO 1: DDL AUTÔNOMO (CREATE TABLE IF NOT EXISTS)
     *
     * Aqui o Statement simples é seguro: o SQL é uma constante fixa escrita
     * pelo engenheiro, sem nenhum parâmetro vindo do usuário 
     */
    public void criarTabelaSeNaoExistir() {
        String sql = """
            CREATE TABLE IF NOT EXISTS pecas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                codigo TEXT UNIQUE NOT NULL,
                nome TEXT NOT NULL,
                preco REAL NOT NULL,
                estoque INTEGER NOT NULL
            );
        """;

        try (Connection conn = ConexaoFactory.obterConexao();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql);
            System.out.println("🔧 [AutoTech] Tabela 'pecas' verificada/criada com sucesso!");

        } catch (SQLException e) {
            System.err.println("❌ Erro na Missão 1 (criarTabela): " + e.getMessage());
        }
    }

    /**
     * MISSÃO 2: INSERT COM RESGATE DE CHAVE PRIMÁRIA GERADA
     *
     * Statement.RETURN_GENERATED_KEYS avisa o driver que queremos receber o ID
     * autoincrementado, lido depois com stmt.getGeneratedKeys().
     *
     * @param peca Objeto a ser salvo.
     * @return O mesmo objeto com o ID preenchido.
     */
    public Peca salvar(Peca peca) {
        String sql = "INSERT INTO pecas (codigo, nome, preco, estoque) VALUES (?, ?, ?, ?);";

        try (Connection conn = ConexaoFactory.obterConexao();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, peca.getCodigo());
            stmt.setString(2, peca.getNome());
            stmt.setDouble(3, peca.getPreco());
            stmt.setInt(4, peca.getEstoque());

            int linhas = stmt.executeUpdate();
            if (linhas > 0) {
                try (ResultSet chaves = stmt.getGeneratedKeys()) {
                    if (chaves.next()) {
                        long idGerado = chaves.getLong(1);
                        peca.setId(idGerado); // Sincroniza o objeto Java com o banco!
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erro na Missão 2 (salvar): " + e.getMessage());
        }

        return peca;
    }

    /**
     * MISSÃO 3: CONSULTA GERAL DE PEÇAS (SELECT com while)
     *
     * @return Lista de peças encontradas no banco, ordenadas por nome.
     */
    public List<Peca> listarTodas() {
        List<Peca> lista = new ArrayList<>();
        String sql = "SELECT id, codigo, nome, preco, estoque FROM pecas ORDER BY nome;";

        try (Connection conn = ConexaoFactory.obterConexao();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearLinha(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Erro na Missão 3 (listarTodas): " + e.getMessage());
        }

        return lista;
    }

    /**
     * MISSÃO 4: CONSULTA POR CÓDIGO ESPECÍFICO (SELECT com WHERE)
     *
     * O código é UNIQUE, então é registro único: usa-se if (rs.next()) e não while.
     *
     * @param codigo Código alfanumérico da peça.
     * @return Objeto Peca se existir, ou null se não encontrada.
     */
    public Peca buscarPorCodigo(String codigo) {
        String sql = "SELECT id, codigo, nome, preco, estoque FROM pecas WHERE codigo = ?;";

        try (Connection conn = ConexaoFactory.obterConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, codigo);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearLinha(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erro na Missão 4 (buscarPorCodigo): " + e.getMessage());
        }

        return null;
    }

    /**
     * MISSÃO 5: BAIXA DE ESTOQUE NO BALCÃO (UPDATE com WHERE)
     *
     * A condição "estoque >= ?" garante que o estoque nunca fique negativo.
     * executeUpdate() devolve o número de linhas alteradas: 0 significa
     * estoque insuficiente ou ID inexistente.
     *
     * @param id Identificador da peça.
     * @param quantidadeRetirada Quantidade a subtrair.
     * @return true se atualizou, false se estoque insuficiente ou ID inexistente.
     */
    public boolean darBaixaEstoque(Long id, int quantidadeRetirada) {
        String sql = "UPDATE pecas SET estoque = estoque - ? WHERE id = ? AND estoque >= ?;";

        try (Connection conn = ConexaoFactory.obterConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, quantidadeRetirada);
            stmt.setLong(2, id);
            stmt.setInt(3, quantidadeRetirada);

            int linhas = stmt.executeUpdate();
            return linhas > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erro na Missão 5 (darBaixaEstoque): " + e.getMessage());
            return false;
        }
    }

    /**
     * Converte a linha atual do ResultSet em um objeto Peca, lendo as colunas
     * pelo NOME (resiliente a mudanças na ordem do SELECT).
     */
    private Peca mapearLinha(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String codigo = rs.getString("codigo");
        String nome = rs.getString("nome");
        double preco = rs.getDouble("preco");
        int estoque = rs.getInt("estoque");

        return new Peca(id, codigo, nome, preco, estoque);
    }
}