package com.github.searchservice.authentication.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.cassandra.core.cql.CqlOperations;
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
    public static final String CREATE_TABLE_CQL = "create table if not exists persistent_logins (username text, series_id text primary key, "
            + "security_token text, last_used timestamp)";

    public static final String DROP_TABLE_CQL = "drop table if exists persistent_logins";

    public static final String SELECT_TOKEN_BY_SERIES_CQL = "select username,series_id,security_token,last_used from persistent_logins where series_id = ?";
    public static final String INSERT_TOKEN_CQL = "insert into persistent_logins (username, series_id, security_token, last_used) values(?,?,?,?)";
    public static final String UPDATE_TOKEN_CQL = "update persistent_logins set security_token = ?, last_used = ? where series_id = ?";
    public static final String REMOVE_USER_TOKENS_CQL = "delete from persistent_logins where username = ?";

    @Value("${security.create-token-table}")
    private boolean createTableOnStartup;

    @Value("${security.drop-token-table}")
    private boolean dropTableOnShutdown;

    @Autowired
    private final CqlOperations cqlTemplate;

    @PostConstruct
    public void createTableOnStartup() {
        if (createTableOnStartup) {
            cqlTemplate.execute(CREATE_TABLE_CQL);
            log.info("Security token table persistent_logins created");
        }
    }

    @PreDestroy
    public void dropTableOnShutdown() {
        if (dropTableOnShutdown) {
            cqlTemplate.execute(DROP_TABLE_CQL);
            log.info("Security token table persistent_logins dropped");
        }
    }

    @Override
    public void createNewToken(PersistentRememberMeToken token) {
        cqlTemplate.execute(INSERT_TOKEN_CQL, token.getUsername(), token.getSeries(), token.getTokenValue(), token.getDate());
    }

    @Override
    public void updateToken(String series, String tokenValue, Date lastUsed) {
        cqlTemplate.execute(UPDATE_TOKEN_CQL, tokenValue, lastUsed, series);
    }

    @Override
    public PersistentRememberMeToken getTokenForSeries(String seriesId) {
        try {
            return cqlTemplate.queryForObject(SELECT_TOKEN_BY_SERIES_CQL, PersistentRememberMeToken.class, seriesId);
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
        cqlTemplate.execute(REMOVE_USER_TOKENS_CQL, username);
    }
}

