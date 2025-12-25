package com.monster.game.view;

import com.monster.game.intent.GameplayIntent;
import com.monster.game.intent.GameplayReducer;
import com.monster.game.model.GameplayState;
import com.monster.game.util.DBConnection; // Pastikan ada
import com.monster.game.util.Sound;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Random;

public class GamePanel extends JPanel {

    // --- MVI COMPONENTS ---
    private GameplayState currentState;
    private GameplayReducer reducer;
    private Timer gameLoopTimer; // Pengganti Thread

    // --- SYSTEM ---
    private GameView parentView;
    private String username;
    private Sound sound = new Sound();
    private boolean isScoreSaved = false; // Flag biar gak save berkali-kali pas Game Over

    // --- ASSETS & SCREEN ---
    final int tileSize = 64;
    final int maxScreenCol = 16;
    final int maxScreenRow = 12;
    public final int screenWidth = tileSize * maxScreenCol;
    public final int screenHeight = tileSize * maxScreenRow;

    // GAMBAR (Lengkap)
    private Image bgImage, treeImage;
    private Image run1, run2, idle1, idle2, idle3;
    private Image monster1, monster2, monster3, monster4;
    private Image bulletPlayerImg, bulletMonsterImg;
    private Image plasma1, plasma2, shield1, shield2;

    // Visual Helpers
    private int spriteCounter = 0;
    private int spriteNum = 1;
    private int currentMouseX, currentMouseY; // Cuma buat visual garis bidik (kalau mau)

    public GamePanel(String username, GameView parentView) {
        this.username = username;
        this.parentView = parentView;

        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.DARK_GRAY);
        this.setDoubleBuffered(true);
        this.setFocusable(true);

        // 1. INISIALISASI MVI
        this.reducer = new GameplayReducer();

        // Spawn Player di Tengah
        int startX = (screenWidth / 2) - (tileSize / 2);
        int startY = (screenHeight / 2) - (tileSize / 2);
        this.currentState = new GameplayState(startX, startY);

        // Generate Pohon Awal
        generateTreesForState();

        // Kirim Intent Init
        currentState = reducer.reduce(currentState, new GameplayIntent.InitGame(screenWidth, screenHeight, tileSize));

        // 2. LOAD SEMUA GAMBAR
        loadImages();

        // 3. SETUP INPUT LISTENER
        setupInputs();

        // 4. MUSIK
        sound.setMusic(0);
        sound.playMusic();
        sound.loopMusic();
    }

    private void dispatchIntent(GameplayIntent intent) {
        // 1. Update State lewat Reducer
        currentState = reducer.reduce(currentState, intent);

        // 2. Cek Sound Trigger LANGSUNG (Biar gak ilang)
        if (currentState.soundTrigger != -1) {
            sound.playSE(currentState.soundTrigger);
        }
    }

    // --- HELPER LOAD GAMBAR (LENGKAP) ---
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

    // --- LOGIKA LOAD SESSION (DARI MENU) ---
    public void loadSession(int savedScore, int savedAmmo, int savedDodged) {
        // Update State Awal
        this.currentState.score = savedScore;
        this.currentState.ammo = savedAmmo;
        this.currentState.dodged = savedDodged;
        // Logic shield counter kalau mau diload juga bisa ditambahkan di State
    }

    // --- LOGIKA GENERATE POHON (HELPER) ---
    private void generateTreesForState() {
        Random rand = new Random();
        int count = 3 + rand.nextInt(4);
        int spawned = 0;

        while(spawned < count) {
            int x = rand.nextInt(maxScreenCol) * tileSize;
            int y = rand.nextInt(maxScreenRow) * tileSize;

            // Jarak Aman dari Player (Tengah)
            double dist = Math.sqrt(Math.pow(x - currentState.playerX, 2) + Math.pow(y - currentState.playerY, 2));
            if (dist > 200) {
                currentState.trees.add(new Point(x,y));
                spawned++;
            }
        }
    }

    // --- MAIN GAME LOOP (TIMER) ---
    public void startGameThread() {
        // 60 FPS = ~16ms
        gameLoopTimer = new Timer(16, e -> {

            // A. Kalau Game Over -> Stop Update Fisika, Cek Save DB
            if (currentState.isGameOver) {
                if (!isScoreSaved) {
                    saveScoreToDB("GAMEOVER");
                    isScoreSaved = true;
                }
                repaint(); // Tetap gambar layar (Game Over Screen)
                return;
            }

            // B. Normal Loop
            // 1. UPDATE STATE VIA REDUCER
            dispatchIntent(new GameplayIntent.GameTick());

            // 2. GAMBAR & ANIMASI
            repaint();
            updateAnimation();
        });
        gameLoopTimer.start();
    }

    private void updateAnimation() {
        spriteCounter++;
        // Animasi gerak player
        boolean isMoving = currentState.upPressed || currentState.downPressed || currentState.leftPressed || currentState.rightPressed;
        if (isMoving) {
            if (spriteCounter > 10) { spriteNum = (spriteNum == 1) ? 2 : 1; spriteCounter = 0; }
        } else {
            if (spriteCounter > 20) { spriteNum = (spriteNum == 1) ? 2 : (spriteNum == 2) ? 3 : 1; spriteCounter = 0; }
        }
    }

    // --- INPUT HANDLER (MVI INTENT) ---
    private void setupInputs() {
        // KEYBOARD
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();

                // 1. TOMBOL GLOBAL (NON-REDUCER)
                if (code == KeyEvent.VK_Q) { // QUIT
                    quitToMenu();
                    return;
                }
                if (code == KeyEvent.VK_SPACE && currentState.isGameOver) { // RESTART
                    resetGame();
                    return;
                }

                // 2. KIRIM INTENT KE REDUCER
                dispatchIntent(new GameplayIntent.InputKey(e.getKeyCode(), true));
            }

            @Override
            public void keyReleased(KeyEvent e) {
                currentState = reducer.reduce(currentState, new GameplayIntent.InputKey(e.getKeyCode(), false));
            }
        });

        // MOUSE
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (currentState.isGameOver) return;
                Point p = getGameCoords(e);
                // Kirim Intent Klik
                dispatchIntent(new GameplayIntent.InputMouse(p.x, p.y, true));
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = getGameCoords(e);
                currentMouseX = p.x; currentMouseY = p.y;
                // Opsional: Kirim posisi mouse ke state kalau reducer butuh data real-time mouse
            }
            @Override public void mouseDragged(MouseEvent e) { mouseMoved(e); }
        };
        this.addMouseListener(mouseHandler);
        this.addMouseMotionListener(mouseHandler);
    }

    // --- FITUR SYSTEM (SAVE/QUIT/RESET) ---

    private void resetGame() {
        // Reset State Total
        int startX = (screenWidth / 2) - (tileSize / 2);
        int startY = (screenHeight / 2) - (tileSize / 2);
        currentState = new GameplayState(startX, startY);
        generateTreesForState();
        isScoreSaved = false;
        sound.playMusic();

        // Timer jalan terus, state aja yang direset
    }

    public void quitToMenu() {
        if (!currentState.isGameOver) {
            saveScoreToDB("ONGOING");
        }
        if (gameLoopTimer != null) gameLoopTimer.stop();
        sound.stopMusic();
        parentView.showMainMenu();
    }

    private void saveScoreToDB(String statusGame) {
        System.out.println("\n--- SAVE PROCESS (SPLIT LOGIC) ---");
        try {
            Connection conn = DBConnection.getConnection();

            // 1. CEK SKOR LAMA
            String checkSql = "SELECT skor FROM tbenefit WHERE username = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, this.username);
            java.sql.ResultSet rs = checkStmt.executeQuery();

            boolean isNewRecord = false;
            int dbScore = 0;

            if (rs.next()) {
                dbScore = rs.getInt("skor");
                // Cek apakah kita memecahkan rekor?
                if (currentState.score > dbScore) {
                    isNewRecord = true;
                }
            } else {
                // User baru, anggap rekor baru
                isNewRecord = true;
            }
            rs.close(); checkStmt.close();

            System.out.println(">> DB Score: " + dbScore + " | Current Score: " + currentState.score);

            // 2. EKSEKUSI UPDATE (TERPISAH)

            if (isNewRecord) {
                // --- JALUR A: REKOR BARU (UPDATE SEMUA) ---
                System.out.println(">> HASIL: REKOR BARU! Update Skor & Status.");

                String sql = "UPDATE tbenefit SET skor=?, peluru_meleset=?, sisa_peluru=?, status=? WHERE username=?";
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setInt(1, currentState.score); // Masukkan skor baru
                pst.setInt(2, currentState.dodged);
                pst.setInt(3, currentState.ammo);
                pst.setString(4, statusGame);
                pst.setString(5, this.username);
                pst.executeUpdate();
                pst.close();

            } else {
                // --- JALUR B: GAK MECANHIN REKOR (CUMA UPDATE STATUS) ---
                // Perhatikan: Kita TIDAK MENYEBUT kolom 'skor' di SQL ini.
                // Jadi skor lama (Highscore) 100% AMAN, gak bakal kesentuh.

                System.out.println(">> HASIL: Skor Tidak Lebih Tinggi. HANYA Update Status.");

                String sql = "UPDATE tbenefit SET status=? WHERE username=?";
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setString(1, statusGame); // Cuma ubah status jadi GAMEOVER
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

    // --- RENDER (VIEW) ---
    // --- RENDER (VIEW) ---
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Scaling Layar
        double widthScale = (double) getWidth() / screenWidth;
        double heightScale = (double) getHeight() / screenHeight;
        double scale = Math.min(widthScale, heightScale);
        double xOffset = (getWidth() - (screenWidth * scale)) / 2;
        double yOffset = (getHeight() - (screenHeight * scale)) / 2;
        g2.translate(xOffset, yOffset);
        g2.scale(scale, scale);

        // 1. Background
        if (bgImage != null) g2.drawImage(bgImage, 0, 0, screenWidth, screenHeight, null);

        // ============================================================
        // LAYER 2: MONSTER DARAT (TANAMAN) -> DI BELAKANG POHON
        // ============================================================
        for (com.monster.game.view.Monster m : currentState.monsters) {
            // TIPE 2 = TANAMAN (Pakai Gambar 3 & 4)
            if (m.type == 1) {
                Image img = (m.spriteNum == 1) ? monster3 : monster4;
                if (img != null) {
                    g2.drawImage(img, m.x, m.y, m.width, m.height, null);
                }
            }
        }

        // 3. Player & Shield (Player digambar sebelum pohon, berarti player jalan di bawah pohon)
        boolean isMoving = currentState.upPressed || currentState.downPressed || currentState.leftPressed || currentState.rightPressed;
        Image pImg = isMoving ? ((spriteNum == 1) ? run1 : run2) : ((spriteNum == 1) ? idle1 : (spriteNum == 2) ? idle2 : idle3);
        g2.drawImage(pImg, currentState.playerX, currentState.playerY, 96, 96, null);

        if (currentState.isShieldActive) {
            Image sImg = (spriteCounter % 10 < 5) ? shield1 : shield2;
            int sx = currentState.playerX + (32 - 100)/2 + 16;
            int sy = currentState.playerY + (40 - 100)/2 + 16;
            if(sImg != null) g2.drawImage(sImg, sx, sy, 100, 100, null);
        }

        // ============================================================
        // LAYER 4: POHON (NIMPUK TANAMAN & PLAYER)
        // ============================================================
        if (treeImage != null) {
            for (Point p : currentState.trees) g2.drawImage(treeImage, p.x, p.y, 256, 256, null);
        }

        // ============================================================
        // LAYER 5: MONSTER UDARA (KELELAWAR) -> DI ATAS POHON
        // ============================================================
        for (com.monster.game.view.Monster m : currentState.monsters) {
            // TIPE 1 = KELELAWAR (Pakai Gambar 1 & 2)
            if (m.type == 2) {
                Image img = (m.spriteNum == 1) ? monster1 : monster2;
                if (img != null) {
                    g2.drawImage(img, m.x, m.y, m.width, m.height, null);
                }
            }
        }

        // 6. Peluru
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
                // Gambar sesuai b.width & b.height (Plasma otomatis besar 128)
                g2.drawImage(bImg, b.x, b.y, b.width, b.height, null);
            }
        }

        // --- UI HUD ---
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, screenWidth, 60);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
        int yPos = 38;

        drawOutlinedText(g2, "PLAYER: " + username, 30, yPos, Color.WHITE);
        drawOutlinedText(g2, "SCORE: " + currentState.score, 300, yPos, Color.YELLOW);
        drawOutlinedText(g2, "AMMO: " + currentState.ammo, 520, yPos, Color.CYAN);
        drawOutlinedText(g2, "MISSED: " + currentState.dodged, 750, yPos, Color.ORANGE);

        if (currentState.hasShield) {
            drawOutlinedText(g2, "[F] SHIELD READY!", 30, 80, Color.GREEN);
        } else if (currentState.isShieldActive) {
            drawOutlinedText(g2, "SHIELD ACTIVE!", 30, 80, Color.CYAN);
        } else {
            drawOutlinedText(g2, "Shield: " + currentState.shieldAmmoCounter + "/10", 30, 80, Color.GRAY);
        }

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

        // Game Over Screen
        if (currentState.isGameOver) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, screenWidth, screenHeight);

            drawCenteredText(g2, "GAME OVER", screenHeight/2 - 50, Color.RED, 72);
            drawCenteredText(g2, "Press [SPACE] to Restart", screenHeight/2 + 20, Color.WHITE, 32);
            drawCenteredText(g2, "Press [Q] to Quit to Menu", screenHeight/2 + 60, Color.WHITE, 32);
            drawCenteredText(g2, "Final Score: " + currentState.score, screenHeight/2 + 110, Color.YELLOW, 24);
        }

        g2.dispose();
    }

    // --- TEXT HELPERS ---
    private void drawOutlinedText(Graphics2D g2, String text, int x, int y, Color c) {
        g2.setColor(Color.BLACK);
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