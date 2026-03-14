package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.domain.City;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CityRepository {
    private static final Logger LOG = LoggerFactory.getLogger(CityRepository.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(CityRepository.class);
    private final DataSource dataSource;

    public CityRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<City> getAll() {
        List<City> cityList = new ArrayList<>();
        String query = "SELECT c.id, c.countryId, c.name, ct.name FROM city as c, country as ct WHERE ct.id = c.countryId";
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
                int id = rs.getInt(1);
                int countryId = rs.getInt(2);
                String name = rs.getString(3);
                String countryName = rs.getString(4);

                cityList.add(new City(id, countryId, name, countryName));
            }
        } catch (SQLException e) {
            LOG.error("Database error while fetching all cities", e);
            throw new RuntimeException("Failed to fetch cities", e);
        }

        return cityList;
    }

    public City findById(Integer cityId) {
        String query = "SELECT c.id, c.countryId, c.name FROM city as c WHERE c.id = ?";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, cityId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    int countryId = rs.getInt(2);
                    String name = rs.getString(3);
                    return new City(id, countryId, name);
                }
            }

            LOG.warn("City not found. cityId={}", cityId);
            return null;
        } catch (SQLException e) {
            LOG.error("Database error while fetching city by id. cityId={}", cityId, e);
            throw new RuntimeException("Failed to fetch city by id", e);
        }
    }

    public List<City> findByName(String name) {
        String query = "SELECT c.id, c.countryId, c.name FROM city c WHERE c.name = ?";

        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, name);

            try (ResultSet rs = statement.executeQuery()) {
                List<City> cityList = new ArrayList<>();
                while (rs.next()) {
                    int id = rs.getInt(1);
                    int countryId = rs.getInt(2);
                    String cityName = rs.getString(3);

                    cityList.add(new City(id, countryId, cityName));
                }
                return cityList;
            }
        } catch (SQLException e) {
            LOG.error("Database error while searching city by name. name={}", name, e);
            throw new RuntimeException("Failed to search city by name", e);
        }
    }

    public long create(City city) {
        String query = "INSERT INTO city(countryid, name) VALUES(?, ?)";
        long id = -1;

        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, city.getCountryId());
            statement.setString(2, city.getName());

            int rows = statement.executeUpdate();

            if (rows == 0) {
                LOG.warn("City creation affected no rows. cityName={}, countryId={}", city.getName(), city.getCountryId());
                throw new SQLException("Creating city failed, no rows affected.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    id = generatedKeys.getLong(1);
                }
            }

            auditLogger.audit("Created city id=" + id + ", name=" + city.getName() + ", countryId=" + city.getCountryId());
            LOG.info("City created successfully. id={}, name={}, countryId={}", id, city.getName(), city.getCountryId());

            return id;
        } catch (SQLException e) {
            LOG.error("Database error while creating city. cityName={}, countryId={}", city.getName(), city.getCountryId(), e);
            throw new RuntimeException("Failed to create city", e);
        }
    }
}
