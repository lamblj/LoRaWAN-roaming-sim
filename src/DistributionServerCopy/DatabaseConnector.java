package DistributionServerCopy;

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
            if (!tables.contains("DSregistrations")) {
                createTableDSregistrations();
            }
            if (!tables.contains("DSservedNSs")) {
                createTableDSservedNSs();
            }
            query = "PRAGMA foreign_keys = ON;";
            statement.executeUpdate(query);
        }catch(SQLException e) {
            e.printStackTrace();
            return;
        }
    }

    private void createTableNSregistrations() throws SQLException {
        query = "CREATE TABLE NSregistrations (NSNetID TEXT (12) PRIMARY KEY, NSIPaddr TEXT (15))";
        statement.executeUpdate(query);
    }

    private void createTableDSregistrations() throws SQLException {
        query = "CREATE TABLE DSregistrations (DSiID INTEGER PRIMARY KEY, DSIPaddr TEXT (15))";
        statement.executeUpdate(query);
    }

    private void createTableDSservedNSs() throws SQLException {
        query = "CREATE TABLE DSservedNSs (DSiID INTEGER REFERENCES DSregistrations (DSiID) ON DELETE CASCADE ON UPDATE CASCADE, NetIDserved TEXT (12));";
        statement.executeUpdate(query);
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
        String IP = "error";
        try {
            ResultSet rs = statement.executeQuery(query);
            rs.next();
            IP = rs.getString(1);
        } catch (SQLException e) {

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

    public void updateRegisteredNSbyDS(String DSIP, List<String>NetIDs) {

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

        // add every NetID as registered with the DS
        for (String NetID : NetIDs) {
            query = "INSERT INTO DSservedNSs (DSiID, NetIDserved) values ('" + iID + "', '" + NetID + "');";
            try {
                statement.executeUpdate(query);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // searches in the database for a specific NetworkServer to see
    // if this DistributionServer is serving it.
    // this function returns the IP address of the DistributionServer if found, or "error" if not
    public String lookupDSservingNetID(String NetID) {
        String DSIPaddr = "error";
        query = "select DSservedNSs.NetIDserved, DSregistrations.DSIPaddr from DSservedNSs, DSregistrations where DSservedNSs.DSiID=DSregistrations.DSiID and NetIDserved='" + NetID + "'";
        try {
            ResultSet rs = statement.executeQuery(query);
            rs.next();
            DSIPaddr = rs.getString(1);
        } catch (SQLException e) {

        }
        return DSIPaddr;
    }
}
