package org.apache.empire.springboot;

import jakarta.annotation.PreDestroy;
import org.apache.empire.db.context.DBContextBase;
import org.apache.empire.db.context.DBRollbackManager;
import org.apache.empire.dbms.DBMSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;

public class DBContextSpring extends DBContextBase implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DBContextSpring.class);

    private final DataSource dataSource;
    private final DBMSHandler dbmsHandler;

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
     * @param readOnly if true, the connection will be set to read-only mode. (ignored)
     * @return a Connection object that is managed by Spring's transaction management.
     */
    @Override
    protected Connection getConnection(boolean readOnly) {
        return DataSourceUtils.getConnection(dataSource);
    }

    @Override
    protected DBRollbackManager getRollbackManager(boolean b) {
        return null;
    }

    @Override
    public Connection getConnection() {
        return getConnection(false);
    }

    @Override
    public boolean isPreparedStatementsEnabled() {
        return true;
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
        // No-op: Let Spring's TransactionManager handle the connection lifecycle
    }

    @Override
    public DBMSHandler getDbms() {
        return this.dbmsHandler;
    }

    @Override
    public boolean isRollbackHandlingEnabled() {
        return false;
    }

    @PreDestroy
    @Override
    public void close() {
        closeConnection();
    }
}
