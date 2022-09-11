package com.github.searchservice.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.core.convert.CassandraCustomConversions;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import org.springframework.lang.NonNull;

import java.util.List;

@Configuration
@EnableCassandraRepositories(basePackages = { "com.github.searchservice.authentication.repository" })
public class DbConfiguration extends AbstractCassandraConfiguration {

    @Value("${spring.data.cassandra.keyspace-name}")
    private String keyspace;

    @Override
    protected String getKeyspaceName() {
        return keyspace;
    }

    @Override
    public @NonNull CassandraCustomConversions customConversions() {
        return new CassandraCustomConversions(
                List.of(
                        new AuthoritiesReadConverter(),
                        new TimestampReadConverter(),
                        new TimestampWriteConverter()
                )
        );
    }
}
