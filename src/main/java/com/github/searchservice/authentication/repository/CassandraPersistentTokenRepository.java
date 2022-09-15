package com.github.searchservice.authentication.repository;

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

@Slf4j
@RequiredArgsConstructor
@Repository
public class CassandraPersistentTokenRepository implements PersistentTokenRepository {

    public static final String PERSISTENT_LOGINS_CREATE_TABLE_CQL = """
            CREATE TABLE IF NOT EXISTS persistent_logins (
                series_id text PRIMARY KEY,
                username text,
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
           INSERT INTO persistent_logins (username, series_id, security_token, last_used)
           VALUES (?,?,?,?) USING TTL 86400
           """;

    public static final String PERSISTENT_LOGINS_UPDATE_TOKEN_CQL =
            "UPDATE persistent_logins USING TTL 86400 SET security_token = ?, last_used = ? WHERE series_id = ?";

    @Value("${security.create-token-table}")
    private boolean createTableOnStartup;

    @Value("${security.drop-token-table}")
    private boolean dropTableOnShutdown;

    @Autowired
    private final CqlTemplate cqlTemplate;

    @PostConstruct
    public void createTableOnStartup() {
        if (createTableOnStartup) {
            cqlTemplate.execute(PERSISTENT_LOGINS_CREATE_TABLE_CQL);
            log.info("Security table created");
        }
    }

    @PreDestroy
    public void dropTableOnShutdown() {
        if (dropTableOnShutdown) {
            cqlTemplate.execute(PERSISTENT_LOGINS_DROP_TABLE_CQL);
            log.info("Security table dropped");
        }
    }

    @Override
    public void createNewToken(PersistentRememberMeToken token) {
        cqlTemplate.execute(
                PERSISTENT_LOGINS_INSERT_TOKEN_CQL,
                token.getUsername(),
                token.getSeries(),
                token.getTokenValue(),
                token.getDate());
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
        // Remove automatically by using TTL
    }

}

