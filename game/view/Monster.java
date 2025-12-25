package com.monster.game.view;

import java.awt.*;
import java.util.ArrayList;

/**
 * File ini mendefinisikan objek Monster (Musuh) di dalam permainan.
 * Tugas utamanya adalah:
 * 1. Mengelola data individu setiap monster (posisi, tipe, dan status aktif).
 * 2. Mengatur logika kecerdasan buatan (AI) sederhana untuk mengejar posisi player.
 * 3. Menangani perbedaan perilaku antara monster darat (dengan tabrakan) dan monster udara (terbang).
 */
public class Monster {
    // Koordinat posisi monster di layar
    public int x, y;

    // Tipe monster: 1 = Kelelawar (Udara), 2 = Tanaman (Darat)
    public int type;

    // Status apakah monster masih hidup atau sudah mati
    public boolean isActive = true;

    // Variabel pembantu untuk animasi sprite (berganti frame)
    public int spriteCounter = 0;
    public int spriteNum = 1;

    // Dimensi visual monster (Wajib public agar bisa diakses oleh Reducer untuk cek tabrakan)
    public int width;
    public int height;

    // Penghitung waktu untuk jeda menembak peluru
    public int shotCounter = 0;

    /**
     * Konstruktor: Inisialisasi posisi dan mengatur ukuran fisik berdasarkan tipe monster.
     */
    public Monster(int startX, int startY, int type) {
        this.x = startX;
        this.y = startY;
        this.type = type;

        // Pengaturan ukuran berbeda untuk setiap jenis monster
        if (type == 1) {
            // Tipe 1: Kelelawar (Visual lebih lebar untuk sayap)
            this.width = 96;
            this.height = 72;
        } else {
            // Tipe 2: Tanaman (Visual kotak)
            this.width = 96;
            this.height = 96;

            // Penyesuaian posisi Y agar monster darat terlihat menempel di tanah
            this.y += (108 - 96);
        }
    }

    /**
     * Memperbarui logika monster di setiap frame, termasuk pergerakan mengejar player.
     * @param playerX Posisi X player saat ini.
     * @param playerY Posisi Y player saat ini.
     * @param trees Daftar rintangan pohon untuk cek tabrakan (khusus tipe darat).
     */
    public void update(int playerX, int playerY, ArrayList<Point> trees) {

        // 1. Tentukan arah dan kecepatan berdasarkan tipe
        int speed = (type == 1) ? 1 : 2; // Kelelawar lebih lambat, Tanaman lebih cepat
        int nextX = x;
        int nextY = y;

        // Logika AI sederhana: Bergerak mendekati koordinat player
        if (x < playerX) nextX += speed;
        else nextX -= speed;

        if (y < playerY) nextY += speed;
        else nextY -= speed;

        // 2. Eksekusi gerakan berdasarkan jenis pergerakan
        if (type == 1) { // TIPE 1: KELELAWAR (TERBANG)
            // Bergerak bebas tanpa terhalang oleh pohon (melewati atasnya)
            this.x = nextX;
            this.y = nextY;
        }
        else { // TIPE 2: MONSTER DARAT (TANAMAN)
            // Wajib memeriksa tabrakan dengan pohon sebelum berpindah posisi
            if (!checkTreeCollision(nextX, nextY, trees)) {
                this.x = nextX;
                this.y = nextY;
            }
        }

        // 3. Update Animasi Sprite
        spriteCounter++;
        if (spriteCounter > 10) {
            spriteNum = (spriteNum == 1) ? 2 : 1;
            spriteCounter = 0;
        }
    }

    /**
     * Memeriksa apakah posisi tujuan monster darat akan menabrak batang pohon.
     * Digunakan agar monster darat tidak berjalan menembus pohon.
     */
    private boolean checkTreeCollision(int nextX, int nextY, ArrayList<Point> trees) {
        // Membuat kotak prediksi posisi masa depan monster
        Rectangle futureHitbox = new Rectangle(nextX, nextY, width, height);

        for (Point p : trees) {
            // Hitbox Batang Pohon (Harus selaras dengan koordinat di GameplayReducer)
            Rectangle treeRect = new Rectangle(p.x + 98, p.y + 196, 60, 40);

            if (futureHitbox.intersects(treeRect)) {
                return true; // Terjadi tabrakan
            }
        }
        return false; // Aman untuk bergerak
    }

    /**
     * Menghasilkan area kotak (Rectangle) yang digunakan untuk deteksi tabrakan dengan peluru atau player.
     * @return Rectangle yang mewakili hitbox monster.
     */
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
}