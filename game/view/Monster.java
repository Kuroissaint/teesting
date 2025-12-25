package com.monster.game.view;

import java.awt.*;
import java.util.ArrayList;

public class Monster {
    public int x, y;
    public int type; // 1 = Kelelawar, 2 = Tanaman
    public boolean isActive = true;
    public int spriteCounter = 0;
    public int spriteNum = 1;

    // --- INI YANG WAJIB ADA BIAR GAK ERROR ---
    public int width;  // Harus PUBLIC
    public int height; // Harus PUBLIC

    // Variabel Nembak
    public int shotCounter = 0;

    public Monster(int startX, int startY, int type) {
        this.x = startX;
        this.y = startY;
        this.type = type;

        // --- SETTING UKURAN DI SINI ---
        if (type == 1) {
            // Tipe 1: Kelelawar (Lebih Lebar)
            this.width = 96;
            this.height = 72;
        } else {
            // Tipe 2: Tanaman (Kotak Kecil)
            this.width = 96;
            this.height = 96;

            // Koreksi posisi Y biar "napak tanah" (Karena lebih pendek dari 64)
            this.y += (108 - 96);
        }
    }

    public void update(int playerX, int playerY, ArrayList<Point> trees) {

        // 1. TENTUKAN POSISI TUJUAN (NEXT X/Y)
        int speed = (type == 1) ? 1 : 2; // Tipe 1 speed 1, Tipe 2 speed 2
        int nextX = x;
        int nextY = y;

        // Logika Kejar Player (Sama rumusnya, beda speed-nya doang)
        if (x < playerX) nextX += speed;
        else nextX -= speed;

        if (y < playerY) nextY += speed;
        else nextY -= speed;

        // 2. EKSEKUSI GERAKAN (BERDASARKAN TIPE)
        if (type == 2) {
            // --- TIPE 1: KELELAWAR (TERBANG) ---
            // Speed 1, Bebas hambatan (Gak usah cek pohon)
            this.x = nextX;
            this.y = nextY;
        }
        else {
            // --- TIPE 2: MONSTER DARAT ---
            // Speed 2, TAPI Wajib Cek Pohon!

            // Kita cek tabrakan dulu sebelum update posisi.
            // Kalau posisi tujuan (nextX, nextY) aman dari pohon, baru jalan.
            if (!checkTreeCollision(nextX, nextY, trees)) {
                this.x = nextX;
                this.y = nextY;
            }
        }

        // 3. ANIMASI SPRITE
        spriteCounter++;
        if (spriteCounter > 10) {
            spriteNum = (spriteNum == 1) ? 2 : 1;
            spriteCounter = 0;
        }
    }

    // --- HELPER: CEK TABRAKAN POHON (Wajib ada di kelas Monster) ---
    private boolean checkTreeCollision(int nextX, int nextY, ArrayList<Point> trees) {
        // Bikin kotak prediksi posisi masa depan
        Rectangle futureHitbox = new Rectangle(nextX, nextY, width, height);

        for (Point p : trees) {
            // Hitbox Batang Pohon (Koordinat harus sama persis kayak di GamePanel/Reducer)
            // x+98, y+196, w60, h40 adalah area batang bawah
            Rectangle treeRect = new Rectangle(p.x + 98, p.y + 196, 60, 40);

            if (futureHitbox.intersects(treeRect)) {
                return true; // TABRAKAN! JANGAN LEWAT SINI
            }
        }
        return false; // AMAN
    }

    // Update Hitbox biar pas sama ukuran visual
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
}