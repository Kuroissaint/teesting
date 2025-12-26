package com.monster.game.model;

import java.awt.Rectangle;

/**
 * Model data untuk entitas Monster.
 * Kelas ini bersifat pasif (hanya menyimpan data/state) sesuai prinsip MVI.
 * Logika pergerakan dan aksi monster dikelola sepenuhnya oleh GameplayReducer.
 */
public class Monster {
    // Koordinat menggunakan double agar perhitungan pergerakan (diagonal/trigonometri)
    // di Reducer bisa sangat presisi dan terlihat halus di layar.
    public double x, y;

    public int width, height, type;
    public boolean isActive = true; // Menandakan apakah monster masih hidup/aktif di layar

    // Timer internal untuk menentukan kapan monster menembakkan peluru
    public int shotCounter = 0;

    // Variabel untuk mengelola urutan animasi gambar (sprite)
    public int spriteNum = 1;      // Frame mana yang sedang tampil
    public int spriteCounter = 0;   // Penghitung waktu pergantian frame

    /**
     * Konstruktor untuk membuat instance monster baru.
     * @param x Posisi horizontal awal
     * @param y Posisi vertikal awal (biasanya di luar layar bawah)
     * @param type Jenis monster (1: Kelelawar, 2: Tanaman)
     */
    public Monster(int x, int y, int type) {
        this.x = x;
        this.y = y;
        this.type = type;

        // Penyesuaian ukuran visual berdasarkan jenis monster (PDF Hal. 7)
        if (type == 1) {
            // Type 1: Kelelawar - Dibuat sedikit lebih kecil agar terlihat lincah
            this.width = 84;
            this.height = 84;
        } else {
            // Type 2: Tanaman - Dibuat lebih besar untuk kesan monster darat yang berat
            this.width = 108;
            this.height = 108;
        }
    }

    /**
     * Menghasilkan area tabrak (Hitbox) untuk deteksi tabrakan.
     * Menggunakan padding (ruang kosong) agar hitbox tidak sebesar gambar aslinya,
     * sehingga memberikan toleransi bagi pemain agar permainan tidak terlalu sulit.
     * * @return Rectangle yang merepresentasikan hitbox monster.
     */
    public Rectangle getBounds() {
        // Semakin besar padding, semakin kecil area yang bisa terkena peluru/tabrakan.
        int padding = 15;
        return new Rectangle(
                (int)x + padding,
                (int)y + padding,
                width - (padding * 2),
                height - (padding * 2)
        );
    }
}