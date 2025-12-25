package com.monster.game.view;

import java.awt.*;

public class Bullet {
    // --- PERBAIKAN DI SINI ---
    // Ubah 'private' menjadi 'public' agar GamePanel bisa membaca posisi X dan Y
    public double x, y;

    private double velocityX, velocityY;
    private int speed = 8;
    public boolean isActive;

    public boolean isPlayerBullet;
    final int size = 20; // Ukuran peluru

    public Bullet(int startX, int startY, int targetX, int targetY, boolean isPlayerBullet) {
        this.x = startX;
        this.y = startY - (size / 2.0);
        this.isPlayerBullet = isPlayerBullet;
        this.isActive = true;

        if (!isPlayerBullet) {
            this.speed = 5;
        }

        double deltaX = targetX - startX;
        double deltaY = targetY - startY;
        double angle = Math.atan2(deltaY, deltaX);

        this.velocityX = Math.cos(angle) * speed;
        this.velocityY = Math.sin(angle) * speed;
    }

    public void update() {
        x += velocityX;
        y += velocityY;
    }

    public void draw(Graphics2D g2, Image img) {
        if (img != null) {
            g2.drawImage(img, (int)x, (int)y, size, size, null);
        } else {
            if (isPlayerBullet) g2.setColor(Color.YELLOW);
            else g2.setColor(Color.MAGENTA);
            g2.fillOval((int)x, (int)y, size, size);
        }
    }

    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, size, size);
    }
}