package com.monster.game.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

public class GamePanel extends JPanel implements Runnable, KeyListener, MouseListener {

    // --- 1. SETTINGAN LAYAR ---
    final int tileSize = 64;
    final int maxScreenCol = 16;
    final int maxScreenRow = 12;
    public final int screenWidth = tileSize * maxScreenCol;  // 1024 px
    public final int screenHeight = tileSize * maxScreenRow; // 768 px

    // --- 2. GAME ENGINE ---
    Thread gameThread;
    boolean isRunning = false;
    int FPS = 60;

    // --- 3. DATA PLAYER ---
    int playerX = 300; // Posisi awal agak tengah
    int playerY = 200;
    int playerSpeed = 5;
    String username;
    boolean isMoving = false;
    int playerScore = 0; // Tambah skor

    // --- 4. GAMBAR ASSETS ---
    private Image run1, run2, idle1, idle2, idle3, bgImage, treeImage;
    private Image monster1, monster2; // Gambar Monster Animasi
    private Image bullets1, bullets2; // GAMBAR PELURU BARU

    // --- 5. OBJEK GAME ---
    ArrayList<Point> trees = new ArrayList<>();
    final int treeSize = 256;

    ArrayList<Bullet> bullets = new ArrayList<>();
    ArrayList<Monster> monsters = new ArrayList<>();
    int monsterSpawnCounter = 0;

    // Animasi Player
    public int spriteCounter = 0;
    public int spriteNum = 1;

    // Input Keyboard
    boolean upPressed, downPressed, leftPressed, rightPressed;

    public GamePanel(String username) {
        this.username = username;
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.DARK_GRAY);
        this.setDoubleBuffered(true);
        this.setFocusable(true);

        this.addKeyListener(this);
        this.addMouseListener(this);

        loadImages();
        generateTrees();
    }

    // --- LOAD SEMUA GAMBAR ---
    private void loadImages() {
        try {
            bgImage = loadImage("/com/monster/game/assets/map_forest.png");
            treeImage = loadImage("/com/monster/game/assets/tree.png");

            // Player
            run1 = loadImage("/com/monster/game/assets/mainch2.png");
            run2 = loadImage("/com/monster/game/assets/mainch3.png");
            idle1 = loadImage("/com/monster/game/assets/mainch1.png");
            idle2 = loadImage("/com/monster/game/assets/mainch2.png");
            idle3 = loadImage("/com/monster/game/assets/mainch3.png");

            // Monster
            monster1 = loadImage("/com/monster/game/assets/monster1.png");
            monster2 = loadImage("/com/monster/game/assets/monster2.png");

            // --- LOAD GAMBAR PELURU ---
            // Pastikan nama file sesuai dengan yang kamu punya di folder assets
            bullets1 = loadImage("/com/monster/game/assets/bullets1.png");
            bullets2 = loadImage("/com/monster/game/assets/bullets2.png");

        } catch (Exception e) { e.printStackTrace(); }
    }

    private Image loadImage(String path) {
        URL url = getClass().getResource(path);
        return (url != null) ? new ImageIcon(url).getImage() : null;
    }

    public void generateTrees() {
        trees.clear();
        Random rand = new Random();
        int numberOfTrees = 5 + rand.nextInt(4);
        for (int i = 0; i < numberOfTrees; i++) {
            int x = rand.nextInt(maxScreenCol) * tileSize;
            int y;
            if (rand.nextBoolean()) y = rand.nextInt(4) * tileSize;
            else y = (8 + rand.nextInt(4)) * tileSize;

            if (Math.abs(x - playerX) > tileSize * 2 || Math.abs(y - playerY) > tileSize * 2) {
                trees.add(new Point(x, y));
            }
        }
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        isRunning = true;
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1000000000 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        while (gameThread != null) {
            long currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;
            if (delta >= 1) { update(); repaint(); delta--; }
        }
    }

    // --- LOGIKA UTAMA GAME ---
    public void update() {
        // 1. UPDATE PLAYER
        isMoving = false;
        int nextX = playerX; int nextY = playerY;
        if (upPressed) { nextY -= playerSpeed; isMoving = true; }
        if (downPressed) { nextY += playerSpeed; isMoving = true; }
        if (leftPressed) { nextX -= playerSpeed; isMoving = true; }
        if (rightPressed) { nextX += playerSpeed; isMoving = true; }

        if (!checkTreeCollision(nextX, nextY)) {
            playerX = nextX; playerY = nextY;
        }
        // Boundary check sederhana
        playerX = Math.max(0, Math.min(screenWidth - tileSize, playerX));
        playerY = Math.max(0, Math.min(screenHeight - tileSize, playerY));

        // 2. UPDATE SEMUA PELURU
        for (int i = 0; i < bullets.size(); i++) {
            Bullet b = bullets.get(i);
            if (b.isActive) b.update();
            else { bullets.remove(i); i--; }
        }

        monsterSpawnCounter++;
        if (monsterSpawnCounter > 100) {
            Random rand = new Random();

            // Logika Spawn: Muncul dari BAWAH, tapi KIRI atau KANAN
            int spawnX;

            // Bagi layar jadi 3 bagian: Kiri, Tengah (Jalan), Kanan
            // Kita cuma mau spawn di Kiri atau Kanan.
            if (rand.nextBoolean()) {
                // Sisi Kiri (0 sampai 1/3 layar)
                spawnX = rand.nextInt(screenWidth / 3);
            } else {
                // Sisi Kanan (2/3 layar sampai ujung)
                spawnX = (screenWidth * 2 / 3) + rand.nextInt(screenWidth / 3);
            }

            // Spawn di Y = screenHeight (Paling Bawah)
            monsters.add(new Monster(spawnX, screenHeight));

            monsterSpawnCounter = 0;
        }

        // 4. UPDATE MONSTER & LOGIKA NEMBAK MEREKA
        int playerCenterX = playerX + tileSize / 2;
        int playerCenterY = playerY + tileSize / 2;

        for (int i = 0; i < monsters.size(); i++) {
            Monster m = monsters.get(i);
            if (m.isActive) {

                // PERUBAHAN PENTING: Kirim posisi Player ke Monster!
                m.update(playerX, playerY);

                // Logika Nembak (Tetap sama)
                m.shotCounter++;
                if (m.shotCounter > 150) {
                    bullets.add(new Bullet(m.x + 32, m.y + 32, playerCenterX, playerCenterY, false));
                    m.shotCounter = 0;
                }

            } else {
                monsters.remove(i); i--;
            }
        }

        // 5. CEK TABRAKAN PELURU (Collision Detection)
        Rectangle playerRect = new Rectangle(playerX + 16, playerY + 16, 32, 48); // Hitbox Player

        for (Bullet b : bullets) {
            if (!b.isActive) continue;
            Rectangle bulletRect = b.getBounds();

            if (b.isPlayerBullet) {
                // --- PELURU PLAYER KENA MONSTER? ---
                for (Monster m : monsters) {
                    if (m.isActive && bulletRect.intersects(m.getBounds())) {
                        m.isActive = false; // Monster Mati
                        b.isActive = false; // Peluru Hilang
                        playerScore += 10; // Tambah Skor
                        System.out.println("Monster Kena! Skor: " + playerScore);
                    }
                }
            } else {
                // --- PELURU MONSTER KENA PLAYER? ---
                if (bulletRect.intersects(playerRect)) {
                    b.isActive = false; // Peluru Hilang
                    // DI SINI NANTI LOGIKA KURANGI NYAWA / GAME OVER
                    System.out.println("PLAYER KENA TEMBAK! AWAS!");
                }
            }
        }

        // 6. ANIMASI PLAYER
        spriteCounter++;
        if (isMoving) {
            if (spriteCounter > 10) { spriteNum = (spriteNum == 1) ? 2 : 1; spriteCounter = 0; }
        } else {
            if (spriteCounter > 20) { spriteNum = (spriteNum == 1) ? 2 : (spriteNum == 2) ? 3 : 1; spriteCounter = 0; }
        }
    }

    private boolean checkTreeCollision(int targetX, int targetY) {
        Rectangle playerRect = new Rectangle(targetX + 16, targetY + 32, 32, 32);
        for (Point p : trees) {
            Rectangle treeRect = new Rectangle(p.x + 32, p.y + 60, 32, 30);
            if (playerRect.intersects(treeRect)) return true;
        }
        return false;
    }

    // --- DRAWING ---
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Skala Layar
        double widthScale = (double) getWidth() / screenWidth;
        double heightScale = (double) getHeight() / screenHeight;
        double scale = Math.min(widthScale, heightScale);
        double xOffset = (getWidth() - (screenWidth * scale)) / 2;
        double yOffset = (getHeight() - (screenHeight * scale)) / 2;

        g2.translate(xOffset, yOffset);
        g2.scale(scale, scale);

        // Background & Pohon
        if (bgImage != null) g2.drawImage(bgImage, 0, 0, screenWidth, screenHeight, null);
        if (treeImage != null) for (Point p : trees) g2.drawImage(treeImage, p.x, p.y, treeSize, treeSize, null);

        // --- GAMBAR PELURU (PILIH GAMBAR SESUAI PEMILIKNYA) ---
        for (Bullet b : bullets) {
            if (b.isPlayerBullet) {
                b.draw(g2, bullets1); // Gambar peluru player
            } else {
                b.draw(g2, bullets2); // Gambar peluru monster
            }
        }

        // Gambar Monster Animasi
        for (Monster m : monsters) m.draw(g2, (m.spriteNum == 1) ? monster1 : monster2);

        // Gambar Player Animasi
        Image imageToDraw = isMoving ? ((spriteNum == 1) ? run1 : run2) : ((spriteNum == 1) ? idle1 : (spriteNum == 2) ? idle2 : idle3);
        int playerDrawSize = 96;
        int drawX = playerX - (playerDrawSize - tileSize) / 2;
        int drawY = playerY - (playerDrawSize - tileSize) / 2;
        if (imageToDraw != null) g2.drawImage(imageToDraw, drawX, drawY, playerDrawSize, playerDrawSize, null);

        // UI (Skor & Username)
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString("Player: " + username, 20, 30);
        g2.setColor(Color.YELLOW);
        g2.drawString("Score: " + playerScore, 20, 55);

        g2.dispose();
    }

    // --- INPUT MOUSE (PLAYER NEMBAK) ---
    @Override
    public void mousePressed(MouseEvent e) {
        // Konversi koordinat mouse layar ke dunia game
        double widthScale = (double) getWidth() / screenWidth;
        double heightScale = (double) getHeight() / screenHeight;
        double scale = Math.min(widthScale, heightScale);
        double xOffset = (getWidth() - (screenWidth * scale)) / 2;
        double yOffset = (getHeight() - (screenHeight * scale)) / 2;
        int gameMouseX = (int) ((e.getX() - xOffset) / scale);
        int gameMouseY = (int) ((e.getY() - yOffset) / scale);

        int playerCenterX = playerX + tileSize / 2;
        int playerCenterY = playerY + tileSize / 2;

        // Buat peluru baru. Parameter terakhir 'true' artinya ini PELURU PLAYER
        bullets.add(new Bullet(playerCenterX, playerCenterY, gameMouseX, gameMouseY, true));
    }

    @Override public void mouseClicked(MouseEvent e) {} @Override public void mouseReleased(MouseEvent e) {} @Override public void mouseEntered(MouseEvent e) {} @Override public void mouseExited(MouseEvent e) {}

    // --- INPUT KEYBOARD ---
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_W) upPressed = true;
        if (code == KeyEvent.VK_S) downPressed = true;
        if (code == KeyEvent.VK_A) leftPressed = true;
        if (code == KeyEvent.VK_D) rightPressed = true;
    }
    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_W) upPressed = false;
        if (code == KeyEvent.VK_S) downPressed = false;
        if (code == KeyEvent.VK_A) leftPressed = false;
        if (code == KeyEvent.VK_D) rightPressed = false;
    }
    @Override public void keyTyped(KeyEvent e) {}
}