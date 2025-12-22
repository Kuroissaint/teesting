package com.monster.game.view;

import com.monster.game.util.DBConnection; // Pastikan punya class ini

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList; // Anti-Crash saat nambah peluru

public class GamePanel extends JPanel implements Runnable, KeyListener, MouseListener {

    // --- 1. SETTINGAN LAYAR ---
    final int tileSize = 64;
    final int maxScreenCol = 16;
    final int maxScreenRow = 12;
    public final int screenWidth = tileSize * maxScreenCol;
    public final int screenHeight = tileSize * maxScreenRow;

    // --- 2. GAME ENGINE ---
    Thread gameThread;
    boolean isRunning = false;
    int FPS = 60;

    // STATUS GAME
    boolean isGameOver = false;
    boolean isScoreSaved = false;
    boolean isPaused = false;

    // REFERENCE KE VIEW UTAMA (Supaya bisa balik ke menu)
    private GameView parentView;

    // --- 3. DATA PLAYER & STATISTIK ---
    int playerX = 300;
    int playerY = 200;
    int playerSpeed = 5;
    String username;
    boolean isMoving = false;

    // STATISTIK DB
    int playerScore = 0;
    int playerAmmo = 0; // Mulai 0 (Cari peluru dari musuh yang meleset)
    int totalBulletsDodged = 0;

    // --- 4. ASSETS ---
    private Image run1, run2, idle1, idle2, idle3, bgImage, treeImage;
    private Image monster1, monster2;
    private Image bulletPlayerImg, bulletMonsterImg;

    // --- 5. OBJEK GAME ---
    ArrayList<Point> trees = new ArrayList<>();
    final int treeSize = 256; // Pohon Besar

    // Pakai CopyOnWriteArrayList biar aman dari error thread
    CopyOnWriteArrayList<Bullet> bullets = new CopyOnWriteArrayList<>();
    CopyOnWriteArrayList<Monster> monsters = new CopyOnWriteArrayList<>();
    int monsterSpawnCounter = 0;

    public int spriteCounter = 0;
    public int spriteNum = 1;

    boolean upPressed, downPressed, leftPressed, rightPressed;

    public GamePanel(String username, GameView parentView) {
        this.username = username;
        this.parentView = parentView; // Simpan referensi parent

        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.DARK_GRAY);
        this.setDoubleBuffered(true);
        this.setFocusable(true);

        this.addKeyListener(this);
        this.addMouseListener(this);

        loadImages();
        generateTrees();
    }

    private void loadImages() {
        try {
            bgImage = loadImage("/com/monster/game/assets/map_forest.png");
            treeImage = loadImage("/com/monster/game/assets/tree.png");

            run1 = loadImage("/com/monster/game/assets/mainch2.png");
            run2 = loadImage("/com/monster/game/assets/mainch3.png");
            idle1 = loadImage("/com/monster/game/assets/mainch1.png");
            idle2 = loadImage("/com/monster/game/assets/mainch2.png");
            idle3 = loadImage("/com/monster/game/assets/mainch3.png");

            monster1 = loadImage("/com/monster/game/assets/monster1.png");
            monster2 = loadImage("/com/monster/game/assets/monster2.png");

            bulletPlayerImg = loadImage("/com/monster/game/assets/bullets1.png");
            bulletMonsterImg = loadImage("/com/monster/game/assets/bullets2.png");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private Image loadImage(String path) {
        URL url = getClass().getResource(path);
        return (url != null) ? new ImageIcon(url).getImage() : null;
    }

    // Generate Pohon (Tidak di pinggir layar)
    public void generateTrees() {
        trees.clear();
        Random rand = new Random();
        int numberOfTrees = 3 + rand.nextInt(3);

        int minCol = 2; int maxCol = maxScreenCol - 4;
        int minRow = 2; int maxRow = maxScreenRow - 4;

        for (int i = 0; i < numberOfTrees; i++) {
            int x = (minCol + rand.nextInt(maxCol)) * tileSize;
            int y = (minRow + rand.nextInt(maxRow)) * tileSize;

            // Jarak aman dari spawn player
            if (Math.abs(x - playerX) > tileSize * 4 || Math.abs(y - playerY) > tileSize * 4) {
                trees.add(new Point(x, y));
            }
        }
    }

    public void resetGame() {
        playerX = 300; playerY = 200;
        playerScore = 0;
        playerAmmo = 0;
        totalBulletsDodged = 0;

        bullets.clear();
        monsters.clear();
        isGameOver = false;
        isScoreSaved = false;
        isMoving = false;
        isPaused = false;
    }

    // --- QUIT TO MENU ---
    public void quitToMenu() {
        // Kalau keluar pas lagi main (bukan pas game over), simpan progres!
        if (!isGameOver) {
            saveScoreToDB("ONGOING");
        }

        isRunning = false;
        gameThread = null;
        parentView.showMainMenu();
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

    // --- LOGIKA UTAMA ---
    public void update() {
        // 1. Cek Game Over & Save DB
        if (isPaused) {
            return; // Kalau pause, jangan update posisi apapun!
        }

        if (isGameOver) {
            if (!isScoreSaved) {
                saveScoreToDB("GAMEOVER"); // Simpan status MATI
                isScoreSaved = true;
            }
            return;
        }

        // 2. Gerak Player
        isMoving = false;
        int nextX = playerX; int nextY = playerY;
        if (upPressed) { nextY -= playerSpeed; isMoving = true; }
        if (downPressed) { nextY += playerSpeed; isMoving = true; }
        if (leftPressed) { nextX -= playerSpeed; isMoving = true; }
        if (rightPressed) { nextX += playerSpeed; isMoving = true; }

        if (!checkTreeCollision(nextX, nextY)) {
            playerX = nextX; playerY = nextY;
        }
        playerX = Math.max(0, Math.min(screenWidth - tileSize, playerX));
        playerY = Math.max(0, Math.min(screenHeight - tileSize, playerY));

        // 3. Update Peluru (Logika Scavenger Ammo)
        for (Bullet b : bullets) {
            if (b.isActive) {
                b.update();

                // A. Kena Pohon -> Hancur (Gak dapet ammo)
                if (checkBulletTreeCollision(b)) {
                    b.isActive = false;
                    continue;
                }

                // B. Keluar Layar -> Cek Ammo
                if (b.x < -50 || b.x > screenWidth + 50 || b.y < -50 || b.y > screenHeight + 50) {
                    b.isActive = false;

                    // Kalau peluru MUSUH keluar layar (artinya player berhasil menghindar)
                    if (!b.isPlayerBullet) {
                        playerAmmo++;
                        totalBulletsDodged++;
                        System.out.println("Dapat Ammo! Total Meleset: " + totalBulletsDodged);
                    }
                }
            } else {
                bullets.remove(b);
            }
        }

        // 4. Spawn Monster (Di pinggir layar)
        monsterSpawnCounter++;
        if (monsterSpawnCounter > 100) {
            Random rand = new Random();
            int spawnX;
            // Kiri atau Kanan layar
            if (rand.nextBoolean()) spawnX = rand.nextInt(screenWidth / 3);
            else spawnX = (screenWidth * 2 / 3) + rand.nextInt(screenWidth / 3);

            monsters.add(new Monster(spawnX, screenHeight));
            monsterSpawnCounter = 0;
        }

        // 5. Update Monster
        int playerCenterX = playerX + tileSize / 2;
        int playerCenterY = playerY + tileSize / 2;

        for (Monster m : monsters) {
            if (m.isActive) {
                m.update(playerX, playerY); // Kejar Player
                m.shotCounter++;
                if (m.shotCounter > 150) {
                    bullets.add(new Bullet(m.x + 32, m.y + 32, playerCenterX, playerCenterY, false));
                    m.shotCounter = 0;
                }
            } else {
                monsters.remove(m);
            }
        }

        // 6. Cek Tabrakan (Player kena peluru/monster)
        checkCollisions();

        // 7. Animasi
        spriteCounter++;
        if (isMoving) {
            if (spriteCounter > 10) { spriteNum = (spriteNum == 1) ? 2 : 1; spriteCounter = 0; }
        } else {
            if (spriteCounter > 20) { spriteNum = (spriteNum == 1) ? 2 : (spriteNum == 2) ? 3 : 1; spriteCounter = 0; }
        }
    }

    // --- DATABASE SAVE ---
    private void saveScoreToDB(String statusGame) {
        System.out.println("Menyimpan Data (" + statusGame + ")...");
        try {
            Connection conn = DBConnection.getConnection();

            // Query SQL ditambah kolom 'status'
            String sql = "INSERT INTO tbenefit (username, skor, peluru_meleset, sisa_peluru, status) VALUES (?, ?, ?, ?, ?)";

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, this.username);
            pstmt.setInt(2, this.playerScore);
            pstmt.setInt(3, this.totalBulletsDodged);
            pstmt.setInt(4, this.playerAmmo);
            pstmt.setString(5, statusGame); // Masukkan ONGOING atau GAMEOVER

            pstmt.executeUpdate();
            System.out.println("SUKSES SAVE: " + statusGame);
            pstmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- COLLISION LOGIC ---
    private boolean checkBulletTreeCollision(Bullet b) {
        Rectangle bulletRect = b.getBounds();
        for (Point p : trees) {
            // Hitbox batang pohon
            int trunkWidth = 60;
            int trunkHeight = 40;
            int offsetX = (treeSize - trunkWidth) / 2;
            int offsetY = treeSize - trunkHeight - 20;
            Rectangle treeRect = new Rectangle(p.x + offsetX, p.y + offsetY, trunkWidth, trunkHeight);

            if (bulletRect.intersects(treeRect)) return true;
        }
        return false;
    }

    private void checkCollisions() {
        Rectangle playerBodyRect = new Rectangle(playerX + 16, playerY + 16, 32, 48);
        for (Bullet b : bullets) {
            if (!b.isActive) continue;
            Rectangle bulletRect = b.getBounds();

            if (b.isPlayerBullet) {
                // Peluru Player kena Monster
                for (Monster m : monsters) {
                    if (m.isActive && bulletRect.intersects(m.getBounds())) {
                        m.isActive = false;
                        b.isActive = false;
                        playerScore += 10;
                    }
                }
            } else {
                // Peluru Monster kena Player
                if (bulletRect.intersects(playerBodyRect)) {
                    b.isActive = false;
                    isGameOver = true;
                }
            }
        }
        // Monster Nabrak Player
        for (Monster m : monsters) {
            if (m.isActive && m.getBounds().intersects(playerBodyRect)) {
                isGameOver = true;
            }
        }
    }

    private boolean checkTreeCollision(int targetX, int targetY) {
        Rectangle playerFeetRect = new Rectangle(targetX + 16, targetY + 32, 32, 32);
        for (Point p : trees) {
            int trunkWidth = 60;
            int trunkHeight = 40;
            int offsetX = (treeSize - trunkWidth) / 2;
            int offsetY = treeSize - trunkHeight - 20;
            Rectangle treeRect = new Rectangle(p.x + offsetX, p.y + offsetY, trunkWidth, trunkHeight);
            if (playerFeetRect.intersects(treeRect)) return true;
        }
        return false;
    }

    // --- RENDERING / GAMBAR ---
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Scaling
        double widthScale = (double) getWidth() / screenWidth;
        double heightScale = (double) getHeight() / screenHeight;
        double scale = Math.min(widthScale, heightScale);
        double xOffset = (getWidth() - (screenWidth * scale)) / 2;
        double yOffset = (getHeight() - (screenHeight * scale)) / 2;

        g2.translate(xOffset, yOffset);
        g2.scale(scale, scale);

        // 1. Background
        if (bgImage != null) g2.drawImage(bgImage, 0, 0, screenWidth, screenHeight, null);

        // 2. Objek (Monster & Peluru)
        for (Monster m : monsters) m.draw(g2, (m.spriteNum == 1) ? monster1 : monster2);
        for (Bullet b : bullets) {
            if (b.isPlayerBullet) b.draw(g2, bulletPlayerImg);
            else b.draw(g2, bulletMonsterImg);
        }

        // 3. Player
        Image imageToDraw = isMoving ? ((spriteNum == 1) ? run1 : run2) : ((spriteNum == 1) ? idle1 : (spriteNum == 2) ? idle2 : idle3);
        int playerDrawSize = 96;
        int drawX = playerX - (playerDrawSize - tileSize) / 2;
        int drawY = playerY - (playerDrawSize - tileSize) / 2;
        if (imageToDraw != null) g2.drawImage(imageToDraw, drawX, drawY, playerDrawSize, playerDrawSize, null);

        // 4. Pohon (Digambar paling atas biar nutupin player/monster)
        if (treeImage != null) {
            for (Point p : trees) g2.drawImage(treeImage, p.x, p.y, treeSize, treeSize, null);
        }

        // 5. UI HUD
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString("Player: " + username, 20, 30);
        g2.setColor(Color.YELLOW);
        g2.drawString("Score: " + playerScore, 20, 55);
        g2.setColor(Color.CYAN);
        g2.drawString("Ammo: " + playerAmmo, 20, 80);
        g2.setColor(Color.ORANGE);
        g2.drawString("Dodged: " + totalBulletsDodged, 20, 105);

        // --- LAYAR PAUSE ---
        if (isPaused && !isGameOver) {
            g2.setColor(new Color(0, 0, 0, 150)); // Hitam transparan
            g2.fillRect(0, 0, screenWidth, screenHeight);

            drawCenteredText(g2, "PAUSED", screenHeight/2 - 50, Color.YELLOW, 72);
            drawCenteredText(g2, "Press [R] to Resume", screenHeight/2 + 20, Color.WHITE, 32);
            drawCenteredText(g2, "Press [Q] to Quit to Menu", screenHeight/2 + 60, Color.WHITE, 32);
        }

        // --- LAYAR GAME OVER (UPDATE: Tambah Tombol Quit) ---
        if (isGameOver) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, screenWidth, screenHeight);

            drawCenteredText(g2, "GAME OVER", screenHeight/2 - 50, Color.RED, 72);
            drawCenteredText(g2, "Press [SPACE] to Restart", screenHeight/2 + 20, Color.WHITE, 32);
            drawCenteredText(g2, "Press [Q] to Quit to Menu", screenHeight/2 + 60, Color.WHITE, 32);

            String scoreText = "Final Score: " + playerScore;
            drawCenteredText(g2, scoreText, screenHeight/2 + 110, Color.YELLOW, 24);
        }
        g2.dispose();
    }

    // Helper buat bikin teks tengah biar rapi
    private void drawCenteredText(Graphics2D g2, String text, int y, Color c, int size) {
        g2.setColor(c);
        g2.setFont(new Font("Arial", Font.BOLD, size));
        int len = (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        g2.drawString(text, (screenWidth - len) / 2, y);
    }

    // --- INPUT ---
    @Override
    public void mousePressed(MouseEvent e) {
        if (isGameOver) return;
        if (playerAmmo <= 0) return; // Harus punya ammo

        double widthScale = (double) getWidth() / screenWidth;
        double heightScale = (double) getHeight() / screenHeight;
        double scale = Math.min(widthScale, heightScale);
        double xOffset = (getWidth() - (screenWidth * scale)) / 2;
        double yOffset = (getHeight() - (screenHeight * scale)) / 2;
        int gameMouseX = (int) ((e.getX() - xOffset) / scale);
        int gameMouseY = (int) ((e.getY() - yOffset) / scale);

        int playerCenterX = playerX + tileSize / 2;
        int playerCenterY = playerY + tileSize / 2;

        bullets.add(new Bullet(playerCenterX, playerCenterY, gameMouseX, gameMouseY, true));
        playerAmmo--;
    }

    @Override public void mouseClicked(MouseEvent e) {} @Override public void mouseReleased(MouseEvent e) {} @Override public void mouseEntered(MouseEvent e) {} @Override public void mouseExited(MouseEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        // 1. TOMBOL PAUSE (ESC)
        if (code == KeyEvent.VK_ESCAPE) {
            if (!isGameOver) {
                isPaused = !isPaused; // Toggle (Nyala/Mati)
            }
        }

        // 2. TOMBOL MENU SAAT PAUSE / GAME OVER (Q)
        if (code == KeyEvent.VK_Q) {
            if (isPaused || isGameOver) {
                quitToMenu();
            }
        }

        // 3. TOMBOL RESUME (R)
        if (code == KeyEvent.VK_R) {
            if (isPaused) isPaused = false;
        }

        // 4. TOMBOL RESTART (SPASI)
        if (code == KeyEvent.VK_SPACE) {
            if (isGameOver) resetGame();
        }

        // Kontrol Gerak (Hanya kalau gak pause & gak game over)
        if (!isPaused && !isGameOver) {
            if (code == KeyEvent.VK_W) upPressed = true;
            if (code == KeyEvent.VK_S) downPressed = true;
            if (code == KeyEvent.VK_A) leftPressed = true;
            if (code == KeyEvent.VK_D) rightPressed = true;
        }
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