package com.monster.game.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.URL;

public class GamePanel extends JPanel implements Runnable, KeyListener {

    // --- 1. SETTINGAN LAYAR ---
    final int tileSize = 64;
    final int maxScreenCol = 16;
    final int maxScreenRow = 12;
    final int screenWidth = tileSize * maxScreenCol;
    final int screenHeight = tileSize * maxScreenRow;

    // --- 2. GAME ENGINE ---
    Thread gameThread;
    boolean isRunning = false;
    int FPS = 60;

    // --- 3. DATA PLAYER ---
    int playerX = 100;
    int playerY = 100;
    int playerSpeed = 4;
    String username;

    boolean isMoving = false;

    // --- 4. GAMBAR & ANIMASI ---
    private Image run1, run2;
    private Image idle1, idle2, idle3;
    private Image bgImage;

    public int spriteCounter = 0;
    public int spriteNum = 1;
    // ----------------------------

    // KONTROL KEYBOARD
    boolean upPressed, downPressed, leftPressed, rightPressed;

    public GamePanel(String username) {
        this.username = username;

        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.DARK_GRAY);
        this.setDoubleBuffered(true);
        this.addKeyListener(this);
        this.setFocusable(true);

        loadImages();
    }

    // --- LOAD GAMBAR ---
    private void loadImages() {
        try {

            // Gambar Background Permainan
            URL bgUrl = getClass().getResource("/com/monster/game/assets/map_forest.png");
            if (bgUrl != null) bgImage = new ImageIcon(bgUrl).getImage();

            // Gambar Lari (mainch2 & mainch3)
            run1 = loadImage("/com/monster/game/assets/mainch2.png");
            run2 = loadImage("/com/monster/game/assets/mainch3.png");

            // Gambar Idle (mainch1, mainch2, mainch3)
            idle1 = loadImage("/com/monster/game/assets/mainch1.png");
            idle2 = loadImage("/com/monster/game/assets/mainch2.png");
            idle3 = loadImage("/com/monster/game/assets/mainch3.png");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Image loadImage(String path) {
        URL url = getClass().getResource(path);
        if (url != null) return new ImageIcon(url).getImage();
        else {
            System.err.println("GAMBAR HILANG: " + path); // Cek error di sini
            return null;
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
        long currentTime;

        while (gameThread != null) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    // --- LOGIKA UTAMA (Semua hitungan matematika di sini) ---
    public void update() {
        // 1. Reset Status Gerak
        isMoving = false;

        // 2. Update Posisi Berdasarkan Tombol
        // Kita pakai variabel sementara dulu biar gampang dicek
        if (upPressed) {
            playerY -= playerSpeed;
            isMoving = true;
        }
        if (downPressed) {
            playerY += playerSpeed;
            isMoving = true;
        }
        if (leftPressed) {
            playerX -= playerSpeed;
            isMoving = true;
        }
        if (rightPressed) {
            playerX += playerSpeed;
            isMoving = true;
        }

        // --- 3. PEMBATAS AREA (BOUNDARY CHECK) ---
        // Biar nggak keluar KIRI
        if (playerX < 0) {
            playerX = 0;
        }
        // Biar nggak keluar ATAS
        if (playerY < 0) {
            playerY = 0;
        }
        // Biar nggak keluar KANAN
        // (screenWidth - tileSize) karena koordinat X itu ada di KIRI gambar
        if (playerX > screenWidth - tileSize) {
            playerX = screenWidth - tileSize;
        }
        // Biar nggak keluar BAWAH
        if (playerY > screenHeight - tileSize) {
            playerY = screenHeight - tileSize;
        }

        // 4. LOGIKA ANIMASI
        spriteCounter++;
        if (isMoving) {
            // Animasi Lari
            if (spriteCounter > 10) {
                if (spriteNum == 1) spriteNum = 2;
                else spriteNum = 1;
                spriteCounter = 0;
            }
        } else {
            // Animasi Idle
            if (spriteCounter > 20) {
                if (spriteNum == 1) spriteNum = 2;
                else if (spriteNum == 2) spriteNum = 3;
                else spriteNum = 1;
                spriteCounter = 0;
            }
        }
    }

    // --- GAMBAR KE LAYAR (Cuma boleh gambar, jangan mikir di sini) ---
    // --- GAMBAR KE LAYAR (DENGAN SKALA DINAMIS) ---
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // 1. HITUNG SKALA (ZOOM)
        // Bandingkan ukuran jendela saat ini (getWidth) dengan ukuran asli game (screenWidth)
        double widthScale = (double) getWidth() / screenWidth;
        double heightScale = (double) getHeight() / screenHeight;

        // Pilih skala terkecil biar GAMBAR GAK PENYOK (Aspect Ratio terjaga)
        double scale = Math.min(widthScale, heightScale);

        // 2. HITUNG POSISI TENGAH (CENTERING)
        // Kalau jendela melebar, hitung sisa ruang biar game tetap di tengah (Letterboxing)
        double xOffset = (getWidth() - (screenWidth * scale)) / 2;
        double yOffset = (getHeight() - (screenHeight * scale)) / 2;

        // 3. TERAPKAN TRANSFORMASI (ZOOM & GESER)
        g2.translate(xOffset, yOffset); // Geser ke tengah
        g2.scale(scale, scale);         // Zoom sesuai ukuran jendela

        // --- MULAI GAMBAR SEPERTI BIASA (Koordinat tetap pakai ukuran asli) ---

        // --- GAMBAR BACKGROUND DULUAN ---
        if (bgImage != null) {
            // Gambar map memenuhi layar (screenWidth & screenHeight adalah ukuran asli game)
            g2.drawImage(bgImage, 0, 0, screenWidth, screenHeight, null);
        } else {
            // Cadangan kalau gambar belum ada
            g2.setColor(new Color(47, 72, 78)); // Warna Hijau Gelap
            g2.fillRect(0, 0, screenWidth, screenHeight);
        }

        // Info User
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        // Font juga otomatis ikut membesar karena kita men-scale satu layar
        g2.drawString("Player: " + username, 20, 30);

        Image imageToDraw = null;

        if (isMoving) {
            // PILIH GAMBAR LARI
            if (spriteNum == 1) imageToDraw = run1;
            else imageToDraw = run2;
        } else {
            // PILIH GAMBAR IDLE
            if (spriteNum == 1) imageToDraw = idle1;
            else if (spriteNum == 2) imageToDraw = idle2;
            else if (spriteNum == 3) imageToDraw = idle3;
            else imageToDraw = idle1;
        }

        // Gambar Player

        int playerDrawSize = 128;
        int drawX = playerX - (playerDrawSize - tileSize) / 2;
        int drawY = playerY - (playerDrawSize - tileSize) / 2;

        if (imageToDraw != null) {
            g2.drawImage(imageToDraw, playerX, playerY, tileSize, tileSize, null);
        } else {
            g2.setColor(Color.CYAN);
            g2.fillRect(playerX, playerY, tileSize, tileSize);
        }

        g2.dispose();
    }

    @Override
    public void keyTyped(KeyEvent e) {}

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
}