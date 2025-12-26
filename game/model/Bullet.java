package com.monster.game.model;

import java.awt.Rectangle;

/**
 * Model data Peluru. Hanya menyimpan koordinat dan kecepatan.
 * Diperbarui: Menambahkan spriteCounter untuk mendukung animasi di Reducer.
 */
public class Bullet {
    public double x, y; // Gunakan double agar pergerakan lebih halus
    public int width, height;
    public double velX, velY;
    public boolean isActive = true;
    public boolean isPlayerBullet;
    public boolean isPlasma = false;

    // --- TAMBAHKAN DUA VARIABEL INI ---
    public int spriteNum = 1;
    public int spriteCounter = 0;
    // ----------------------------------

    public Bullet(int x, int y, int targetX, int targetY, boolean isPlayer) {
        this.x = x;
        this.y = y;
        this.isPlayerBullet = isPlayer;
        this.width = 24;
        this.height = 24;
        calculateVelocity(targetX, targetY);
    }

    public Bullet(int x, int y, int targetX, int targetY, boolean isPlayer, boolean isPlasma) {
        this(x, y, targetX, targetY, isPlayer);
        this.isPlasma = isPlasma;
        if (isPlasma) {
            this.width = 96;
            this.height = 96;
        }
    }

    private void calculateVelocity(int tx, int ty) {
        double angle = Math.atan2(ty - y, tx - x);
        int speed = isPlayerBullet ? 12 : 5;
        this.velX = Math.cos(angle) * speed;
        this.velY = Math.sin(angle) * speed;
    }

    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, width, height);
    }
}