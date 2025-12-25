package com.monster.game;

import com.monster.game.intent.GameReducer;
import com.monster.game.view.GameView;
import javax.swing.SwingUtilities;

/**
 * File ini adalah titik masuk utama (Entry Point) dari seluruh aplikasi permainan.
 * Tugas utamanya adalah menginisialisasi komponen dasar arsitektur MVI,
 * yaitu Reducer dan View, serta menjalankan antarmuka pengguna (GUI).
 */
public class Main {
    /**
     * Method main yang akan dieksekusi pertama kali saat program dijalankan.
     */
    public static void main(String[] args) {
        // Menjalankan GUI di dalam Event Dispatch Thread (EDT) agar aplikasi berjalan stabil
        SwingUtilities.invokeLater(() -> {

            // 1. Inisialisasi GameReducer sebagai pengolah logika sistem utama
            GameReducer reducer = new GameReducer();

            // 2. Inisialisasi GameView (Frame Utama) dan menyuntikkan (inject) reducer ke dalamnya
            GameView window = new GameView(reducer);

            // 3. Menampilkan jendela aplikasi ke layar pengguna
            window.setVisible(true);
        });
    }
}