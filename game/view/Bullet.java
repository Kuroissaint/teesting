package com.monster.game.view;

import java.awt.*;

public class Bullet {
    private double x, y;
    private double velocityX, velocityY;
    private int speed = 8; // Kecepatan peluru
    public boolean isActive;

    // --- PENANDA PEMILIK PELURU ---
    public boolean isPlayerBullet; // True = Player, False = Monster
    final int size = 200; // Ukuran gambar peluru (sesuaikan dengan gambarmu)

    public Bullet(int startX, int startY, int targetX, int targetY, boolean isPlayerBullet) {
        this.x = startX;
        this.y = startY - (size / 2.0); // Posisi tengah
        this.isPlayerBullet = isPlayerBullet;
        this.isActive = true;

        // Jika ini peluru monster, kecepatannya kita kurangi dikit biar adil
        if (!isPlayerBullet) {
            this.speed = 5;
        }

        // Hitung sudut tembakan ke arah target
        double deltaX = targetX - startX;
        double deltaY = targetY - startY;
        double angle = Math.atan2(deltaY, deltaX);

        this.velocityX = Math.cos(angle) * speed;
        this.velocityY = Math.sin(angle) * speed;
    }

    public void update() {
        x += velocityX;
        y += velocityY;

        // Hapus jika keluar layar
        if (x < -50 || x > 1050 || y < -50 || y > 800) {
            isActive = false;
        }
    }

    // --- DRAW MENERIMA GAMBAR ---
    public void draw(Graphics2D g2, Image img) {
        if (img != null) {
            g2.drawImage(img, (int)x, (int)y, size, size, null);
        } else {
            // Gambar cadangan jika file tidak ditemukan
            if (isPlayerBullet) g2.setColor(Color.YELLOW);
            else g2.setColor(Color.MAGENTA);
            g2.fillOval((int)x, (int)y, size, size);
        }
    }

    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, size, size);
    }
}