package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;

@Repository
public class UserRepository {

    private static final Logger LOG = LoggerFactory.getLogger(UserRepository.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(UserRepository.class);

    private final DataSource dataSource;

    public UserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public User findUser(String username) {
        String query = "SELECT id, username, password FROM users WHERE username='" + username + "'";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            if (rs.next()) {
                int id = rs.getInt(1);
                String username1 = rs.getString(2);
                String password = rs.getString(3);
                return new User(id, username1, password);
            }

            LOG.warn("User not found for username={}", username);
            return null;
        } catch (SQLException e) {
            LOG.error("Database error while fetching user for username={}", username, e);
            throw new RuntimeException("Failed to fetch user", e);
        }
    }

    public String findUsername(int id) {
        String query = "SELECT username FROM users WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }

            LOG.warn("Username not found for userId={}", id);
            return null;
        } catch (SQLException e) {
            LOG.error("Database error while fetching username for userId={}", id, e);
            throw new RuntimeException("Failed to fetch username", e);
        }
    }

    public void updateUsername(int id, String username) {
        String oldUsername = findUsername(id);
        if (oldUsername == null) {
            LOG.warn("Attempt to update username for non-existing user. userId={}, newUsername={}", id, username);
            return;
        }

        String query = "UPDATE users SET username = ? WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, username);
            statement.setInt(2, id);

            int rows = statement.executeUpdate();

            if (rows == 0) {
                LOG.warn("Username update affected no rows. userId={}, oldUsername={}, newUsername={}", id, oldUsername, username);
                return;
            }

            auditLogger.audit("Updated username for userId=" + id + ", oldUsername=" + oldUsername + ", newUsername=" + username);
            LOG.info("Username updated successfully. userId={}, oldUsername={}, newUsername={}", id, oldUsername, username);
        } catch (SQLException e) {
            LOG.error("Database error while updating username. userId={}, newUsername={}", id, username, e);
            throw new RuntimeException("Failed to update username", e);
        }
    }

    public boolean validCredentials(String username, String password) {
        String query = "SELECT username FROM users WHERE username = ? AND password = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, username);
            statement.setString(2, password);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOG.error("Database error while validating credentials for username={}", username, e);
            throw new RuntimeException("Failed to validate credentials", e);
        }
    }

    public void delete(int userId) {
        String query = "DELETE FROM users WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, userId);
            int rows = statement.executeUpdate();

            if (rows == 0) {
                LOG.warn("Attempt to delete non-existing user. userId={}", userId);
                return;
            }

            auditLogger.audit("Deleted user id=" + userId);
            LOG.info("User deleted successfully. userId={}", userId);
        } catch (SQLException e) {
            LOG.error("Database error while deleting user. userId={}", userId, e);
            throw new RuntimeException("Failed to delete user", e);
        }
    }
}