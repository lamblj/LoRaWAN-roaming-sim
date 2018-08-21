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
            else {
//                System.out.println("Database already initiated");
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

    public void deleteNSregistration (String NetID) {
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
            e.printStackTrace();
        }
        return IP;
    }
}
