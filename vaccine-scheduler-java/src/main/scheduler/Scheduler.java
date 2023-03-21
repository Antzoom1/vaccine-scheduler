package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Appointment;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        if(tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        if(usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        if(!(isStrong(password))) {
            return;
        }

        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);

        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            currentPatient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }   finally {
            currentPatient = null;
        }
    }



    private static boolean isStrong(String password) {
        if(password.length() < 8){
            System.out.println("The length of your password should be greater than 8");
            return false;
        }else if ((!(password.contains("?")))&&(!(password.contains("@")))&&(!(password.contains("#")))&&(!(password.contains("!")))){
            System.out.println("Your password should include at least a special character");
            return false;
        }else if(!(doubleCheckIfStrong(password))){
            System.out.println("Your password should include a number digit, a Uppercase letter and a Lowercase letter");
            return false;
        }
        return true;
    }

    private static boolean doubleCheckIfStrong(String password) {
        boolean isUppercase = false;
        boolean isDigit = false;
        boolean isLowercase = false;
        for(int i = 0; i < password.length(); i++) {
            char check = password.charAt(i);
            if(Character.isUpperCase(check)){
                isUppercase = true;
            }
            if(Character.isDigit(check)) {
                isDigit = true;
            }
            if(Character.isLowerCase(check)) {
                isLowercase = true;
            }
        }
        if(isUppercase == true && isDigit == true && isLowercase == true) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
        cm.closeConnection();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first!");
            return;
        }
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        Date date = null;
        String tempDate = tokens[1];
        try {
            date = Date.valueOf(tempDate);
        } catch (IllegalArgumentException e) {
            System.out.println("Please try again!");
        }
        if (date == null) {
            System.out.println("Please try again!");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection connection = cm.createConnection();
        String Availabilities = "Select Username from Availabilities where Time = ? and Availabilities.username NOT IN (Select Caregiver_name from Appointments Where Time = ?)";
        String getVaccine = "SELECT Name,Doses FROM Vaccines";
        try {
            PreparedStatement statement = connection.prepareStatement(Availabilities);
            statement.setDate(1, date);
            statement.setDate(2, date);
            ResultSet result_caregiver = statement.executeQuery();
            PreparedStatement statement_vaccine = connection.prepareStatement(getVaccine);
            ResultSet result_vaccine = statement_vaccine.executeQuery();
            if (result_caregiver.isBeforeFirst()) {
                while (result_caregiver.next()) {
                    System.out.print("Caregiver: " + result_caregiver.getString(1) + " ");
                }
            } else {
                System.out.println("Please try again!");
                return;
            }
            while (result_vaccine.next()) {
                System.out.print("Vaccine: " + result_vaccine.getString("Name") +
                        "Doses left: " + result_vaccine.getInt("Doses"));
            }
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) {
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
            if (currentPatient == null) {
                System.out.println("Please login as a patient!");
                return;
            }
            String date = tokens[1];
            String vaccineName = tokens[2];
            Vaccine vaccine = null;
            try {
                vaccine = new Vaccine.VaccineGetter(vaccineName).get();
            } catch (SQLException e) {
                System.out.println("Error");
                e.printStackTrace();
            }
            if (vaccine.getAvailableDoses() != 0 && vaccine != null) {
                scheduler.model.Appointment isAppoint = null;
                try {
                    Date tempDate = Date.valueOf(date);
                    String SelectedCaregiver = null;
                    ConnectionManager cm = new ConnectionManager();
                    Connection con = cm.createConnection();
                    String Caregiver = "Select Username from Availabilities where Time = (?)" +
                            " and Availabilities.username NOT IN (Select Caregiver_name from Appointments" +
                            " Where Time = ?) order by NEWID()";
                    try {
                        PreparedStatement statement = con.prepareStatement(Caregiver);
                        statement.setDate(1, tempDate);
                        statement.setDate(2, tempDate);
                        ResultSet result = statement.executeQuery();
                        while (result.next()) {
                            SelectedCaregiver = result.getString("Username");
                        }
                    } catch (SQLException e) {
                        System.out.println("Error");
                    } finally {
                        cm.closeConnection();
                    }
                    if (SelectedCaregiver == null) {
                        System.out.println("No Caregiver is available!");
                        return;
                    }
                    int id = 0;
                    ConnectionManager max = new ConnectionManager();
                    Connection connectionMax = max.createConnection();

                    String MAX = "Select MAX(ID) from Appointments";
                    try {
                        PreparedStatement statement = connectionMax.prepareStatement(MAX);
                        ResultSet result = statement.executeQuery();
                        while (result.next()) {
                            id = result.getInt(1) + 1;
                        }
                    } catch (SQLException e) {
                        throw new SQLException();
                    } finally {
                        cm.closeConnection();
                    }
                    isAppoint = new Appointment.AppointmentBuilder(id, currentPatient.username, SelectedCaregiver, vaccineName, tempDate).build();
                    vaccine.decreaseAvailableDoses(1);
                    isAppoint.saveToDB();
                    System.out.println("Appointment ID: " + id + ", Caregiver username: " + SelectedCaregiver);
                } catch (SQLException e) {
                    System.out.println("Please try again!");
                    e.printStackTrace();
                }
            } else {
                System.out.println("No available vaccine type!");
                return;
            }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String isAppointed;
        String username;

        if(currentPatient!=null) {
            isAppointed = "SELECT * FROM Appointments WHERE Patient_Name = ?";
            username= currentPatient.getUsername();
        } else {
            isAppointed = "SELECT * FROM Appointments WHERE Caregiver_Name = ?";
            username= currentCaregiver.getUsername();
        }
        try {
            PreparedStatement statement = con.prepareStatement(isAppointed);
            statement.setString(1, username);
            ResultSet result = statement.executeQuery();
            while(result.next()) {
                System.out.print("Appointment ID: " + result.getInt(1) + " Vaccine_name: " +
                        result.getString(4) + " Date: " + result.getDate(5) + " ");
                if(currentCaregiver == null) {
                    System.out.print("Caregiver_name: " + result.getString(3) + " ");
                }else{
                    System.out.print("Patient_name: " + result.getString(2) + " ");
                }
            }
            return;
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void logout(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }if(currentCaregiver != null){
            currentCaregiver = null;
        }else if(currentPatient != null) {
            currentPatient = null;
        }
        System.out.println("Successfully logged out!");
        }
}
