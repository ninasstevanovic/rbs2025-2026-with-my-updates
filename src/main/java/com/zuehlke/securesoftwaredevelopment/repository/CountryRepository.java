package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.domain.Country;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CountryRepository {
    private static final Logger LOG = LoggerFactory.getLogger(CountryRepository.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(CountryRepository.class);
    private final DataSource dataSource;

    public CountryRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Country> getAll() {
        List<Country> countryList = new ArrayList<>();
        String query = "SELECT c.id, c.name FROM country as c";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
                int id = rs.getInt(1);
                String name = rs.getString(2);
                countryList.add(new Country(id, name));
            }

            return countryList;
        } catch (SQLException e) {
            LOG.error("Database error while fetching all countries", e);
            throw new RuntimeException("Failed to fetch countries", e);
        }
    }

    public Country findById(Integer countryId) {
        String query = "SELECT c.id, c.name FROM country as c WHERE c.id = " + countryId;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            if (rs.next()) {
                int id = rs.getInt(1);
                String name = rs.getString(2);
                return new Country(id, name);
            }

            LOG.warn("City not found. countryId={}", countryId);
            return null;
        } catch (SQLException e) {
            LOG.error("Database error while fetching city by id. countryId={}", countryId, e);
            throw new RuntimeException("Failed to fetch country by id", e);
        }
    }

    public List<Country> findByName(String name) {
        String query = "SELECT c.id FROM country as c WHERE c.name like '" + name + "'";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            List<Country> countryList = new ArrayList<>();
            while (rs.next()) {
                int id = rs.getInt(1);
                countryList.add(new Country(id, name));
            }

            return countryList;
        } catch (SQLException e) {
            LOG.error("Database error while searching country by name. name={}", name, e);
            throw new RuntimeException("Failed to search country by name", e);
        }
    }

    public long create(Country country) {
        String query = "INSERT INTO country(name) VALUES('" + country.getName() + "')";
        long id = 0;

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
        ) {
            int rows = statement.executeUpdate(query);

            if (rows == 0) {
                LOG.warn("Country creation affected no rows. countryName={}", country.getName());
                throw new SQLException("Creating country failed, no rows affected.");
            }

            auditLogger.audit("Created country id=" + id + ", name=" + country.getName());
            LOG.info("Country created successfully. id={}, name={}", id, country.getName());

            return id;
        } catch (SQLException e) {
            LOG.error("Database error while creating country. countryName={}, countryId={}", country.getName(), country.getId(), e);
            throw new RuntimeException("Failed to create country", e);
        }
    }
}
