package org.apache.empire.samples.springboot;

import org.apache.empire.db.DBContext;
import org.apache.empire.springboot.EmpireDBConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(EmpireDBConfig.class)
public class SampleDBConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleDBConfig.class);

    @Bean
    public SampleDB empireDatabase(DBContext context) {
        SampleDB db = new SampleDB();
        db.open(context);
        if (context instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                LOGGER.warn("Failed to close Empire DB context after initialization", e);
            }
        }
        return db;
    }
}
