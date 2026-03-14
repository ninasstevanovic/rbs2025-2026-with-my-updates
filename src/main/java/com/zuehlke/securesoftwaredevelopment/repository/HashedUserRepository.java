package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.domain.HashedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;

@Repository
public class HashedUserRepository {

    private static final Logger LOG = LoggerFactory.getLogger(HashedUserRepository.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(CityRepository.class);

    private final DataSource dataSource;

    public HashedUserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public HashedUser findUser(String username) {
        String sqlQuery = "select passwordHash, salt, totpKey from hashedUsers where username = '" + username + "'";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sqlQuery)) {
            if (rs.next()) {
                String passwordHash = rs.getString(1);
                String salt = rs.getString(2);
                String totpKey = rs.getString(3);
                return new HashedUser(username, passwordHash, salt, totpKey);
            }

            LOG.warn("Hashed user not found for username={}", username);
            return null;
        } catch (SQLException e) {
            LOG.error("Database error while loading hashed user for username={}", username, e);
            throw new RuntimeException("Failed to load hashed user", e);
        }
    }

    public void saveTotpKey(String username, String totpKey) {
        String sqlQuery = "update hashedUsers set totpKey = ? where username = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sqlQuery)) {
            statement.setString(1, totpKey);
            statement.setString(2, username);

            int rows = statement.executeUpdate();
            if (rows == 0) {
                LOG.warn("TOTP key update affected no rows for username={}", username);
            } else {
                auditLogger.audit("Updated TOTP key for username=" + username);
                LOG.info("TOTP key updated for username={}", username);
            }
        } catch (SQLException e) {
            LOG.error("Database error while updating TOTP key for username={}", username, e);
            throw new RuntimeException("Failed to update TOTP key", e);
        }
    }
}
