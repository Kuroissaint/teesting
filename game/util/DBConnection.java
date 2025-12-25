package com.monster.game.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * File ini berfungsi sebagai utilitas untuk mengelola koneksi ke database MySQL.
 * Tugas utamanya adalah menyediakan objek 'Connection' yang digunakan oleh Reducer
 * atau DAO untuk melakukan operasi CRUD (Create, Read, Update, Delete) pada database.
 */
public class DBConnection {

    // Alamat URL database MySQL. Pastikan nama database 'db_monster_game' sudah dibuat di phpMyAdmin.
    private static final String URL = "jdbc:mysql://localhost:3306/db_monster_game";

    // Kredensial login MySQL. Default XAMPP adalah username 'root' dengan password kosong.
    private static final String USER = "root";
    private static final String PASSWORD = "";

    /**
     * Membuka dan mengembalikan koneksi aktif ke database.
     * @return Objek Connection jika berhasil, atau null jika terjadi kesalahan.
     */
    public static Connection getConnection() {
        Connection connection = null;
        try {
            // Memuat Driver JDBC MySQL ke dalam memori
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Mencoba menghubungkan ke server MySQL menggunakan kredensial yang ditentukan
            connection = DriverManager.getConnection(URL, USER, PASSWORD);

        } catch (ClassNotFoundException | SQLException e) {
            // Mencatat log kesalahan jika koneksi gagal (misal: MySQL mati atau DB tidak ditemukan)
            System.err.println("Koneksi Gagal: " + e.getMessage());
            e.printStackTrace();
        }
        return connection;
    }
}