package DistributionServer;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;

public class DatabaseConnector {

    private Connection dbConnection;
    private String query;
    private Statement statement;

    public DatabaseConnector() {
        initialize();
    }

    private void initialize() {
        // In SQLite3 connecting to a non-existing database will automatically create it.
        // Therefore we need to check if the tables we need are there. If no, we have a fresh DB.
        // If yes, we have an already established DB.

        try {
            // connect to database
            String url = "jdbc:sqlite:DSdatabase.db";
            dbConnection = DriverManager.getConnection(url);
            System.out.println("Successfully connected to the database");
            // check if the tables we need exist in the DB
            query = "SELECT name FROM sqlite_master WHERE type='table'";
            statement  = dbConnection.createStatement();
            ResultSet rs = statement.executeQuery(query);
            List<String> tables = new LinkedList<>();
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
            // if any of the tables are missing, create them
            if (!tables.contains("NSregistrations")) {
                createTableNSregistrations();
            }
            else if (!tables.contains("DSregistrations")) {
                createTableDSregistrations();
            }
        }catch(SQLException e) {
            e.printStackTrace();
            return;
        }
    }

    private void createTableNSregistrations() throws SQLException {
        query = "CREATE TABLE NSregistrations (NSNetID TEXT PRIMARY KEY, NSIPaddr TEXT)";
        statement.executeQuery(query);
//        System.out.println("NSregistrations table created");
    }

    private void createTableDSregistrations() throws SQLException {
        query ="CREATE TABLE DSregistrations (NSservedIPaddr TEXT PRIMARY KEY, DSIPaddr TEXT)";
        statement.executeQuery(query);
//        System.out.println("DSregistrations table created");
    }

    public void saveNSregistration(String NetID, String IP) {
        query = "INSERT INTO NSregistrations (NSNetID, NSIPaddr) VALUES ('" + NetID + "', '" + IP + "');";
        try {
            statement.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteNSregistration(String NetID) {
        query = "DELETE from NSregistrations WHERE NSNetID = '" + NetID + "';";
        try {
            statement.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String lookupNSIPaddr(String NetID) {
        query = "SELECT NSIPaddr FROM NSregistrations WHERE NSNetID = '" + NetID + "';";
        String IP = "";
        try {
            ResultSet rs = statement.executeQuery(query);
            rs.next();
            IP = rs.getString(1);
        } catch (SQLException e) {
            if(e.getMessage().equals("ResultSet closed")) {
                System.out.println("error");
                IP = "error";
            }
        }
        return IP;
    }

    public void saveDSregistration(String DSIP) {
        query = "INSERT INTO DSregistrations (DSIPaddr) values ('" + DSIP + "');";
        try {
            statement.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteDSregistration(String DSIP) {
        query = "DELETE from DSregistrations WHERE DSIPaddr = '" + DSIP + "';";
        try {
            statement.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateRegisteredNSbyDS(String DSIP, List<String>NSIPs) {

        // retrieve the internal ID of the DS that sent the update
        query = "SELECT DSiID FROM DSregistrations WHERE DSIPaddr = '" + DSIP + "';";
        String iID = "";
        try {
            ResultSet rs = statement.executeQuery(query);
            rs.next();
            iID = rs.getString(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // delete every currently registered NS associated with the DS
        query = "DELETE from DSservedNSs WHERE DSiID = '" + iID + "';";
        try {
            statement.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // add every IP as registered with the DS
        for (String NSIP : NSIPs) {
            query = "INSERT INTO DSservedNSs (DSiID, NSIPaddr) values ('" + iID + "', '" + NSIP + "');";
            try {
                statement.executeUpdate(query);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // searches in the DB for the DS serving a specific NS
    // if an appropriate DS is found, this returns the DS's IP address
    public String lookupDSservingNetID(String NetID) {
        String DSIPaddr = "error";

        return DSIPaddr;
    }
}
