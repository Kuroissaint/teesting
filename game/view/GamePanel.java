package com.monster.game.view;

import com.monster.game.util.DBConnection; // Pastikan punya class ini
import com.monster.game.util.Sound;

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

    boolean debugMode = false; // <--- TAMBAHKAN INI

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
    private Image shield1, shield2; // Gambar shield

    // --- 4. ASSETS ---
    private Image run1, run2, idle1, idle2, idle3, bgImage, treeImage;
    private Image monster1, monster2, monster3, monster4;
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
    boolean hasShield = false;      // Apakah player MENYIMPAN shield? (Siap pakai)
    boolean isShieldActive = false; // Apakah shield SEDANG DIPAKAI? (Kebal)

    int shieldAmmoCounter = 0;      // Hitung ammo buat dapet shield (Target 10)
    int shieldTimer = 0;            // Timer durasi shield
    final int shieldMaxTime = 180;  // 3 Detik (60 FPS x 3)

    // SETUP SOUND
    Sound sound = new Sound();

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
            monster3 = loadImage("/com/monster/game/assets/monster3.png");
            monster4 = loadImage("/com/monster/game/assets/monster4.png");

            bulletPlayerImg = loadImage("/com/monster/game/assets/bullets1.png");
            bulletMonsterImg = loadImage("/com/monster/game/assets/bullets2.png");

            shield1 = loadImage("/com/monster/game/assets/shield1.png");
            shield2 = loadImage("/com/monster/game/assets/shield2.png");
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

        // NYALAKAN MUSIK (Index 0)
        playMusic(0);
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

        // --- LOGIKA UPDATE SHIELD (Durasi) ---
        if (isShieldActive) {
            shieldTimer++;
            if (shieldTimer > shieldMaxTime) {
                isShieldActive = false; // Durasi habis
                shieldTimer = 0;
                System.out.println("Shield DEACTIVATED!");
            }
        }

        // 3. Update Peluru (Logika Scavenger Ammo)
        for (Bullet b : bullets) {
            if (b.isActive) {
                b.update();

                // --- PERUBAHAN DI SINI: CEK MONSTER DULUAN ---
                // Supaya kalau ada Kelelawar di atas pohon, dia yang kena tembak, bukan pohonnya.
                boolean hitMonster = false;

                if (b.isPlayerBullet) {
                    for (Monster m : monsters) {
                        // Cek tabrakan peluru dengan monster yang masih hidup
                        if (m.isActive && b.getBounds().intersects(m.getBounds())) {
                            m.isActive = false;
                            b.isActive = false; // Peluru hancur
                            playerScore += 10;
                            hitMonster = true;
                            break; // Satu peluru cuma bisa kena satu monster
                        }
                    }
                }

                // Kalau sudah kena monster, stop proses peluru ini (jangan cek pohon lagi)
                if (hitMonster) continue;

                // --- BARU CEK KENA POHON ---
                if (checkBulletTreeCollision(b)) {
                    b.isActive = false;
                    continue;
                }

                // B. Keluar Layar -> Cek Ammo
                if (b.x < -50 || b.x > screenWidth + 50 || b.y < -50 || b.y > screenHeight + 50) {
                    b.isActive = false;

                    if (!b.isPlayerBullet) {
                        playerAmmo++;
                        totalBulletsDodged++;

                        // --- LOGIKA DAPET SHIELD ---
                        shieldAmmoCounter++;
                        if (shieldAmmoCounter >= 10) {
                            shieldAmmoCounter = 0; // Reset counter
                            if (!hasShield) {
                                hasShield = true; // Dapet Shield!
                                System.out.println("Shield OBTAINED!");
                            }
                        }
                    }
                }
            } else {
                bullets.remove(b);
            }
        }

        // 4. Spawn Monster (Di pinggir layar)
        monsterSpawnCounter++;
        if (monsterSpawnCounter > 100) {
            // --- PENTING: DEKLARASI rand HARUS ADA DI SINI ---
            Random rand = new Random();

            int spawnX;
            // Kiri atau Kanan layar
            if (rand.nextBoolean()) spawnX = rand.nextInt(screenWidth / 3);
            else spawnX = (screenWidth * 2 / 3) + rand.nextInt(screenWidth / 3);

            // --- LOGIKA TIPE MONSTER ---
            int tipeMonster;
            // Gunakan rand yang sudah dibuat di atas
            if (rand.nextInt(10) < 8) {
                tipeMonster = 1; // 60% Kelelawar (Rusher)
            } else {
                tipeMonster = 2; // 40% Tanaman (Shooter)
            }

            // Spawn Monster dengan Tipe
            monsters.add(new Monster(spawnX, screenHeight, tipeMonster));
            monsterSpawnCounter = 0;

            if (tipeMonster == 1) {
                playSE(3); // Bunyi Tanaman (Index 3)
            } else {
                playSE(2); // Bunyi Kelelawar (Index 2)
            }

        }

        // 5. Update Monster
        int playerCenterX = playerX + tileSize / 2;
        int playerCenterY = playerY + tileSize / 2;

        for (Monster m : monsters) {
            if (m.isActive) {
                m.update(playerX, playerY, trees);

                // Logika Nembak (Sesuai Tipe)
                m.shotCounter++;

                int batasNembak;
                if (m.type == 1) {
                    batasNembak = 100; // Tanaman: Sering nembak
                } else {
                    batasNembak = 300; // Kelelawar: Jarang nembak
                }

                if (m.shotCounter > batasNembak) {
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

            // KITA PAKAI UPDATE (Karena data sudah dibuat di awal oleh Reducer)
            String sql = "UPDATE tbenefit SET skor=?, peluru_meleset=?, sisa_peluru=?, status=? WHERE username=?";

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, this.playerScore);
            pstmt.setInt(2, this.totalBulletsDodged);
            pstmt.setInt(3, this.playerAmmo);
            pstmt.setString(4, statusGame);
            pstmt.setString(5, this.username); // Where clause

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("SUKSES UPDATE: " + statusGame);
            } else {
                // Jaga-jaga kalau usernya dihapus manual pas main
                System.out.println("User hilang? Mencoba Insert ulang...");
                // (Opsional: Bisa insert manual di sini kalau mau)
            }

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

            int hitBoxWidth = 100;  // Lebar area tabrak (sebelumnya cuma 60)
            int hitBoxHeight = 150; // Tinggi area tabrak (sebelumnya cuma 40)

            // Posisikan di tengah-tengah gambar pohon
            int offsetX = (treeSize - hitBoxWidth) / 2;
            int offsetY = (treeSize - hitBoxHeight) / 2;

            Rectangle treeRect = new Rectangle(p.x + offsetX, p.y + offsetY, hitBoxWidth, hitBoxHeight);

            if (bulletRect.intersects(treeRect)) {
                return true; // Kena pohon!
            }
        }
        return false;
    }

    private void checkCollisions() {
        Rectangle playerBodyRect = new Rectangle(playerX + 16, playerY + 16, 32, 48);

        // 1. Cek Peluru Musuh kena Player
        for (Bullet b : bullets) {
            if (!b.isActive || b.isPlayerBullet) continue;

            if (b.getBounds().intersects(playerBodyRect)) {

                // --- CEK SHIELD DULU ---
                if (isShieldActive) {
                    b.isActive = false; // Peluru ditangkis (hancur)
                    continue; // Player selamat, lanjut ke peluru berikutnya
                }

                // Kalau gak pake shield, baru mati
                b.isActive = false;
                isGameOver = true;

                stopMusic(); // Matikan lagu latar biar dramatis
                playSE(5);   // Bunyi Game Over (Index 5)
            }
        }

        // 2. Cek Monster Nabrak Player
        for (Monster m : monsters) {
            if (m.isActive && m.getBounds().intersects(playerBodyRect)) {
                // Shield juga bisa melindungi dari tabrakan monster (Opsional)
                if (isShieldActive) {
                    // Kalau mau monsternya mental atau mati bisa diatur di sini
                    // Untuk sekarang kita bikin player tembus aja (invincible)
                    continue;
                }
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

        for (Monster m : monsters) {
            if (m.type == 1) {
                Image imgToDraw = (m.spriteNum == 1) ? monster3 : monster4;
                m.draw(g2, imgToDraw);
            }
        }

        // --- GAMBAR EFEK SHIELD ---
        if (isShieldActive) {
            // Animasi kedap-kedip cepat (Ganti gambar tiap 5 frame)
            Image shieldImg = (spriteCounter % 10 < 5) ? shield1 : shield2;

            // Gambar agak transparan dikit biar keren (Opsional)
            // g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));

            // Gambar di posisi player (tengah)
            int shieldSize = 100; // Sesuaikan ukuran shield
            int sx = playerX + (32 - shieldSize)/2 + 16; // Center X
            int sy = playerY + (40 - shieldSize)/2 + 16; // Center Y

            if (shieldImg != null) g2.drawImage(shieldImg, sx, sy, shieldSize, shieldSize, null);

            // Reset transparansi (PENTING kalau pake setComposite)
            // g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }

        // 2. Player
        Image imageToDraw = isMoving ? ((spriteNum == 1) ? run1 : run2) : ((spriteNum == 1) ? idle1 : (spriteNum == 2) ? idle2 : idle3);
        int playerDrawSize = 96;
        int drawX = playerX - (playerDrawSize - tileSize) / 2;
        int drawY = playerY - (playerDrawSize - tileSize) / 2;
        if (imageToDraw != null) g2.drawImage(imageToDraw, drawX, drawY, playerDrawSize, playerDrawSize, null);

        // 3. Pohon (Digambar paling atas biar nutupin player/monster)
        if (treeImage != null) {
            for (Point p : trees) g2.drawImage(treeImage, p.x, p.y, treeSize, treeSize, null);
        }

        // 4. Objek (Monster & Peluru)
        for (Monster m : monsters) {
            if (m.type == 2) {
                Image imgToDraw = (m.spriteNum == 1) ? monster1 : monster2;
                m.draw(g2, imgToDraw);
            }
        }
        for (Bullet b : bullets) {
            if (b.isPlayerBullet) b.draw(g2, bulletPlayerImg);
            else b.draw(g2, bulletMonsterImg);
        }

        // --- 5. UI HUD (TAMPILAN BARU) ---

        // A. Background Bar Transparan di Atas (Biar teks jelas terbaca)
        g2.setColor(new Color(0, 0, 0, 150)); // Hitam transparan
        g2.fillRect(0, 0, screenWidth, 60);   // Tinggi bar 60 pixel dari atas

        // B. Setting Font Lebih Bagus
        g2.setFont(new Font("Segoe UI", Font.BOLD, 22)); // Font tegas & tebal

        // C. Posisi Horizontal (Memanjang ke Samping)
        int yPos = 38; // Posisi vertikal sejajar

        // Item 1: Player Name (Paling Kiri)
        drawOutlinedText(g2, "PLAYER: " + username, 30, yPos, Color.WHITE);

        // Item 2: Score (Geser ke kanan)
        drawOutlinedText(g2, "SCORE: " + playerScore, 300, yPos, Color.YELLOW);

        // Item 3: Ammo (Geser lagi)
        drawOutlinedText(g2, "AMMO: " + playerAmmo, 520, yPos, Color.CYAN);

        // Item 4: Dodged (Paling Kanan)
        drawOutlinedText(g2, "MISSED: " + totalBulletsDodged, 750, yPos, Color.ORANGE);

        // Item 5: Status Shield (Ready/Not Ready)
        if (hasShield) {
            drawOutlinedText(g2, "[F] SHIELD READY!", 30, 80, Color.GREEN); // Warna Hijau di bawah nama
        } else if (isShieldActive) {
            drawOutlinedText(g2, "SHIELD ACTIVE!", 30, 80, Color.CYAN);
        } else {
            // Tampilkan progress bar sederhana (Opsional)
            drawOutlinedText(g2, "Shield: " + shieldAmmoCounter + "/10", 30, 80, Color.GRAY);
        }

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

        // --- FITUR VISUALISASI HITBOX (DEBUG MODE) ---
        if (debugMode) {
            g2.setStroke(new BasicStroke(1)); // Garis tipis

            // 1. HITBOX PLAYER
            // Badan (Area kena damage) - Warna Merah
            g2.setColor(Color.RED);
            g2.drawRect(playerX + 16, playerY + 16, 32, 48);

            // Kaki (Area tabrak pohon) - Warna Biru
            g2.setColor(Color.BLUE);
            g2.drawRect(playerX + 16, playerY + 32, 32, 32);

            // 2. HITBOX MONSTER (Kuning)
            g2.setColor(Color.YELLOW);
            for (Monster m : monsters) {
                if (m.isActive) {
                    Rectangle r = m.getBounds();
                    g2.drawRect(r.x, r.y, r.width, r.height);
                }
            }

            // 3. HITBOX PELURU (Hijau)
            g2.setColor(Color.GREEN);
            for (Bullet b : bullets) {
                if (b.isActive) {
                    Rectangle r = b.getBounds();
                    g2.drawRect(r.x, r.y, r.width, r.height);
                }
            }

            // 4. HITBOX POHON (Putih)
            g2.setColor(Color.WHITE);
            for (Point p : trees) {
                // Logika ukuran hitbox pohon (sesuaikan dengan kode collision kamu)
                int trunkWidth = 60;
                int trunkHeight = 40;
                int offsetX = (treeSize - trunkWidth) / 2;
                int offsetY = treeSize - trunkHeight - 20;

                g2.drawRect(p.x + offsetX, p.y + offsetY, trunkWidth, trunkHeight);
            }

            // Tulis status Debug di pojok
            g2.setColor(Color.RED);
            g2.drawString("DEBUG MODE ON", 10, screenHeight - 10);
        }

        g2.dispose();
    }

    // --- FITUR LOAD GAME ---
    public void loadSession(int savedScore, int savedAmmo, int savedDodged) {
        this.playerScore = savedScore;
        this.playerAmmo = savedAmmo;
        this.totalBulletsDodged = savedDodged;

        System.out.println("Game Dilanjutkan! Skor: " + savedScore);
    }

    // Helper buat bikin teks tengah biar rapi
    private void drawCenteredText(Graphics2D g2, String text, int y, Color c, int size) {
        g2.setColor(c);
        g2.setFont(new Font("Arial", Font.BOLD, size));
        int len = (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        g2.drawString(text, (screenWidth - len) / 2, y);
    }

    // --- METHOD HELPER UNTUK TEKS KEREN (OUTLINE) ---
    private void drawOutlinedText(Graphics2D g2, String text, int x, int y, Color c) {
        g2.setColor(Color.BLACK); // Warna Outline (Hitam)
        // Gambar 4 kali geser dikit (Atas, Bawah, Kiri, Kanan) buat efek tebal
        g2.drawString(text, x-2, y);
        g2.drawString(text, x+2, y);
        g2.drawString(text, x, y-2);
        g2.drawString(text, x, y+2);

        g2.setColor(c); // Warna Utama
        g2.drawString(text, x, y);
    }

    // --- METHOD UNTUK MUSIK & SFX ---
    public void playMusic(int i) {
        sound.setFile(i);
        sound.play();
        sound.loop(); // Musik latar bakal ngeloop terus
    }

    public void stopMusic() {
        sound.stop();
    }

    public void playSE(int i) {
        sound.setFile(i); // SE = Sound Effect
        sound.play(); // Cuma bunyi sekali (gak ngeloop)
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

        playSE(1);
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

        // 5. TOMBOL DEBUG (T) - Tampilkan Hitbox
        if (code == KeyEvent.VK_T) {
            debugMode = !debugMode; // Hidup/Mati
        }

        // 6. TOMBOL SHIELD (F)
        if (code == KeyEvent.VK_F) {
            // Syarat: Punya simpenan shield DAN Shield tidak sedang aktif
            if (hasShield && !isShieldActive) {
                hasShield = false; // Pake simpenan
                isShieldActive = true; // Aktifkan mode kebal
                shieldTimer = 0; // Mulai hitung 3 detik

                playSE(4);
            }
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