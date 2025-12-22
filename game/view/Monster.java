package com.monster.game.view;

import java.awt.*;
import java.util.Random;

public class Monster {
    public int x, y;
    private int speed = 2; // Kecepatan monster
    public boolean isActive;
    private int size = 128;

    // Variabel Animasi
    public int spriteCounter = 0;
    public int spriteNum = 1;
    public int shotCounter = 0;

    public Monster(int startX, int startY) {
        this.x = startX;
        this.y = startY;
        this.isActive = true;
        this.shotCounter = new Random().nextInt(100);
    }

    // --- PERUBAHAN DI SINI: TERIMA POSISI PLAYER ---
    public void update(int playerX, int playerY) {

        // --- 1. LOGIKA PERGERAKAN (MENGEJAR PLAYER) ---
        int targetX = playerX + 16;
        int targetY = playerY + 16;

        double deltaX = targetX - x;
        double deltaY = targetY - y;

        double angle = Math.atan2(deltaY, deltaX);

        x += Math.cos(angle) * speed;
        y += Math.sin(angle) * speed;

        // --- 2. LOGIKA ANIMASI (INI YANG HILANG TADI) ---
        spriteCounter++;
        if (spriteCounter > 12) {
            spriteNum = (spriteNum == 1) ? 2 : 1;
            spriteCounter = 0;
        }
    }

    public void draw(Graphics2D g2, Image img) {
        if (img != null) {
            g2.drawImage(img, x, y, size, size, null);
        } else {
            g2.setColor(Color.RED);
            g2.fillRect(x, y, size, size);
        }
    }

    public Rectangle getBounds() {
        int padding = 32; // Semakin besar angka ini, hitbox semakin kecil/masuk ke dalam
        return new Rectangle(x + padding, y + padding, size - (padding * 2), size - (padding * 2));
    }
}