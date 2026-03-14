package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.domain.RoomType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RoomRepository {
    private static final Logger LOG = LoggerFactory.getLogger(RoomRepository.class);

    private final DataSource dataSource;

    public RoomRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<RoomType> getAllRoomTypes(int hotelId) {
        List<RoomType> roomTypes = new ArrayList<>();
        String query = "SELECT id, name, capacity, pricePerNight, totalRooms FROM roomType WHERE hotelId = ?";

        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, hotelId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Integer id = rs.getInt(1);
                    String name = rs.getString(2);
                    int capacity = rs.getInt(3);
                    BigDecimal pricePerNight = rs.getBigDecimal(4);
                    int totalRooms = rs.getInt(5);

                    roomTypes.add(new RoomType(id, hotelId, name, capacity, pricePerNight, totalRooms));
                }
            }

            return roomTypes;
        } catch (SQLException e) {
            LOG.error("Database error while fetching room types for hotelId={}", hotelId, e);
            throw new RuntimeException("Failed to fetch room types", e);
        }
    }

    public RoomType findByIdAndHotelId(int roomTypeId, int hotelId) {
        String query = "SELECT name, capacity, pricePerNight, totalRooms FROM roomType WHERE id = ? AND hotelId = ?";

        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, roomTypeId);
            statement.setInt(2, hotelId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString(1);
                    int capacity = rs.getInt(2);
                    BigDecimal pricePerNight = rs.getBigDecimal(3);
                    int totalRooms = rs.getInt(4);

                    RoomType roomType = new RoomType();
                    roomType.setId(roomTypeId);
                    roomType.setHotelId(hotelId);
                    roomType.setName(name);
                    roomType.setCapacity(capacity);
                    roomType.setPricePerNight(pricePerNight);
                    roomType.setTotalRooms(totalRooms);

                    return roomType;
                }
            }

            LOG.warn("Room type not found. roomTypeId={}, hotelId={}", roomTypeId, hotelId);
            return null;
        } catch (SQLException e) {
            LOG.error("Database error while fetching room type. roomTypeId={}, hotelId={}", roomTypeId, hotelId, e);
            throw new RuntimeException("Failed to fetch room type", e);
        }
    }
}