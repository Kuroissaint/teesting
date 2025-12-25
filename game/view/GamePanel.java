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

    // STATUS PLASMA
    boolean isPlasmaReady = false;

    boolean debugMode = false; // <--- TAMBAHKAN INI

    // REFERENCE KE VIEW UTAMA (Supaya bisa balik ke menu)
    private GameView parentView;

    // --- 3. DATA PLAYER & STATISTIK ---
    int playerX = (tileSize * maxScreenCol) / 2 - (tileSize / 2);
    int playerY = (tileSize * maxScreenRow) / 2 - (tileSize / 2);
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
    private Image plasma1, plasma2;

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

    // TRACKING MOUSE
    int currentMouseX = 0;
    int currentMouseY = 0;
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
            plasma1 = loadImage("/com/monster/game/assets/plasma1.png");
            plasma2 = loadImage("/com/monster/game/assets/plasma2.png");

            shield1 = loadImage("/com/monster/game/assets/shield1.png");
            shield2 = loadImage("/com/monster/game/assets/shield2.png");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private Image loadImage(String path) {
        URL url = getClass().getResource(path);
        return (url != null) ? new ImageIcon(url).getImage() : null;
    }

    // Generate Pohon (Versi Anti-Tengah)
    public void generateTrees() {
        trees.clear();
        Random rand = new Random();

        // Mau berapa pohon? (Misal 6 sampai 9 pohon)
        int targetTreeCount = 3 + rand.nextInt(3);

        int treesSpawned = 0;

        // Kita pakai WHILE loop, biar dia terus mencoba sampai jumlah pohon terpenuhi
        // (Jadi kalau gagal spawn karena di tengah, dia bakal cari tempat lain)
        while (treesSpawned < targetTreeCount) {

            // Random Kolom & Baris (Seluruh layar)
            int col = rand.nextInt(maxScreenCol);
            int row = rand.nextInt(maxScreenRow);

            int x = col * tileSize;
            int y = row * tileSize;

            // HITUNG JARAK KE PLAYER (YANG ADA DI TENGAH)
            double distance = Math.sqrt(Math.pow(x - playerX, 2) + Math.pow(y - playerY, 2));

            // --- ATURAN JARAK ---
            // Jarak harus lebih besar dari 3 kotak (192 pixel)
            // Semakin besar angkanya, semakin luas area kosong di tengah.
            int safeZoneRadius = tileSize * 3;

            // Syarat:
            // 1. Jarak cukup jauh dari player
            // 2. Tidak mepet banget sama pinggir layar (Opsional, biar rapi)
            boolean notEdge = (col > 0 && col < maxScreenCol - 1 && row > 0 && row < maxScreenRow - 1);

            if (distance > safeZoneRadius && notEdge) {
                // Cek tumpang tindih sesama pohon (biar gak numpuk di satu titik)
                boolean overlapping = false;
                for (Point p : trees) {
                    if (p.x == x && p.y == y) {
                        overlapping = true;
                        break;
                    }
                }

                if (!overlapping) {
                    trees.add(new Point(x, y));
                    treesSpawned++; // Berhasil nambah 1 pohon
                }
            }
        }
    }

    public void resetGame() {
        playerX = (screenWidth / 2) - (tileSize / 2);
        playerY = (screenHeight / 2) - (tileSize / 2);
        playerScore = 0;
        playerAmmo = 0;
        totalBulletsDodged = 0;
        shieldAmmoCounter = 0;

        bullets.clear();
        monsters.clear();
        isGameOver = false;
        isScoreSaved = false;
        isMoving = false;
        isPaused = false;
        hasShield = false;
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
        sound.stopMusic();
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        isRunning = true;
        gameThread.start();

        // NYALAKAN MUSIK (Index 0)
        sound.setMusic(0);
        sound.playMusic();
        sound.loopMusic();
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
                boolean hitMonster = false;

                if (b.isPlayerBullet) {
                    for (Monster m : monsters) {
                        // Cek tabrakan peluru dengan monster yang masih hidup
                        if (m.isActive && b.getBounds().intersects(m.getBounds())) {
                            m.isActive = false; // Monster SELALU Mati
                            playerScore += 10;

                            playSE(6);
                            // --- LOGIKA PIERCING (TEMBUS) ---
                            if (b.isPlasma) {
                                // JANGAN matikan peluru (b.isActive biarkan true)
                                // JANGAN pakai break (biar lanjut ngecek monster di belakangnya)
                                playSE(2); // Bunyi hit
                            } else {
                                // Kalau Peluru Biasa -> Hancur
                                b.isActive = false;
                                hitMonster = true;
                                break; // Satu peluru biasa cuma kena 1 monster
                            }
                        }
                    }
                }

                // Kalau peluru biasa sudah kena monster, stop (jangan cek pohon)
                if (hitMonster) continue;

                // --- BARU CEK KENA POHON ---
                // Tambahkan syarat: Cuma peluru BIASA yang hancur kena pohon.
                // Plasma TEMBUS pohon (!b.isPlasma).
                if (!b.isPlasma) {
                    if (checkBulletTreeCollision(b)) {
                        b.isActive = false;
                        continue;
                    }
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

    // --- DATABASE SAVE (HIGHSCORE SYSTEM) ---
    private void saveScoreToDB(String statusGame) {
        System.out.println("Mengecek Highscore untuk: " + username + "...");
        try {
            Connection conn = DBConnection.getConnection();

            // 1. AMBIL SKOR LAMA DULU
            String checkSql = "SELECT skor FROM tbenefit WHERE username = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, this.username);
            java.sql.ResultSet rs = checkStmt.executeQuery();

            boolean shouldUpdate = false;

            if (rs.next()) {
                int dbScore = rs.getInt("skor");
                System.out.println("Skor Lama di DB: " + dbScore + " | Skor Baru: " + this.playerScore);

                // LOGIKA HIGHSCORE: Cuma update kalau skor sekarang LEBIH BESAR
                if (this.playerScore > dbScore) {
                    shouldUpdate = true;
                } else {
                    System.out.println("Skor tidak disimpan (Belum Highscore).");
                }
            } else {
                // Kalau user entah kenapa gak ada di DB, kita anggap harus update/insert
                // (Biasanya jarang terjadi kalau loginnya bener)
                System.out.println("User tidak ditemukan, mencoba update...");
                shouldUpdate = true;
            }
            rs.close();
            checkStmt.close();

            // 2. PROSES UPDATE (Hanya jika shouldUpdate == true)
            if (shouldUpdate) {
                String updateSql = "UPDATE tbenefit SET skor=?, peluru_meleset=?, sisa_peluru=?, status=? WHERE username=?";
                PreparedStatement pstmt = conn.prepareStatement(updateSql);
                pstmt.setInt(1, this.playerScore);
                pstmt.setInt(2, this.totalBulletsDodged);
                pstmt.setInt(3, this.playerAmmo);
                pstmt.setString(4, statusGame);
                pstmt.setString(5, this.username);

                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    System.out.println("NEW HIGHSCORE SAVED!");
                }
                pstmt.close();
            }

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

    // --- COLLISION LOGIC (VERSI FINAL & FIX) ---
    private void checkCollisions() {
        Rectangle playerBodyRect = new Rectangle(playerX + 16, playerY + 16, 32, 48);

        // 1. Cek Peluru Musuh kena Player
        for (Bullet b : bullets) {
            // Skip peluru player & peluru yg sudah mati
            if (!b.isActive || b.isPlayerBullet) continue;

            if (b.getBounds().intersects(playerBodyRect)) {

                // CEK SHIELD (Anti Peluru)
                if (isShieldActive) {
                    b.isActive = false; // Peluru hancur kena shield
                    System.out.println("Shield blocks bullet!");
                    continue; // Player selamat, lanjut loop berikutnya
                }

                // Gak ada shield -> Mati
                b.isActive = false;
                isGameOver = true;
                stopMusic();
                playSE(5);
            }
        }

        // 2. Cek Monster Nabrak Player
        for (Monster m : monsters) {
            if (m.isActive && m.getBounds().intersects(playerBodyRect)) {

                // --- TAMBAHAN PENTING DI SINI ---
                // CEK SHIELD (Anti Tabrak)
                if (isShieldActive) {
                    // Kalau shield aktif, tabrakan DIABAIKAN.
                    // Player tembus monster (Invincible)
                    System.out.println("Shield blocks monster collision!");
                    continue; // Skip kode Game Over di bawah
                }

                // Gak ada shield -> Mati
                isGameOver = true;
                stopMusic();
                playSE(5);
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

        // INDIKATOR PLASMA READY
        if (isPlasmaReady) {
            // KONDISI 1: SUDAH DITEKAN 'E' (SIAP TEMBAK)
            // Efek kedap-kedip Magenta/Putih
            if (spriteCounter % 20 < 10) {
                drawOutlinedText(g2, "[CLICK] FIRE PLASMA!", 30, 120, Color.MAGENTA);
            } else {
                drawOutlinedText(g2, "[CLICK] FIRE PLASMA!", 30, 120, Color.WHITE);
            }
        } else {
            // KONDISI 2: BELUM DITEKAN 'E' (TAMPILKAN PROGRESS)
            if (playerAmmo >= 20) {
                // Kalau ammo cukup tapi belum tekan E -> Kasih warna Hijau & Info Tombol
                drawOutlinedText(g2, "Plasma: " + playerAmmo + "/20 [Press E]", 30, 120, Color.GREEN);
            } else {
                // Kalau ammo belum cukup -> Warna Abu-abu (Progress)
                drawOutlinedText(g2, "Plasma: " + playerAmmo + "/20", 30, 120, Color.GRAY);
            }
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

    // --- METHOD UNTUK MUSIK & SFX (UPDATE SESUAI SOUND.JAVA BARU) ---
    public void playMusic(int i) {
        sound.setMusic(i); // Sebelumnya setFile
        sound.playMusic(); // Sebelumnya play
        sound.loopMusic(); // Sebelumnya loop
    }

    public void stopMusic() {
        sound.stopMusic(); // Sebelumnya stop
    }

    public void playSE(int i) {
        sound.playSE(i);   // Langsung panggil playSE dari Sound.java yang baru
    }

    // --- INPUT ---
    @Override
    public void mousePressed(MouseEvent e) {
        if (isGameOver) return;

        // Hitung posisi mouse (Game Coordinates)
        double widthScale = (double) getWidth() / screenWidth;
        double heightScale = (double) getHeight() / screenHeight;
        double scale = Math.min(widthScale, heightScale);
        double xOffset = (getWidth() - (screenWidth * scale)) / 2;
        double yOffset = (getHeight() - (screenHeight * scale)) / 2;
        int gameMouseX = (int) ((e.getX() - xOffset) / scale);
        int gameMouseY = (int) ((e.getY() - yOffset) / scale);
        int pCX = playerX + tileSize / 2;
        int pCY = playerY + tileSize / 2;

        // --- LOGIKA NEMBAK BARU ---

        if (isPlasmaReady) {
            // --- KASUS A: NEMBAK PLASMA ---
            if (playerAmmo >= 20) {
                playerAmmo -= 20;

                // Spawn Plasma (Target = Posisi Klik Mouse)
                bullets.add(new Bullet(pCX, pCY, gameMouseX, gameMouseY, true, true));

                // Bunyi Plasma (Misal index 1 atau buat baru)
                playSE(7);

                // Matikan mode plasma setelah nembak (Balik ke peluru biasa)
                isPlasmaReady = false;
                System.out.println("Plasma Fired!");
            }
        } else {
            // --- KASUS B: NEMBAK BIASA ---
            if (playerAmmo > 0) {
                bullets.add(new Bullet(pCX, pCY, gameMouseX, gameMouseY, true));
                playerAmmo--;
                playSE(1);
            }
        }
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
            sound.playMusic();
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

        // 7. TOMBOL PLASMA (E) - Equip / Unequip
        if (code == KeyEvent.VK_E) {
            if (!isGameOver && !isPaused) {
                // Cek Ammo dulu
                if (playerAmmo >= 20) {
                    // Toggle: Kalau nyala jadi mati, kalau mati jadi nyala
                    isPlasmaReady = !isPlasmaReady;

                    if (isPlasmaReady) {
                        System.out.println("PLASMA EQUIPPED! Klik Mouse untuk tembak.");
                        playSE(4); // Bunyi 'Ting' (Sound Shield) sebagai tanda siap
                    } else {
                        System.out.println("Plasma Cancelled.");
                    }
                } else {
                    System.out.println("Ammo kurang! Butuh 20.");
                    isPlasmaReady = false;
                }
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