package org.apache.empire.springboot;

import org.apache.empire.commons.StringUtils;
import org.apache.empire.db.DBContext;
import org.apache.empire.dbms.DBMSHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;


@Configuration
@EnableConfigurationProperties(EmpireDBConfigProperties.class)
public class EmpireDBConfig {
    private final EmpireDBConfigProperties empireDBConfigProperties;

    public EmpireDBConfig(EmpireDBConfigProperties empireDBConfigProperties) {
        this.empireDBConfigProperties = empireDBConfigProperties;
    }

    @Bean
    public DBContext dbContext(DataSource dataSource) {
        try {
            String dbmsHandlerClass = empireDBConfigProperties.getDbmsHandlerClass();
            if (StringUtils.isEmpty(dbmsHandlerClass)) {
                throw new RuntimeException("Configuration error: Fully qualified DBMS handler class path not found under application property 'empiredb.dbmsHandlerClass'");
            }
            DBMSHandler dbmsHandler = (DBMSHandler) Class.forName(dbmsHandlerClass).getDeclaredConstructor().newInstance();
            return new DBContextSpring(new TransactionAwareDataSourceProxy(dataSource), dbmsHandler);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException("Failed to create DBMS handler for Empire DB", e);
        }
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
