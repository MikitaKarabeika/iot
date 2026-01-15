package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String URL = "jdbc:postgresql://localhost:5432/plant_system";
    private static final String USER = "user";
    private static final String PASS = "password";

    public DatabaseManager() {
        initializeDatabase();
    }

    private void initializeDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS plants (" +
                "id SERIAL PRIMARY KEY, " +
                "name VARCHAR(255) UNIQUE NOT NULL, " +
                "humidity_threshold INTEGER NOT NULL" +
                ");";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Baza danych została utworzona!");
        } catch (SQLException e) {
            System.err.println("Błąd: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<Plant> getPlantsFromDb() {
        List<Plant> plants = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name, humidity_threshold FROM plants ORDER BY name")) {
            while (rs.next()) {
                plants.add(new Plant(rs.getString("name"), rs.getInt("humidity_threshold")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return plants;
    }

    public void addPlant(String name, int threshold) {
        String sql = "INSERT INTO plants (name, humidity_threshold) VALUES (?, ?) " +
                "ON CONFLICT (name) DO UPDATE SET humidity_threshold = EXCLUDED.humidity_threshold";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setInt(2, threshold);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deletePlant(String name) {
        String sql = "DELETE FROM plants WHERE name = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}