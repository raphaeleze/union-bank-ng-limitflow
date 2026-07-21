package com.limitflow.backend;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.reactivestreams.Publisher;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bridges zonky's embedded-database-spring-test (JDBC-only — no R2DBC support, see
 * https://github.com/zonkyio/embedded-database-spring-test/issues/121 — and a random port per
 * test run) to R2DBC: reads the actual host/port/database/user the embedded instance's JDBC
 * DataSource ended up with, and builds a matching R2DBC ConnectionFactory from it, overriding the
 * static {@code spring.r2dbc.url} default that only resolves outside tests.
 *
 * <p>The lookup is deferred to each {@link ConnectionFactory#create()} call rather than done once
 * when this bean is built: zonky's {@code refresh = BEFORE_EACH_TEST_METHOD} swaps the JDBC
 * DataSource to point at a freshly-created, randomly-named database before every test method,
 * behind the same {@link DataSource} bean. A {@link ConnectionFactory} resolved once at
 * bean-creation time — and then cached, since this bean is a singleton — would keep pointing at
 * whichever database existed at startup and fail with "database does not exist" once the first
 * refresh swaps it out from under it.
 *
 * <p>The JDBC lookup (getConnection/getMetaData) is a genuinely blocking call, so it's wrapped in
 * {@code subscribeOn(Schedulers.boundedElastic())} rather than run inline: without that, it
 * executes directly on the Reactor Netty event-loop thread that triggered it, blocking that loop
 * and causing exactly the kind of unpredictable request-handling behavior non-blocking servers
 * are supposed to avoid.
 */
@TestConfiguration
public class EmbeddedR2dbcConfig {

    private static final Pattern JDBC_URL_PATTERN = Pattern.compile("jdbc:postgresql://([^:/]+):(\\d+)/([^?]+)");

    @Bean
    @Primary
    public ConnectionFactory testConnectionFactory(DataSource dataSource) {
        return new RefreshingConnectionFactory(dataSource);
    }

    private static ConnectionFactory resolveCurrent(DataSource dataSource) throws SQLException {
        String jdbcUrl;
        String username;
        try (java.sql.Connection connection = dataSource.getConnection()) {
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

    private record RefreshingConnectionFactory(DataSource dataSource) implements ConnectionFactory {

        @Override
        public Publisher<? extends Connection> create() {
            return Mono.fromCallable(() -> resolveCurrent(dataSource))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(factory -> Mono.from(factory.create()));
        }

        @Override
        public ConnectionFactoryMetadata getMetadata() {
            try {
                return resolveCurrent(dataSource).getMetadata();
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
