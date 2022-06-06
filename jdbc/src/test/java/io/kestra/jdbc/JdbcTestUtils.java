package io.kestra.jdbc;

import io.micronaut.flyway.FlywayConfigurationProperties;
import io.micronaut.flyway.FlywayMigrator;
import lombok.SneakyThrows;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.impl.DSL;

import javax.sql.DataSource;

import static io.kestra.core.utils.Rethrow.throwPredicate;

@Singleton
public class JdbcTestUtils {
    @Inject
    private DSLContextWrapper dslContextWrapper;

    @Inject
    private FlywayMigrator flywayMigrator;

    @Inject
    private FlywayConfigurationProperties config;

    @Inject
    private DataSource dataSource;

    @SneakyThrows
    public void drop() {
        dslContextWrapper.transaction((configuration) -> {
            DSLContext dslContext = DSL.using(configuration);

            dslContext
                .meta()
                .getTables()
                .stream()
                .filter(throwPredicate(table ->
                    (configuration.dialect() == SQLDialect.MYSQL && table.getSchema().getName().equals(dataSource.getConnection().getCatalog())) ||
                        configuration.dialect() != SQLDialect.MYSQL
                ))
                .filter(table -> !table.getName().equals("flyway_schema_history"))
                .forEach(t -> dslContext.truncate(t.getName()).execute());
        });
    }

    public void migrate() {
        dslContextWrapper.transaction((configuration) -> {
            flywayMigrator.run(config, dataSource);
        });
    }
}
