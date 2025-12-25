package com.monster.game.intent;

/**
 * File ini mendefinisikan Intent khusus untuk memicu proses pemuatan data (Load Data).
 * Dalam arsitektur MVI, class ini digunakan sebagai sinyal atau perintah dari View
 * yang memberitahu Reducer bahwa aplikasi perlu mengambil data skor atau leaderboard dari database.
 */
public class LoadDataIntent implements GameIntent {
    // Class ini berfungsi sebagai penanda aksi "Muat Data" tanpa memerlukan parameter tambahan.
}