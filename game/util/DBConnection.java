package com.monster.game.util;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    // Pastikan nama database benar
    private static final String URL = "jdbc:mysql://localhost:3306/db_monster_game";
    private static final String USER = "root";
    private static final String PASS = "";

    public static Connection getConnection() {
        try {
            // --- BARIS AJAIB INI YANG DITAMBAHKAN ---
            // Kita paksa Java buat kenalan sama Driver MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
            // ----------------------------------------

            return DriverManager.getConnection(URL, USER, PASS);
        } catch (Exception e) {
            System.err.println("Koneksi Gagal: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}