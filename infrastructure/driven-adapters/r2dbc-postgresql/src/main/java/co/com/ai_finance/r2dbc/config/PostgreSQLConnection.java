package co.com.ai_finance.r2dbc.config;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Objects;

@Configuration
@EnableTransactionManagement
@EnableR2dbcRepositories(basePackages = "co.com.ai_finance.r2dbc.repository")
public class PostgreSQLConnection {

    private final PostgresqlConnectionProperties properties;

    public PostgreSQLConnection(PostgresqlConnectionProperties properties) {
        this.properties = properties;
    }

    @Bean
    public PostgresqlConnectionFactory postgresConnectionFactory() {
        PostgresqlConnectionConfiguration.Builder builder = PostgresqlConnectionConfiguration.builder()
                .host(properties.getHost())
                .database(properties.getDatabase())
                .username(properties.getUsername())
                .password(properties.getPassword())
                .port(properties.getPort());

        if (Objects.nonNull(properties.getSchema()) && !properties.getSchema().isEmpty()) {
            builder.schema(properties.getSchema());
        }

        return new PostgresqlConnectionFactory(builder.build());
    }

    @Bean(destroyMethod = "dispose")
    public ConnectionPool connectionPool() {
        ConnectionPoolConfiguration.Builder builder = ConnectionPoolConfiguration.builder(postgresConnectionFactory())
                .maxSize(properties.getMaxSize())
                .initialSize(properties.getPoolSize())
                .maxIdleTime(Duration.ofSeconds(properties.getMaxIdleTime()));

        if (StringUtils.hasText(properties.getValidationQuery())) {
            builder.validationQuery(properties.getValidationQuery());
        }

        return new ConnectionPool(builder.build());
    }


    @Bean("r2dbcTransactionManager")
    public R2dbcTransactionManager transactionManager(ConnectionPool connectionPool) {
        return new R2dbcTransactionManager(connectionPool);
    }

    @Bean
    public TransactionalOperator transactionalOperator(ReactiveTransactionManager r2dbcTransactionManager) {
        return TransactionalOperator.create(r2dbcTransactionManager);
    }


    @Bean
    public DatabaseClient databaseClient(ConnectionPool connectionPool) {
        return DatabaseClient.builder()
                .connectionFactory(connectionPool)
                .namedParameters(true)
                .build();
    }

}