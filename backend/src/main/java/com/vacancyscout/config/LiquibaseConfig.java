package com.vacancyscout.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import liquibase.integration.spring.SpringLiquibase;
import javax.sql.DataSource;

@Configuration
public class LiquibaseConfig {
    @Bean
    public SpringLiquibase liquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:db/changelog/db.changelog-master.xml");
        return liquibase;
    }
}
