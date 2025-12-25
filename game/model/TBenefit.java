package com.monster.game.model;

/**
 * File ini berfungsi sebagai Model Data atau Entity (POJO - Plain Old Java Object).
 * Tugas utamanya adalah merepresentasikan satu baris data dari tabel 'tbenefit' di database.
 * Class ini digunakan untuk mempermudah transfer data antara Database, Reducer, dan View.
 */
public class TBenefit {

    // --- ATRIBUT DATA (Sesuai dengan kolom di tabel database) ---
    private int id;                 // Primary Key (Auto Increment)
    private String username;        // Nama unik pemain
    private int skor;               // Skor tertinggi (Highscore)
    private int peluruMeleset;      // Statistik jumlah peluru musuh yang dilewati
    private int sisaPeluru;         // Jumlah peluru terakhir pemain
    private String status;           // Status permainan ("ONGOING" atau "GAMEOVER")

    /**
     * CONSTRUCTOR KOSONG
     * Wajib ada agar GameReducer dapat membuat objek ini secara dinamis
     * saat mengambil data dari database (ResultSet).
     */
    public TBenefit() {
    }

    /**
     * CONSTRUCTOR LENGKAP
     * Digunakan saat perlu membuat objek dengan data awal yang sudah ditentukan.
     */
    public TBenefit(String username, int skor, int peluruMeleset, int sisaPeluru) {
        this.username = username;
        this.skor = skor;
        this.peluruMeleset = peluruMeleset;
        this.sisaPeluru = sisaPeluru;
    }

    // --- GETTER & SETTER ---
    // Digunakan untuk mengambil (Get) atau mengubah (Set) nilai variabel private di atas.

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getSkor() { return skor; }
    public void setSkor(int skor) { this.skor = skor; }

    public int getPeluruMeleset() { return peluruMeleset; }
    public void setPeluruMeleset(int peluruMeleset) { this.peluruMeleset = peluruMeleset; }

    public int getSisaPeluru() { return sisaPeluru; }
    public void setSisaPeluru(int sisaPeluru) { this.sisaPeluru = sisaPeluru; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}