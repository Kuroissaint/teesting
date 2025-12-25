package com.monster.game.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // 1. Ganti nama database jadi 'db_monster_game' sesuai file SQL kamu
    private static final String URL = "jdbc:mysql://localhost:3306/db_monster_game";

    // 2. Sesuaikan User & Password MySQL kamu (Default XAMPP biasanya root & kosong)
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() {
        Connection connection = null;
        try {
            // Load Driver MySQL (Opsional di Java baru, tapi bagus buat jaga-jaga)
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Buat Koneksi
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            // System.out.println("Koneksi Database Berhasil!"); // Uncomment buat cek

        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Koneksi Gagal: " + e.getMessage());
            e.printStackTrace();
        }
        return connection;
    }
}