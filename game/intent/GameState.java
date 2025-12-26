package com.monster.game.intent;

import com.monster.game.model.TBenefit;
import java.util.List;

/**
 * File ini berfungsi sebagai "State" atau representasi data tunggal untuk bagian Menu Utama.
 * Dalam arsitektur MVI, State adalah objek yang bersifat 'immutable' (tidak berubah)
 * yang merangkum semua informasi yang diperlukan oleh View (GameView) untuk merender layar.
 */
public class GameState {

    // Data daftar peringkat pemain yang diambil dari database
    private List<TBenefit> leaderboard;

    // Pesan status untuk memberitahu View kondisi sistem (contoh: "Loading", "Ready", atau "Playing:user:score")
    private String status;

    /**
     * Konstruktor untuk membuat snapshot kondisi data pada waktu tertentu.
     * @param leaderboard Daftar skor tertinggi yang akan ditampilkan.
     * @param status Pesan informasi untuk navigasi atau indikator proses.
     */
    public GameState(List<TBenefit> leaderboard, String status) {
        this.leaderboard = leaderboard;
        this.status = status;
    }

    /**
     * @return Mengambil daftar data leaderboard untuk ditampilkan di tabel.
     */
    public List<TBenefit> getLeaderboard() {
        return leaderboard;
    }

    /**
     * @return Mengambil status saat ini untuk menentukan aksi UI selanjutnya.
     */
    public String getStatus() {
        return status;
    }
}