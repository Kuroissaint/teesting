package com.monster.game.view;

import java.awt.*;

/**
 * File ini mendefinisikan objek Bullet (Peluru) yang digunakan oleh Player maupun Monster.
 * Tugas utamanya adalah mengelola posisi, pergerakan, ukuran, dan area tabrakan (hitbox)
 * dari setiap proyektil yang ditembakkan di dalam game.
 */
public class Bullet {
    // Koordinat posisi peluru di layar
    public int x, y;

    // Kecepatan gerak peluru pada sumbu X dan Y (hasil perhitungan sudut)
    public double dx, dy;

    // Kecepatan total peluru
    public int speed;

    // Status peluru; jika false, peluru akan dihapus dari memori oleh Reducer
    public boolean isActive = true;

    // Menentukan apakah peluru milik Player (mengarah ke monster) atau Musuh (mengarah ke player)
    public boolean isPlayerBullet;

    // Menentukan apakah peluru ini adalah jenis spesial "Plasma"
    public boolean isPlasma = false;

    // Variabel pembantu untuk animasi sprite (khusus peluru plasma)
    public int spriteCounter = 0;
    public int spriteNum = 1;

    // Dimensi visual peluru
    public int width;
    public int height;

    /**
     * Konstruktor 1: Untuk membuat peluru biasa (non-plasma).
     */
    public Bullet(int startX, int startY, int targetX, int targetY, boolean isPlayerBullet) {
        this(startX, startY, targetX, targetY, isPlayerBullet, false);
    }

    /**
     * Konstruktor 2: Versi lengkap untuk mengatur apakah peluru tersebut jenis Plasma atau bukan.
     * Di sini ukuran dan kecepatan peluru ditentukan berdasarkan jenisnya.
     */
    public Bullet(int startX, int startY, int targetX, int targetY, boolean isPlayerBullet, boolean isPlasma) {
        this.x = startX;
        this.y = startY;
        this.isPlayerBullet = isPlayerBullet;
        this.isPlasma = isPlasma;

        // Pengaturan properti berdasarkan jenis peluru
        if (isPlasma) {
            this.width = 128; // Ukuran Plasma lebih besar (Area damage luas)
            this.height = 128;
            this.speed = 8;
        } else {
            this.width = 24;  // Ukuran Peluru Biasa kecil
            this.height = 24;
            // Peluru player dibuat lebih cepat daripada peluru monster agar lebih responsif
            this.speed = (isPlayerBullet) ? 12 : 4;
        }

        // Kalkulasi Sudut Tembak: Menghitung arah gerak peluru dari posisi asal ke target mouse/player
        double angle = Math.atan2(targetY - startY, targetX - startX);
        this.dx = speed * Math.cos(angle);
        this.dy = speed * Math.sin(angle);
    }

    /**
     * Memperbarui posisi peluru berdasarkan kecepatan arah (dx, dy).
     * Juga mengelola pergantian frame animasi jika peluru adalah jenis Plasma.
     */
    public void update() {
        x += dx;
        y += dy;

        // Animasi bergantian sprite untuk efek plasma yang berkedip/bergerak
        if (isPlasma) {
            spriteCounter++;
            if (spriteCounter > 5) {
                spriteNum = (spriteNum == 1) ? 2 : 1;
                spriteCounter = 0;
            }
        }
    }

    /**
     * Menghasilkan area kotak (Rectangle) yang digunakan untuk deteksi tabrakan.
     * @return Rectangle yang mewakili hitbox peluru.
     */
    public Rectangle getBounds() {
        // Memberikan margin agar hitbox sedikit lebih kecil dari gambar asli
        // sehingga permainan terasa lebih adil dan akurat bagi pemain.
        int margin = isPlasma ? 20 : 2;
        return new Rectangle(x + margin, y + margin, width - (margin*2), height - (margin*2));
    }
}