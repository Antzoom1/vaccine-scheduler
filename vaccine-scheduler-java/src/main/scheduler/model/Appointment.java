package scheduler.model;
import scheduler.db.ConnectionManager;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.*;

public class Appointment {
    private static int id;
    private final String patientName;
    private final String caregiverName;
    private final String vaccineName;
    private final Date time;

    private Appointment(AppointmentBuilder builder) {
        this.id = builder.id;
        this.patientName = builder.patientName;
        this.caregiverName = builder.caregiverName;
        this.vaccineName = builder.vaccineName;
        this.time = builder.time;
    }
    public static int getMaxID() throws SQLException{
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String getMAX = "Select MAX(ID) from Appointments";
        try {
            PreparedStatement statement = con.prepareStatement(getMAX);
            ResultSet getmax = statement.executeQuery();
            if (getmax.next()) {
                return getmax.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public static class AppointmentBuilder {
        private final int id;
        private final String patientName;
        private final String caregiverName;
        private final String vaccineName;
        private final Date time;

        public AppointmentBuilder(int id, String patientName, String caregiverName, String vaccineName, Date time) {
            this.id = id;
            this.patientName = patientName;
            this.caregiverName = caregiverName;
            this.vaccineName = vaccineName;
            this.time = time;
        }

        public Appointment build() throws SQLException {
            return new Appointment(this);
        }
    }

    public void saveToDB() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addAppointment = "INSERT INTO Appointments VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addAppointment);
            statement.setInt(1, this.id);
            statement.setString(2, this.patientName);
            statement.setString(3, this.caregiverName);
            statement.setString(4, this.vaccineName);
            statement.setDate(5, this.time);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

}