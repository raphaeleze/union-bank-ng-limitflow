package com.limitflow.backend;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bridges zonky's embedded-database-spring-test (JDBC-only — no R2DBC support, see
 * https://github.com/zonkyio/embedded-database-spring-test/issues/121 — and a random port per
 * test run) to R2DBC: reads the actual host/port/database/user the embedded instance's JDBC
 * DataSource ended up with, and builds a matching R2DBC ConnectionFactory from it, overriding the
 * static {@code spring.r2dbc.url} default that only resolves outside tests.
 */
@TestConfiguration
public class EmbeddedR2dbcConfig {

    private static final Pattern JDBC_URL_PATTERN = Pattern.compile("jdbc:postgresql://([^:/]+):(\\d+)/([^?]+)");

    @Bean
    @Primary
    public ConnectionFactory testConnectionFactory(DataSource dataSource) throws Exception {
        String jdbcUrl;
        String username;
        try (Connection connection = dataSource.getConnection()) {
            jdbcUrl = connection.getMetaData().getURL();
            username = connection.getMetaData().getUserName();
        }
        Matcher matcher = JDBC_URL_PATTERN.matcher(jdbcUrl);
        if (!matcher.matches()) {
            throw new IllegalStateException("Unexpected embedded JDBC URL shape: " + jdbcUrl);
        }

        return ConnectionFactories.get(ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.DRIVER, "postgresql")
                .option(ConnectionFactoryOptions.HOST, matcher.group(1))
                .option(ConnectionFactoryOptions.PORT, Integer.parseInt(matcher.group(2)))
                .option(ConnectionFactoryOptions.DATABASE, matcher.group(3))
                .option(ConnectionFactoryOptions.USER, username)
                .option(ConnectionFactoryOptions.PASSWORD, "")
                .build());
    }
}
