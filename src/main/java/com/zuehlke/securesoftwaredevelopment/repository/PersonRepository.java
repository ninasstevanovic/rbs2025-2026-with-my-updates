package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.config.Entity;
import com.zuehlke.securesoftwaredevelopment.domain.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class PersonRepository {

    private static final Logger LOG = LoggerFactory.getLogger(PersonRepository.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(PersonRepository.class);

    private final DataSource dataSource;

    public PersonRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Person> getAll() {
        List<Person> personList = new ArrayList<>();
        String query = "SELECT id, firstName, lastName, email FROM persons";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
                personList.add(createPersonFromResultSet(rs));
            }
        } catch (SQLException e) {
            LOG.error("Database error while fetching all people", e);
            throw new RuntimeException("Failed to fetch people", e);
        }
        return personList;
    }

    public List<Person> search(String searchTerm) {
        List<Person> personList = new ArrayList<>();
        String query = "SELECT id, firstName, lastName, email FROM persons WHERE UPPER(firstName) like UPPER('%" + searchTerm + "%')" +
                " OR UPPER(lastName) like UPPER('%" + searchTerm + "%')";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
                personList.add(createPersonFromResultSet(rs));
            }
        } catch (SQLException e) {
            LOG.error("Database error while fetching people by term. searchTerm={}", searchTerm, e);
            throw new RuntimeException("Failed to fetch people", e);
        }
        return personList;
    }

    public Person get(String personId) {
        String query = "SELECT id, firstName, lastName, email FROM persons WHERE id = " + personId;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            if (rs.next()) {
                return createPersonFromResultSet(rs);
            }

            LOG.warn("Person not found. personId={}", personId);
            return null;
        } catch (SQLException e) {
            LOG.error("Database error while fetching person by id. personId={}", personId, e);
            throw new RuntimeException("Failed to fetch person by id", e);
        }
    }

    public void delete(int personId) {
        String query = "DELETE FROM persons WHERE id = " + personId;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            int rows = statement.executeUpdate(query);
            if (rows == 0) {
                LOG.warn("Attempt to delete non-existing person. personId={}", personId);
                return;
            }

            auditLogger.audit("Deleted person id=" + personId);
            LOG.info("Person deleted successfully. personId={}", personId);
        } catch (SQLException e) {
            LOG.error("Database error while deleting person. personId={}", personId, e);
            throw new RuntimeException("Failed to delete person", e);
        }
    }

    private Person createPersonFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt(1);
        String firstName = rs.getString(2);
        String lastName = rs.getString(3);
        String email = rs.getString(4);
        return new Person("" + id, firstName, lastName, email);
    }

    public void update(Person personUpdate) {
        Person personFromDb = get(personUpdate.getId());
        if (personFromDb == null) {
            LOG.warn("Attempt to update non-existing person. personId={}", personUpdate.getId());
            return;
        }
        String query = "UPDATE persons SET firstName = ?, lastName = '" + personUpdate.getLastName() + "', email = ? where id = " + personUpdate.getId();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
        ) {
            String firstName = personUpdate.getFirstName() != null ? personUpdate.getFirstName() : personFromDb.getFirstName();
            String email = personUpdate.getEmail() != null ? personUpdate.getEmail() : personFromDb.getEmail();
            statement.setString(1, firstName);
            statement.setString(2, email);
            int rows = statement.executeUpdate();
            if (rows == 0) {
                LOG.warn("Person update affected no rows. personId={}", personUpdate.getId());
                return;
            }

            auditLogger.audit(
                    "Updated person id=" + personUpdate.getId()
                            + ", oldFirstName=" + personFromDb.getFirstName()
                            + ", newFirstName=" + firstName
                            + ", oldLastName=" + personFromDb.getLastName()
                            + ", newLastName=" + personUpdate.getLastName()
                            + ", oldEmail=" + personFromDb.getEmail()
                            + ", newEmail=" + email
            );

            LOG.info("Person updated successfully. personId={}", personUpdate.getId());
        } catch (SQLException e) {
            LOG.error("Database error while updating person. personId={}", personUpdate.getId(), e);
            throw new RuntimeException("Failed to update person", e);
        }
    }
}
