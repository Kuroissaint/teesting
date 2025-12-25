package com.monster.game.view;

import java.awt.*;
import java.util.ArrayList; // Tambahan Import

public class Monster {
    public int x, y;
    public double speed;
    public boolean isActive = true;
    public int shotCounter = 0;
    public int spriteCounter = 0;
    public int spriteNum = 1;
    public int type; // 1 = Tanaman, 2 = Kelelawar

    public Monster(int x, int y, int type) {
        this.x = x;
        this.y = y;
        this.type = type;

        if (type == 1) this.speed = 0.5; // Tanaman Lambat
        else if (type == 2) this.speed = 2; // Kelelawar Cepat
    }

    // --- UPDATE TERBARU: Menerima List Pohon ---
    public void update(int playerX, int playerY, ArrayList<Point> trees) {
        int nextX = x;
        int nextY = y;

        // Hitung calon posisi berikutnya
        if (x < playerX) nextX += speed;
        if (x > playerX) nextX -= speed;
        if (y < playerY) nextY += speed;
        if (y > playerY) nextY -= speed;

        // --- LOGIKA TABRAKAN ---
        if (type == 1) {
            // Tipe 1 (Tanaman): Cek tabrakan pohon (Gak bisa tembus)
            if (!checkTreeCollision(nextX, nextY, trees)) {
                x = nextX;
                y = nextY;
            }
        } else {
            // Tipe 2 (Kelelawar): Terbang (Tembus pohon/Abaikan tabrakan)
            x = nextX;
            y = nextY;
        }

        // Animasi
        spriteCounter++;
        if (spriteCounter > 12) {
            spriteNum = (spriteNum == 1) ? 2 : 1;
            spriteCounter = 0;
        }
    }

    // --- METHOD CEK TABRAKAN (Sama seperti di GamePanel) ---
    private boolean checkTreeCollision(int targetX, int targetY, ArrayList<Point> trees) {
        // Hitbox Monster (Agak kecil biar gak gampang nyangkut)
        Rectangle monsterRect = new Rectangle(targetX + 20, targetY + 20, 24, 24);
        int treeSize = 256;

        for (Point p : trees) {
            // Hitbox Batang Pohon
            int trunkWidth = 60;
            int trunkHeight = 40;
            int offsetX = (treeSize - trunkWidth) / 2;
            int offsetY = treeSize - trunkHeight - 20;

            Rectangle treeRect = new Rectangle(p.x + offsetX, p.y + offsetY, trunkWidth, trunkHeight);

            if (monsterRect.intersects(treeRect)) return true;
        }
        return false;
    }

    public void draw(Graphics2D g2, Image img) {
        if (img != null) g2.drawImage(img, x, y, 128, 128, null);
    }

    public Rectangle getBounds() {
        return new Rectangle(x + 32, y + 32, 64, 64);
    }
}