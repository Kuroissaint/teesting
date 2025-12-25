package com.monster.game.view;

import com.monster.game.intent.GameplayIntent;
import com.monster.game.intent.GameplayReducer;
import com.monster.game.model.GameplayState;
import com.monster.game.util.DBConnection;
import com.monster.game.util.Sound;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Random;

/**
 * File ini bertindak sebagai "View" utama dalam arsitektur MVI untuk layar permainan.
 * Tugas utamanya adalah:
 * 1. Merender (menggambar) seluruh objek permainan berdasarkan snapshot data dari GameplayState.
 * 2. Menangkap input pengguna (Keyboard & Mouse) dan mengirimkannya sebagai "Intent" ke Reducer.
 * 3. Menjalankan Game Loop menggunakan Swing Timer.
 */
public class GamePanel extends JPanel {

    // --- KOMPONEN MVI ---
    private GameplayState currentState; // Snapshot data kondisi permainan saat ini
    private GameplayReducer reducer;    // Pengolah logika untuk mengubah state
    private Timer gameLoopTimer;        // Pengatur detak jantung permainan (FPS)

    // --- SISTEM ---
    private GameView parentView;        // Referensi ke frame utama untuk navigasi antar layar
    private String username;            // Nama pemain yang sedang aktif
    private Sound sound = new Sound();  // Pemutar musik dan efek suara
    private boolean isScoreSaved = false; // Mencegah duplikasi penyimpanan data saat mati

    // --- ASET & UKURAN LAYAR ---
    final int tileSize = 64;            // Ukuran dasar satu kotak (tile)
    final int maxScreenCol = 16;
    final int maxScreenRow = 12;
    public final int screenWidth = tileSize * maxScreenCol;
    public final int screenHeight = tileSize * maxScreenRow;

    // Aset Gambar
    private Image bgImage, treeImage;
    private Image run1, run2, idle1, idle2, idle3;
    private Image monster1, monster2, monster3, monster4;
    private Image bulletPlayerImg, bulletMonsterImg;
    private Image plasma1, plasma2, shield1, shield2;

    // Pembantu Visual
    private int spriteCounter = 0;      // Penghitung frame untuk animasi
    private int spriteNum = 1;          // Indeks frame sprite yang aktif
    private int currentMouseX, currentMouseY;

    /**
     * Konstruktor: Menyiapkan panel, inisialisasi state awal, dan memuat aset.
     */
    public GamePanel(String username, GameView parentView) {
        this.username = username;
        this.parentView = parentView;

        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.DARK_GRAY);
        this.setDoubleBuffered(true);
        this.setFocusable(true);

        // 1. INISIALISASI MVI
        this.reducer = new GameplayReducer();

        // Tentukan posisi awal player di tengah layar
        int startX = (screenWidth / 2) - (tileSize / 2);
        int startY = (screenHeight / 2) - (tileSize / 2);
        this.currentState = new GameplayState(startX, startY);

        // Siapkan rintangan pohon secara acak
        generateTreesForState();

        // Beri tahu Reducer untuk melakukan inisialisasi konfigurasi layar
        currentState = reducer.reduce(currentState, new GameplayIntent.InitGame(screenWidth, screenHeight, tileSize));

        // 2. MEMUAT SEMUA ASSET GAMBAR
        loadImages();

        // 3. MENYIAPKAN PENANGKAP INPUT (KEYBOARD & MOUSE)
        setupInputs();

        // 4. MEMULAI MUSIK LATAR
        sound.setMusic(0);
        sound.playMusic();
        sound.loopMusic();
    }

    /**
     * Mengirim pesan aksi (Intent) ke Reducer dan memutar suara jika ada perintah dari State.
     */
    private void dispatchIntent(GameplayIntent intent) {
        // Update State melalui pengolahan logika di Reducer
        currentState = reducer.reduce(currentState, intent);

        // Cek apakah ada efek suara yang perlu diputar segera
        if (currentState.soundTrigger != -1) {
            sound.playSE(currentState.soundTrigger);
        }
    }

    /**
     * Memuat file gambar dari folder assets ke dalam objek Image.
     */
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

    /**
     * Memuat data statistik sesi permainan dari menu utama (Resume Game).
     */
    public void loadSession(int savedScore, int savedAmmo, int savedDodged) {
        this.currentState.score = savedScore;
        this.currentState.ammo = savedAmmo;
        this.currentState.dodged = savedDodged;
    }

    /**
     * Menghasilkan posisi rintangan pohon secara acak di area yang aman dari posisi player.
     */
    private void generateTreesForState() {
        Random rand = new Random();
        int count = 3 + rand.nextInt(4);
        int spawned = 0;

        while(spawned < count) {
            int x = rand.nextInt(maxScreenCol) * tileSize;
            int y = rand.nextInt(maxScreenRow) * tileSize;

            double dist = Math.sqrt(Math.pow(x - currentState.playerX, 2) + Math.pow(y - currentState.playerY, 2));
            if (dist > 200) {
                currentState.trees.add(new Point(x,y));
                spawned++;
            }
        }
    }

    /**
     * Menjalankan detak permainan (Game Loop) dengan target 60 Frame Per Detik.
     */
    public void startGameThread() {
        gameLoopTimer = new Timer(16, e -> {

            // A. Penanganan Kondisi Game Over
            if (currentState.isGameOver) {
                if (!isScoreSaved) {
                    saveScoreToDB("GAMEOVER"); // Simpan statistik terakhir ke DB
                    isScoreSaved = true;
                }
                repaint();
                return;
            }

            // B. Alur Permainan Normal
            // 1. Kirim sinyal GameTick ke Reducer untuk update posisi dan tabrakan
            dispatchIntent(new GameplayIntent.GameTick());

            // 2. Gambar ulang layar dan update urutan animasi sprite
            repaint();
            updateAnimation();
        });
        gameLoopTimer.start();
    }

    /**
     * Mengatur pergantian frame sprite untuk menciptakan efek pergerakan animasi.
     */
    private void updateAnimation() {
        spriteCounter++;
        boolean isMoving = currentState.upPressed || currentState.downPressed || currentState.leftPressed || currentState.rightPressed;
        if (isMoving) {
            // Animasi lebih cepat saat bergerak
            if (spriteCounter > 10) { spriteNum = (spriteNum == 1) ? 2 : 1; spriteCounter = 0; }
        } else {
            // Animasi lebih lambat saat diam (Idle)
            if (spriteCounter > 20) { spriteNum = (spriteNum == 1) ? 2 : (spriteNum == 2) ? 3 : 1; spriteCounter = 0; }
        }
    }

    /**
     * Mendefinisikan aksi saat tombol keyboard atau mouse ditekan.
     */
    private void setupInputs() {
        // PENANGANAN KEYBOARD
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();

                // 1. Logika Pause (P)
                if (code == KeyEvent.VK_P) {
                    dispatchIntent(new GameplayIntent.TogglePause());
                    return;
                }

                // 2. Logika Restart (R) - Hanya saat Game Over atau Pause
                if (code == KeyEvent.VK_R) {
                    if (currentState.isGameOver || currentState.isPaused) {
                        resetGame();
                        return;
                    }
                }

                // 3. Logika Kembali ke Menu (Space) - Hanya saat Game Over atau Pause
                if (code == KeyEvent.VK_SPACE) {
                    if (currentState.isGameOver || currentState.isPaused) {
                        quitToMenu();
                        return;
                    }
                }

                // Jalankan input pergerakan/aksi hanya jika game TIDAK dipause
                if (!currentState.isPaused) {
                    dispatchIntent(new GameplayIntent.InputKey(code, true));
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                currentState = reducer.reduce(currentState, new GameplayIntent.InputKey(e.getKeyCode(), false));
            }
        });

        // PENANGANAN MOUSE
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (currentState.isGameOver) return;
                Point p = getGameCoords(e);
                // Kirim Intent tembak berdasarkan koordinat klik mouse
                dispatchIntent(new GameplayIntent.InputMouse(p.x, p.y, true));
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = getGameCoords(e);
                currentMouseX = p.x; currentMouseY = p.y;
            }
            @Override public void mouseDragged(MouseEvent e) { mouseMoved(e); }
        };
        this.addMouseListener(mouseHandler);
        this.addMouseMotionListener(mouseHandler);
    }

    /**
     * Mereset seluruh kondisi state permainan untuk memulai dari awal.
     */
    private void resetGame() {
        int startX = (screenWidth / 2) - (tileSize / 2);
        int startY = (screenHeight / 2) - (tileSize / 2);
        currentState = new GameplayState(startX, startY);
        generateTreesForState();
        isScoreSaved = false;
        sound.playMusic();
    }

    /**
     * Menghentikan game dan kembali ke tampilan menu utama.
     */
    public void quitToMenu() {
        if (!currentState.isGameOver) {
            saveScoreToDB("ONGOING"); // Simpan status agar bisa dilanjutkan nanti
        }
        if (gameLoopTimer != null) gameLoopTimer.stop();
        sound.stopMusic();
        parentView.showMainMenu();
    }

    /**
     * Mengelola penyimpanan skor dan status ke database MySQL.
     * Menggunakan logika Split Query agar Highscore lama tidak tertimpa skor yang lebih rendah.
     */
    private void saveScoreToDB(String statusGame) {
        System.out.println("\n--- SAVE PROCESS (SPLIT LOGIC) ---");
        try {
            Connection conn = DBConnection.getConnection();

            // 1. Ambil skor lama dari database untuk dibandingkan
            String checkSql = "SELECT skor FROM tbenefit WHERE username = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, this.username);
            java.sql.ResultSet rs = checkStmt.executeQuery();

            boolean isNewRecord = false;
            int dbScore = 0;

            if (rs.next()) {
                dbScore = rs.getInt("skor");
                if (currentState.score > dbScore) {
                    isNewRecord = true;
                }
            } else {
                isNewRecord = true; // User baru
            }
            rs.close(); checkStmt.close();

            System.out.println(">> DB Score: " + dbScore + " | Current Score: " + currentState.score);

            // 2. EKSEKUSI UPDATE BERDASARKAN HASIL BANDING
            if (isNewRecord) {
                // Jalur A: Rekor baru pecah, update semua statistik
                System.out.println(">> HASIL: REKOR BARU! Update Skor & Status.");

                String sql = "UPDATE tbenefit SET skor=?, peluru_meleset=?, sisa_peluru=?, status=? WHERE username=?";
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setInt(1, currentState.score);
                pst.setInt(2, currentState.dodged);
                pst.setInt(3, currentState.ammo);
                pst.setString(4, statusGame);
                pst.setString(5, this.username);
                pst.executeUpdate();
                pst.close();

            } else {
                // Jalur B: Skor baru tidak lebih tinggi, hanya perbarui status game saja
                System.out.println(">> HASIL: Skor Tidak Lebih Tinggi. HANYA Update Status.");

                String sql = "UPDATE tbenefit SET status=? WHERE username=?";
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setString(1, statusGame);
                pst.setString(2, this.username);
                pst.executeUpdate();
                pst.close();
            }

            conn.close();
            System.out.println(">> SAVE SUKSES.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Konversi koordinat layar ke koordinat game untuk mendukung penskalaan jendela (scaling).
     */
    private Point getGameCoords(MouseEvent e) {
        double widthScale = (double) getWidth() / screenWidth;
        double heightScale = (double) getHeight() / screenHeight;
        double scale = Math.min(widthScale, heightScale);
        double xOffset = (getWidth() - (screenWidth * scale)) / 2;
        double yOffset = (getHeight() - (screenHeight * scale)) / 2;
        int x = (int) ((e.getX() - xOffset) / scale);
        int y = (int) ((e.getY() - yOffset) / scale);
        return new Point(x, y);
    }

    /**
     * Jantung dari sistem rendering; menggambar seluruh visual berdasarkan State.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // --- SISTEM PENSKALAAN OTOMATIS (Aspect Ratio) ---
        double widthScale = (double) getWidth() / screenWidth;
        double heightScale = (double) getHeight() / screenHeight;
        double scale = Math.min(widthScale, heightScale);
        double xOffset = (getWidth() - (screenWidth * scale)) / 2;
        double yOffset = (getHeight() - (screenHeight * scale)) / 2;
        g2.translate(xOffset, yOffset);
        g2.scale(scale, scale);

        // 1. Gambar Latar Belakang (Background)
        if (bgImage != null) g2.drawImage(bgImage, 0, 0, screenWidth, screenHeight, null);

        // 2. LAYER MONSTER DARAT (TANAMAN) -> Berada di belakang pohon
        for (com.monster.game.view.Monster m : currentState.monsters) {
            if (m.type == 1) { // Tipe Tanaman
                Image img = (m.spriteNum == 1) ? monster3 : monster4;
                if (img != null) {
                    g2.drawImage(img, m.x, m.y, m.width, m.height, null);
                }
            }
        }

        // 3. LAYER PLAYER & PERISAI (SHIELD)
        boolean isMoving = currentState.upPressed || currentState.downPressed || currentState.leftPressed || currentState.rightPressed;
        Image pImg = isMoving ? ((spriteNum == 1) ? run1 : run2) : ((spriteNum == 1) ? idle1 : (spriteNum == 2) ? idle2 : idle3);
        g2.drawImage(pImg, currentState.playerX, currentState.playerY, 96, 96, null);

        if (currentState.isShieldActive) {
            Image sImg = (spriteCounter % 10 < 5) ? shield1 : shield2;
            int sx = currentState.playerX - 2;
            int sy = currentState.playerY - 2;
            if(sImg != null) g2.drawImage(sImg, sx, sy, 100, 100, null);
        }

        // 4. LAYER POHON (Menutupi objek darat di bawahnya)
        if (treeImage != null) {
            for (Point p : currentState.trees) g2.drawImage(treeImage, p.x, p.y, 256, 256, null);
        }

        // 5. LAYER MONSTER UDARA (KELELAWAR) -> Berada di atas pohon
        for (com.monster.game.view.Monster m : currentState.monsters) {
            if (m.type == 2) { // Tipe Kelelawar
                Image img = (m.spriteNum == 1) ? monster1 : monster2;
                if (img != null) {
                    g2.drawImage(img, m.x, m.y, m.width, m.height, null);
                }
            }
        }

        // 6. LAYER PELURU (PROJECTILE)
        for (com.monster.game.view.Bullet b : currentState.bullets) {
            Image bImg;
            if (b.isPlayerBullet) {
                if (b.isPlasma) {
                    bImg = (b.spriteNum == 1) ? plasma1 : plasma2;
                } else {
                    bImg = bulletPlayerImg;
                }
            } else {
                bImg = bulletMonsterImg;
            }

            if (bImg != null) {
                g2.drawImage(bImg, b.x, b.y, b.width, b.height, null);
            }
        }

        // --- LAYER UI / HUD (Tampilan Layar Atas) ---
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, screenWidth, 60);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
        int yPos = 38;

        drawOutlinedText(g2, "PLAYER: " + username, 30, yPos, Color.WHITE);
        drawOutlinedText(g2, "SCORE: " + currentState.score, 300, yPos, Color.YELLOW);
        drawOutlinedText(g2, "AMMO: " + currentState.ammo, 520, yPos, Color.CYAN);
        drawOutlinedText(g2, "MISSED: " + currentState.dodged, 750, yPos, Color.ORANGE);

        // Status Perisai
        if (currentState.hasShield) {
            drawOutlinedText(g2, "[F] SHIELD READY!", 30, 80, Color.GREEN);
        } else if (currentState.isShieldActive) {
            drawOutlinedText(g2, "SHIELD ACTIVE!", 30, 80, Color.CYAN);
        } else {
            drawOutlinedText(g2, "Shield: " + currentState.shieldAmmoCounter + "/10", 30, 80, Color.GRAY);
        }

        // Status Senjata Plasma
        if (currentState.isPlasmaReady) {
            if (spriteCounter % 20 < 10)
                drawOutlinedText(g2, "[CLICK] FIRE PLASMA!", 30, 120, Color.MAGENTA);
            else
                drawOutlinedText(g2, "[CLICK] FIRE PLASMA!", 30, 120, Color.WHITE);
        } else {
            if (currentState.ammo >= 20)
                drawOutlinedText(g2, "Plasma: " + currentState.ammo + "/20 [Press E]", 30, 120, Color.GREEN);
            else
                drawOutlinedText(g2, "Plasma: " + currentState.ammo + "/20", 30, 120, Color.GRAY);
        }

        // Overlay Menu Pause
        if (currentState.isPaused && !currentState.isGameOver) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, screenWidth, screenHeight);
            drawCenteredText(g2, "PAUSED", screenHeight/2 - 40, Color.WHITE, 60);
            drawCenteredText(g2, "[P] Resume | [R] Restart | [SPACE] Menu", screenHeight/2 + 20, Color.YELLOW, 24);
        }

        // Overlay Layar Game Over
        if (currentState.isGameOver) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, screenWidth, screenHeight);

            drawCenteredText(g2, "GAME OVER", screenHeight/2 - 50, Color.RED, 72);
            drawCenteredText(g2, "Press [R] to Restart", screenHeight/2 + 20, Color.WHITE, 32);
            drawCenteredText(g2, "Press [SPACE] to Quit to Menu", screenHeight/2 + 60, Color.WHITE, 32);
            drawCenteredText(g2, "Final Score: " + currentState.score, screenHeight/2 + 110, Color.YELLOW, 24);
        }

        g2.dispose();
    }

    // --- PEMBANTU PENGGAMBARAN TEKS ---

    private void drawOutlinedText(Graphics2D g2, String text, int x, int y, Color c) {
        g2.setColor(Color.BLACK); // Outline hitam
        g2.drawString(text, x-2, y); g2.drawString(text, x+2, y);
        g2.drawString(text, x, y-2); g2.drawString(text, x, y+2);
        g2.setColor(c);
        g2.drawString(text, x, y);
    }

    private void drawCenteredText(Graphics2D g2, String text, int y, Color c, int size) {
        g2.setColor(c);
        g2.setFont(new Font("Arial", Font.BOLD, size));
        int len = (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        g2.drawString(text, (screenWidth - len) / 2, y);
    }
}