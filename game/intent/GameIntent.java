package com.monster.game.intent;

/**
 * File ini berfungsi sebagai base interface atau "Marker Interface" untuk komponen Intent
 * dalam arsitektur MVI (Model-View-Intent).
 * Tugas utamanya adalah menjadi tipe data induk yang mengelompokkan semua aksi, event,
 * atau perintah yang dikirimkan dari View untuk nantinya diproses oleh Reducer.
 */
public interface GameIntent {
    // Interface ini dibiarkan kosong karena hanya berfungsi sebagai penanda (Marker Interface).
}