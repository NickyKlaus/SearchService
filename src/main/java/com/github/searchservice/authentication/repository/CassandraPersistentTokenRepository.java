package com.github.searchservice.authentication.repository;

import com.datastax.oss.driver.api.core.cql.BatchStatementBuilder;
import com.datastax.oss.driver.api.core.cql.BatchType;
import com.datastax.oss.driver.api.core.cql.BatchableStatement;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.github.searchservice.authentication.model.PersistentRememberMeUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.cassandra.core.cql.CqlTemplate;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Date;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Repository
public class CassandraPersistentTokenRepository implements PersistentTokenRepository {

    public static final String PERSISTENT_USERS_CREATE_TABLE_CQL = """
                CREATE TABLE IF NOT EXISTS persistent_users (
                    username text PRIMARY KEY,
                    series_id set<text>)
                """;

    public static final String PERSISTENT_USERS_DROP_TABLE_CQL = "DROP TABLE IF EXISTS persistent_users";

    public static final String PERSISTENT_USERS_SELECT_SERIES_BY_USERNAME_CQL = """
           SELECT username, series_id
           FROM persistent_users
           WHERE username = ?
           """;

    public static final String PERSISTENT_USERS_INSERT_USER_CQL = "INSERT persistent_users SET username = ?, series_id = series_id + { ? } WHERE ";

    public static final String PERSISTENT_USERS_REMOVE_USER_CQL = "DELETE FROM persistent_users WHERE username = ?";

    public static final String PERSISTENT_LOGINS_CREATE_TABLE_CQL = """
            CREATE TABLE IF NOT EXISTS persistent_logins (
                username text,
                series_id text PRIMARY KEY,
                security_token text,
                last_used timestamp)
            """;

    public static final String PERSISTENT_LOGINS_DROP_TABLE_CQL = "DROP TABLE IF EXISTS persistent_logins";

    public static final String PERSISTENT_LOGINS_SELECT_TOKEN_BY_SERIES_CQL = """
           SELECT username, series_id, security_token, last_used
           FROM persistent_logins
           WHERE series_id = ?
           """;

    public static final String PERSISTENT_LOGINS_INSERT_TOKEN_CQL = """
           INSERT INTO persistent_logins (
               username, series_id, security_token, last_used)
           VALUES (?,?,?,?)
           """;

    public static final String PERSISTENT_LOGINS_UPDATE_TOKEN_CQL = "UPDATE persistent_logins SET security_token = ?, last_used = ? WHERE series_id = ?";

    public static final String PERSISTENT_LOGINS_REMOVE_TOKEN_CQL = "DELETE FROM persistent_logins WHERE series_id = ?";

    @Value("${security.create-token-table}")
    private boolean createTableOnStartup;

    @Value("${security.drop-token-table}")
    private boolean dropTableOnShutdown;

    @Autowired
    private final CqlTemplate cqlTemplate;

    @PostConstruct
    public void createTablesOnStartup() {
        if (createTableOnStartup) {
            cqlTemplate.execute(PERSISTENT_USERS_CREATE_TABLE_CQL);
            cqlTemplate.execute(PERSISTENT_LOGINS_CREATE_TABLE_CQL);
            log.info("Security tables created");
        }
    }

    @PreDestroy
    public void dropTablesOnShutdown() {
        if (dropTableOnShutdown) {
            cqlTemplate.execute(PERSISTENT_USERS_DROP_TABLE_CQL);
            cqlTemplate.execute(PERSISTENT_LOGINS_DROP_TABLE_CQL);
            log.info("Security tables dropped");
        }
    }

    @Override
    public void createNewToken(PersistentRememberMeToken token) {
        var batchStatement =
                new BatchStatementBuilder(BatchType.LOGGED)
                        .addStatements(
                                SimpleStatement.newInstance(
                                        PERSISTENT_LOGINS_INSERT_TOKEN_CQL,
                                        token.getUsername(),
                                        token.getSeries(),
                                        token.getTokenValue(),
                                        token.getDate()),
                                SimpleStatement.newInstance(
                                        PERSISTENT_USERS_INSERT_USER_CQL,
                                        token.getUsername(),
                                        token.getSeries()))
                        .build();
        cqlTemplate.execute(batchStatement);
    }

    @Override
    public void updateToken(String series, String tokenValue, Date lastUsed) {
        cqlTemplate.execute(PERSISTENT_LOGINS_UPDATE_TOKEN_CQL, tokenValue, lastUsed, series);
    }

    @Override
    public PersistentRememberMeToken getTokenForSeries(String seriesId) {
        try {
            return cqlTemplate.queryForObject(
                    PERSISTENT_LOGINS_SELECT_TOKEN_BY_SERIES_CQL,
                    PersistentRememberMeToken.class,
                    seriesId);
        } catch (EmptyResultDataAccessException ex) {
            log.debug(String.format("Querying token for series '%s' returned no results.", seriesId), ex);
        } catch (IncorrectResultSizeDataAccessException ex) {
            log.error(String.format(
                    "Querying token for series '%s' returned more than one value. Series" + " should be unique",
                    seriesId));
        } catch (DataAccessException ex) {
            log.error("Failed to load token for series " + seriesId, ex);
        }
        return null;
    }

    @Override
    public void removeUserTokens(String username) {
        var user = getRememberMeUserForUsername(username);
        if (user.isPresent()) {
            var batchStatement =
                    new BatchStatementBuilder(BatchType.LOGGED)
                            .addStatements(
                                    user.get().series().stream()
                                            .map(seriesId -> SimpleStatement.newInstance(
                                                    PERSISTENT_LOGINS_REMOVE_TOKEN_CQL, seriesId))
                                            .toArray(BatchableStatement[]::new))

                            .addStatement(SimpleStatement.newInstance(PERSISTENT_USERS_REMOVE_USER_CQL, user.get().username()))
                            .build();

            cqlTemplate.execute(batchStatement);
        }
    }

    protected Optional<PersistentRememberMeUser> getRememberMeUserForUsername(String username) {
        try {
            return Optional.of(
                    cqlTemplate.queryForObject(
                            PERSISTENT_USERS_SELECT_SERIES_BY_USERNAME_CQL,
                            PersistentRememberMeUser.class,
                            username));
        } catch (EmptyResultDataAccessException ex) {
            log.debug(String.format("Querying user for username '%s' returned no results.", username), ex);
        } catch (DataAccessException ex) {
            log.error("Failed to load user for username " + username, ex);
        }
        return Optional.empty();
    }

}

