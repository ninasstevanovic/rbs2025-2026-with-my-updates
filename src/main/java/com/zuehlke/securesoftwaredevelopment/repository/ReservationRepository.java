package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.domain.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ReservationRepository {
    private static final Logger LOG = LoggerFactory.getLogger(ReservationRepository.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(ReservationRepository.class);

    private final DataSource dataSource;

    public ReservationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public long create(Reservation r) {
        String query = "INSERT INTO reservation(userId, hotelId, roomTypeId, startDate, endDate, roomsCount, guestsCount, totalPrice) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
        long id = -1;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, r.getUserId());
            statement.setInt(2, r.getHotelId());
            statement.setInt(3, r.getRoomTypeId());
            statement.setDate(4, Date.valueOf(r.getStartDate()));
            statement.setDate(5, Date.valueOf(r.getEndDate()));
            statement.setInt(6, r.getRoomsCount());
            statement.setInt(7, r.getGuestsCount());
            statement.setBigDecimal(8, r.getTotalPrice());

            int rows = statement.executeUpdate();

            if (rows == 0) {
                LOG.warn("Reservation creation affected no rows. userId={}, hotelId={}, roomTypeId={}, startDate={}, endDate={}",
                        r.getUserId(), r.getHotelId(), r.getRoomTypeId(), r.getStartDate(), r.getEndDate());
                throw new SQLException("Creating reservation failed, no rows affected.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    id = generatedKeys.getLong(1);
                } else {
                    LOG.warn("Reservation created but no generated key returned. userId={}, hotelId={}, roomTypeId={}",
                            r.getUserId(), r.getHotelId(), r.getRoomTypeId());
                }
            }

            auditLogger.audit(
                    "Created reservation id=" + id
                            + ", userId=" + r.getUserId()
                            + ", hotelId=" + r.getHotelId()
                            + ", roomTypeId=" + r.getRoomTypeId()
                            + ", startDate=" + r.getStartDate()
                            + ", endDate=" + r.getEndDate()
                            + ", roomsCount=" + r.getRoomsCount()
                            + ", guestsCount=" + r.getGuestsCount()
                            + ", totalPrice=" + r.getTotalPrice()
            );

            LOG.info("Reservation created successfully. reservationId={}, userId={}, hotelId={}, roomTypeId={}",
                    id, r.getUserId(), r.getHotelId(), r.getRoomTypeId());

            return id;
        } catch (SQLException e) {
            LOG.error("Database error while creating reservation. userId={}, hotelId={}, roomTypeId={}, startDate={}, endDate={}",
                    r.getUserId(), r.getHotelId(), r.getRoomTypeId(), r.getStartDate(), r.getEndDate(), e);
            throw new RuntimeException("Failed to create reservation", e);
        }
    }

    public List<Reservation> getAll() {
        List<Reservation> reservationList = new ArrayList<>();
        String query = "SELECT r.id, r.userId, r.hotelId, h.name, r.roomTypeId, rt.name, r.startDate, r.endDate, r.roomsCount, r.guestsCount, r.totalPrice " +
                "FROM reservation as r, hotel as h, roomType as rt WHERE r.hotelId = h.id and rt.id = r.roomTypeId";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {

            while (rs.next()) {
                Reservation r = createReservationFromResultSet(rs);
                reservationList.add(r);
            }

            return reservationList;
        } catch (SQLException e) {
            LOG.error("Database error while fetching all reservations", e);
            throw new RuntimeException("Failed to fetch reservations", e);
        }
    }

    public List<Reservation> forUser(Integer userId) {
        List<Reservation> reservationList = new ArrayList<>();
        String query = "SELECT r.id, r.userId, r.hotelId, h.name, r.roomTypeId, rt.name, r.startDate, r.endDate, r.roomsCount, r.guestsCount, r.totalPrice " +
                "FROM reservation as r, hotel as h, roomType as rt " +
                "WHERE r.hotelId = h.id and rt.id = r.roomTypeId and r.userId = " + userId;

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {

            while (rs.next()) {
                Reservation r = createReservationFromResultSet(rs);
                reservationList.add(r);
            }

            return reservationList;
        } catch (SQLException e) {
            LOG.error("Database error while fetching reservations for userId={}", userId, e);
            throw new RuntimeException("Failed to fetch reservations for user", e);
        }
    }

    private Reservation createReservationFromResultSet(ResultSet rs) throws SQLException {
        Integer id = rs.getInt(1);
        Integer userId = rs.getInt(2);
        Integer hotelId = rs.getInt(3);
        String hotelName = rs.getString(4);
        Integer roomTypeId = rs.getInt(5);
        String roomTypeName = rs.getString(6);
        LocalDate startDate = rs.getDate(7).toLocalDate();
        LocalDate endDate = rs.getDate(8).toLocalDate();
        Integer roomsCount = rs.getInt(9);
        Integer guestCount = rs.getInt(10);
        BigDecimal totalPrice = rs.getBigDecimal(11);

        return new Reservation(
                id,
                userId,
                hotelId,
                hotelName,
                roomTypeId,
                roomTypeName,
                startDate,
                endDate,
                roomsCount,
                guestCount,
                totalPrice
        );
    }

    public void deleteById(Integer id) {
        String query = "DELETE FROM reservation WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setInt(1, id);
            int rows = statement.executeUpdate();

            if (rows == 0) {
                LOG.warn("Attempt to delete non-existing reservation. reservationId={}", id);
                return;
            }

            auditLogger.audit("Deleted reservation id=" + id);
            LOG.info("Reservation deleted successfully. reservationId={}", id);
        } catch (SQLException e) {
            LOG.error("Database error while deleting reservation. reservationId={}", id, e);
            throw new RuntimeException("Failed to delete reservation", e);
        }
    }
}