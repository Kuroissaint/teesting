package com.monster.game.view;

import java.awt.*;

public class Bullet {
    public int x, y;
    public double dx, dy;
    public int speed;
    public boolean isActive = true;
    public boolean isPlayerBullet;

    public boolean isPlasma = false;
    public int spriteCounter = 0;
    public int spriteNum = 1;

    // --- TAMBAHAN VARIABEL UKURAN ---
    public int width;
    public int height;

    // Constructor 1 (Biasa)
    public Bullet(int startX, int startY, int targetX, int targetY, boolean isPlayerBullet) {
        this(startX, startY, targetX, targetY, isPlayerBullet, false);
    }

    // Constructor 2 (Lengkap)
    public Bullet(int startX, int startY, int targetX, int targetY, boolean isPlayerBullet, boolean isPlasma) {
        this.x = startX;
        this.y = startY;
        this.isPlayerBullet = isPlayerBullet;
        this.isPlasma = isPlasma;

        // --- ATUR UKURAN DI SINI ---
        if (isPlasma) {
            this.width = 64; // Ukuran Plasma Besar
            this.height = 64;
            this.speed = 8;
            System.out.println("✅ MEMBUAT PLASMA! Ukuran: " + this.width); // <-- TAMBAHKAN INI
        } else {
            this.width = 24; // Ukuran Peluru Biasa Kecil
            this.height = 24;
            this.speed = (isPlayerBullet) ? 12 : 4;
            System.out.println("⚠️ MEMBUAT PELURU BIASA. Ukuran: " + this.width); // <-- TAMBAHKAN INI
        }

        // Sudut Tembak
        double angle = Math.atan2(targetY - startY, targetX - startX);
        this.dx = speed * Math.cos(angle);
        this.dy = speed * Math.sin(angle);
    }

    public void update() {
        x += dx;
        y += dy;

        if (isPlasma) {
            spriteCounter++;
            if (spriteCounter > 5) {
                spriteNum = (spriteNum == 1) ? 2 : 1;
                spriteCounter = 0;
            }
        }
    }

    // Hitbox ngikutin ukuran width/height (Otomatis Pas)
    public Rectangle getBounds() {
        // Kita kecilin dikit hitbox-nya dari gambar asli biar enak (Offset)
        int margin = isPlasma ? 10 : 2;
        return new Rectangle(x + margin, y + margin, width - (margin*2), height - (margin*2));
    }
}