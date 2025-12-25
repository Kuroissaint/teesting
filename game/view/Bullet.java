package com.monster.game.view;

import java.awt.*;

public class Bullet {
    public int x, y;
    public double dx, dy;
    public int speed; // Speed dinamis (diatur di constructor)
    public boolean isActive = true;
    public boolean isPlayerBullet;

    // VARIABEL PLASMA
    public boolean isPlasma = false;
    public int spriteCounter = 0;
    public int spriteNum = 1;

    // Constructor 1: Peluru Biasa (Dipanggil saat klik Mouse)
    public Bullet(int startX, int startY, int targetX, int targetY, boolean isPlayerBullet) {
        this(startX, startY, targetX, targetY, isPlayerBullet, false); // Panggil constructor bawah
    }

    // Constructor 2: Peluru Khusus (Bisa set Plasma)
    public Bullet(int startX, int startY, int targetX, int targetY, boolean isPlayerBullet, boolean isPlasma) {
        this.x = startX;
        this.y = startY;
        this.isPlayerBullet = isPlayerBullet;
        this.isPlasma = isPlasma;

        // --- ATUR KECEPATAN PELURU DI SINI ---
        if (isPlayerBullet) {
            if (isPlasma) {
                this.speed = 8;  // Plasma: Agak lambat karena "berat/kuat"
            } else {
                this.speed = 12; // Peluru Player Biasa: CEPAT (Biar gampang kena musuh)
            }
        } else {
            this.speed = 6;      // Peluru Monster: LAMBAT (Biar player bisa menghindar)
        }

        // Hitung sudut tembak (biar lurus ke arah target)
        double angle = Math.atan2(targetY - startY, targetX - startX);
        this.dx = speed * Math.cos(angle);
        this.dy = speed * Math.sin(angle);
    }

    public void update() {
        x += dx;
        y += dy;

        // Animasi Plasma (Ganti frame tiap 5 tick)
        if (isPlasma) {
            spriteCounter++;
            if (spriteCounter > 5) {
                spriteNum = (spriteNum == 1) ? 2 : 1;
                spriteCounter = 0;
            }
        }
    }

    public void draw(Graphics2D g2, Image img) {
        // Kalau Plasma, gambarnya agak gedean
        if (isPlasma) {
            g2.drawImage(img, x, y, 64, 64, null); // Ukuran Plasma 64x64
        } else {
            g2.drawImage(img, x, y, 16, 16, null); // Ukuran Peluru Biasa 16x16
        }
    }

    public Rectangle getBounds() {
        if (isPlasma) {
            // Hitbox Plasma BESAR
            return new Rectangle(x + 7, y + 7, 50, 50);
        } else {
            // Hitbox Peluru Biasa KECIL
            return new Rectangle(x + 2, y + 2, 12, 12);
        }
    }
}