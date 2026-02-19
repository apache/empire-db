package org.apache.empire.springboot;

import jakarta.annotation.PreDestroy;
import org.apache.empire.db.context.DBContextBase;
import org.apache.empire.db.context.DBRollbackManager;
import org.apache.empire.db.exceptions.EmpireSQLException;
import org.apache.empire.dbms.DBMSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DBContextSpring extends DBContextBase implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DBContextSpring.class);

    private final DataSource dataSource;
    private final DBMSHandler dbmsHandler;

    private final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    public DBContextSpring(DataSource dataSource, DBMSHandler dbmsHandler) {
        this.dataSource = dataSource;
        this.dbmsHandler = dbmsHandler;
    }

    /**
     * Get a connection from the Spring-managed DataSource.
     * <p>
     * the Spring Boot autoconfigured DataSource provides connections that work with @Transactional, provided Spring manages the transaction.
     * Key points:
     * Spring binds the Connection to the transaction (e.g., DataSourceTransactionManager for JDBC). Repeated access within the same @Transactional method returns the same Connection.
     * Use Spring abstractions (e.g., JdbcTemplate, NamedParameterJdbcTemplate, JPA/EntityManager) or DataSourceUtils.getConnection(...). Then you are transaction-aware automatically.
     * If you call dataSource.getConnection() directly you can bypass Spring's transaction binding — that Connection won’t automatically participate in the @Transactional transaction.
     * Hikari provides proxy connections (pooling, auto-commit handling) and is compatible with Spring transactions.
     * Do not call conn.setAutoCommit(false) manually when Spring manages the transaction.
     * <p>
     * Summary: Spring Boot provided DataSources work with @Transactional as long as you use Spring’s access paths or DataSourceUtils and have the appropriate TransactionManager active.
     *
     * @param readOnly if true, the connection will be set to read-only mode.
     * @return a Connection object that is managed by Spring's transaction management.
     */
    @Override
    protected Connection getConnection(boolean readOnly) {
        Connection conn = connectionHolder.get();
        try {
            if (conn == null || conn.isClosed()) {
                conn = DataSourceUtils.getConnection(dataSource);
                if (readOnly) {
                    conn.setReadOnly(true);
                }
                connectionHolder.set(conn);
                LOGGER.debug("Obtained Spring-managed connection {}", conn);
            }
            return conn;
        } catch (SQLException e) {
            throw new EmpireSQLException(dbmsHandler, e);
        }
    }

    @Override
    public Connection getConnection()
    {
        return getConnection(false);
    }

    @Override
    protected DBRollbackManager getRollbackManager(boolean b) {
        return null;
    }

    @Override
    public boolean isPreparedStatementsEnabled() {
        return true;
    }

    @Override
    public DBMSHandler getDbms() {
        return this.dbmsHandler;
    }

    @Override
    public boolean isRollbackHandlingEnabled() {
        return false;
    }

    @Override
    public void commit() {
        // No-op: Let Spring's TransactionManager handle the commit
    }

    @Override
    public void rollback() {
        // No-op: Let Spring's TransactionManager handle the rollback
    }

    @Override
    protected void closeConnection() {
        Connection conn = connectionHolder.get();
        connectionHolder.remove();
        if (conn != null) {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    @PreDestroy
    @Override
    public void close() {
        closeConnection();
    }
}
