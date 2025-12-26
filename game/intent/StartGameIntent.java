package com.monster.game.intent;

/**
 * File ini mendefinisikan Intent khusus untuk memulai permainan (Start Game).
 * Dalam arsitektur MVI, class ini membawa data identitas (username) dari View
 * ke Reducer untuk menentukan apakah pemain akan memulai sesi baru atau melanjutkan sesi lama.
 */
public class StartGameIntent implements GameIntent {

    // Nama pengguna yang diinputkan saat login atau memilih profil
    public String username;

    /**
     * Konstruktor untuk membungkus data username ke dalam sebuah Intent.
     * @param username String nama pengguna yang dikirim dari layar menu.
     */
    public StartGameIntent(String username) {
        this.username = username;
    }
}