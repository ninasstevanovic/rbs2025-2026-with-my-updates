package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.domain.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RatingRepository {

    private static final Logger LOG = LoggerFactory.getLogger(RatingRepository.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(RatingRepository.class);

    private final DataSource dataSource;

    public RatingRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void createOrUpdate(Rating rating) {
        String selectQuery = "SELECT hotelId, userId, rating FROM ratings WHERE hotelId = ? AND userId = ?";
        String updateQuery = "UPDATE ratings SET rating = ? WHERE hotelId = ? AND userId = ?";
        String insertQuery = "INSERT INTO ratings(hotelId, userId, rating) VALUES (?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement selectStatement = connection.prepareStatement(selectQuery)) {

            selectStatement.setInt(1, rating.getHotelId());
            selectStatement.setInt(2, rating.getUserId());

            try (ResultSet rs = selectStatement.executeQuery()) {
                if (rs.next()) {
                    int oldRating = rs.getInt(3);

                    try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                        updateStatement.setInt(1, rating.getRating());
                        updateStatement.setInt(2, rating.getHotelId());
                        updateStatement.setInt(3, rating.getUserId());

                        int rows = updateStatement.executeUpdate();

                        if (rows == 0) {
                            LOG.warn("Rating update affected no rows. hotelId={}, userId={}, newRating={}",
                                    rating.getHotelId(), rating.getUserId(), rating.getRating());
                            return;
                        }

                        auditLogger.audit(
                                "Updated rating for hotelId=" + rating.getHotelId()
                                        + ", userId=" + rating.getUserId()
                                        + ", oldRating=" + oldRating
                                        + ", newRating=" + rating.getRating()
                        );

                        LOG.info("Rating updated successfully. hotelId={}, userId={}, oldRating={}, newRating={}",
                                rating.getHotelId(), rating.getUserId(), oldRating, rating.getRating());
                    }
                } else {
                    try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                        insertStatement.setInt(1, rating.getHotelId());
                        insertStatement.setInt(2, rating.getUserId());
                        insertStatement.setInt(3, rating.getRating());

                        int rows = insertStatement.executeUpdate();

                        if (rows == 0) {
                            LOG.warn("Rating insert affected no rows. hotelId={}, userId={}, rating={}",
                                    rating.getHotelId(), rating.getUserId(), rating.getRating());
                            return;
                        }

                        auditLogger.audit(
                                "Created rating for hotelId=" + rating.getHotelId()
                                        + ", userId=" + rating.getUserId()
                                        + ", rating=" + rating.getRating()
                        );

                        LOG.info("Rating created successfully. hotelId={}, userId={}, rating={}",
                                rating.getHotelId(), rating.getUserId(), rating.getRating());
                    }
                }
            }
        } catch (SQLException e) {
            LOG.error("Database error while creating/updating rating. hotelId={}, userId={}, rating={}",
                    rating.getHotelId(), rating.getUserId(), rating.getRating(), e);
            throw new RuntimeException("Failed to create or update rating", e);
        }
    }

    public List<Rating> getAll(String hotelId) {
        List<Rating> ratingList = new ArrayList<>();
        String query = "SELECT hotelId, userId, rating FROM ratings WHERE hotelId = " + hotelId;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
                ratingList.add(new Rating(rs.getInt(1), rs.getInt(2), rs.getInt(3)));
            }
        } catch (SQLException e) {
            LOG.error("Database error while fetching ratings for hotelId={}", hotelId, e);
            throw new RuntimeException("Failed to fetch ratings", e);
        }
        return ratingList;
    }
}