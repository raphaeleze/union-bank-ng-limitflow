package com.limitflow.backend.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Flyway needs a JDBC DataSource, but Spring Boot's own DataSourceAutoConfiguration backs off
 * whenever an R2DBC ConnectionFactory bean is present ({@code @ConditionalOnMissingBean}) — which
 * this app always has, since R2DBC is its actual data-access mechanism. Defining the DataSource
 * bean explicitly here sidesteps that guard, so Flyway's migrations still run in every
 * environment. This is also the exact bean zonky's {@code @AutoConfigureEmbeddedDatabase}
 * intercepts and replaces in tests, the same as it would an auto-configured one.
 */
@Configuration
public class FlywayDataSourceConfig {

    @Bean
    public DataSource dataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .build();
    }
}
