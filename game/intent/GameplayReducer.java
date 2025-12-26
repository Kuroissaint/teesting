package com.monster.game.intent;

import com.monster.game.model.GameplayState;
import com.monster.game.model.Bullet;
import com.monster.game.model.Monster;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Random;

/**
 * File ini bertindak sebagai "Otak" atau pusat logika permainan dalam arsitektur MVI.
 * Tugas utamanya adalah menerima State lama dan sebuah Intent, lalu memprosesnya
 * untuk menghasilkan State baru yang telah diperbarui.
 * Reducer memastikan semua aturan permainan (fisika, skor, tabrakan) berjalan secara terpusat.
 */
public class GameplayReducer {

    private int screenWidth, screenHeight, tileSize;
    private Random rand = new Random();

    /**
     * Method utama yang memfilter dan mengarahkan setiap Intent ke fungsi pemrosesan yang sesuai.
     */
    public GameplayState reduce(GameplayState oldState, GameplayIntent intent) {
        // Menggunakan referensi state lama untuk diperbarui (Pragmatic MVI)
        GameplayState state = oldState;
        state.soundTrigger = -1; // Reset sinyal suara di setiap frame agar tidak berulang

        // 1. Inisialisasi data layar dan game awal
        if (intent instanceof GameplayIntent.InitGame) {
            GameplayIntent.InitGame init = (GameplayIntent.InitGame) intent;
            this.screenWidth = init.screenW;
            this.screenHeight = init.screenH;
            this.tileSize = init.tileSize;
            return state;
        }

        // Jika game sedang berhenti (Pause/Game Over), hentikan pemrosesan logika fisika
        if (state.isGameOver || state.isPaused) {
            // Khusus intent Pause tetap diproses agar bisa unpause
            if (intent instanceof GameplayIntent.TogglePause && !state.isGameOver) {
                state.isPaused = !state.isPaused;
            }
            return state;
        }

        // 2. Memproses input Keyboard
        if (intent instanceof GameplayIntent.InputKey) {
            handleKeyInput(state, (GameplayIntent.InputKey) intent);
            return state;
        }

        // 3. Memproses input Mouse (Menembak)
        if (intent instanceof GameplayIntent.InputMouse) {
            handleMouseInput(state, (GameplayIntent.InputMouse) intent);
            return state;
        }

        // 4. Memproses detak jantung game (Fisika, Monster, Tabrakan)
        if (intent instanceof GameplayIntent.GameTick) {
            updatePhysics(state);
            return state;
        }

        // 5. Menangani perpindahan status Pause
        if (intent instanceof GameplayIntent.TogglePause) {
            if (!state.isGameOver) {
                state.isPaused = !state.isPaused;
            }
        }

        return state;
    }

    /**
     * Mengelola seluruh logika pergerakan dan interaksi objek di dalam game.
     * Dalam arsitektur MVI murni, semua kalkulasi fisika dilakukan terpusat di sini.
     */
    private void updatePhysics(GameplayState s) {

        // --- 1. LOGIKA PERGERAKAN PLAYER ---
        int speed = 5;
        int nextX = s.playerX;
        int nextY = s.playerY;

        // Menentukan posisi tujuan berdasarkan input tombol panah (Arrow Keys) [cite: 1, 2]
        if (s.upPressed) nextY -= speed;
        if (s.downPressed) nextY += speed;
        if (s.leftPressed) nextX -= speed;
        if (s.rightPressed) nextX += speed;

        // Cek tabrakan dengan batang pohon sebelum memindahkan posisi [cite: 1]
        if (!checkTreeCollision(s, nextX, nextY)) {
            s.playerX = nextX;
            s.playerY = nextY;
        }

        // Memastikan player tidak keluar dari batas layar [cite: 1]
        s.playerX = Math.max(0, Math.min(screenWidth - tileSize, s.playerX));
        s.playerY = Math.max(0, Math.min(screenHeight - tileSize, s.playerY));

        // --- 2. LOGIKA DURASI SHIELD ---
        if (s.isShieldActive) {
            s.shieldTimer++;
            if (s.shieldTimer > 180) { // Shield mati setelah 3 detik (60 FPS * 3) [cite: 1]
                s.isShieldActive = false;
                s.shieldTimer = 0;
            }
        }

        // --- 3. UPDATE PERGERAKAN & TABRAKAN PELURU ---
        for (Bullet b : s.bullets) {
            if (b.isActive) {
                // Pergerakan peluru berdasarkan kecepatan yang dikalkulasi di model [cite: 3]
                b.x += b.velX;
                b.y += b.velY;

                // Logika Animasi Plasma (jika peluru adalah tipe plasma)
                if (b.isPlasma) {
                    b.spriteCounter++;
                    if (b.spriteCounter > 5) {
                        b.spriteNum = (b.spriteNum == 1) ? 2 : 1;
                        b.spriteCounter = 0;
                    }
                }

                // A. Cek Tabrakan Peluru dengan Pohon [cite: 1]
                boolean hitTree = false;
                for (Point p : s.trees) {
                    // Hitbox batang pohon (disesuaikan dengan ukuran gambar tree.png) [cite: 1]
                    Rectangle treeRect = new Rectangle(p.x + 98, p.y + 196, 60, 40);
                    if (b.getBounds().intersects(treeRect)) {
                        if (!b.isPlasma) { // Peluru biasa hancur, Plasma menembus [cite: 1]
                            b.isActive = false;
                            hitTree = true;
                            break;
                        }
                    }
                }
                if (hitTree) continue;

                // B. Cek Peluru Player mengenai Monster [cite: 1]
                if (b.isPlayerBullet) {
                    for (Monster m : s.monsters) {
                        if (m.isActive && b.getBounds().intersects(m.getBounds())) {
                            m.isActive = false;
                            s.score += 10; // Skor bertambah setiap monster kalah [cite: 1]
                            s.soundTrigger = 6; // Sinyal bunyi monster mati [cite: 1]

                            if (!b.isPlasma) {
                                b.isActive = false;
                                break;
                            }
                        }
                    }
                }

                // C. Cek Peluru Keluar Layar (Scavenger Ammo) [cite: 1, 2]
                if (b.x < -150 || b.x > screenWidth + 150 || b.y < -150 || b.y > screenHeight + 150) {
                    b.isActive = false;
                    if (!b.isPlayerBullet) {
                        s.ammo++; // Peluru alien meleset menambah amunisi pemain [cite: 1, 2]
                        s.dodged++; // Statistik peluru meleset bertambah [cite: 1, 2]

                        // Logika pengumpulan shield baru [cite: 1]
                        s.shieldAmmoCounter++;
                        if (s.shieldAmmoCounter >= 10 && !s.hasShield) {
                            s.hasShield = true;
                            s.shieldAmmoCounter = 0;
                        }
                    }
                }
            } else {
                s.bullets.remove(b);
            }
        }

        // --- 4. UPDATE PERGERAKAN & AKSI MONSTER ---
        s.monsterSpawnCounter++;
        if (s.monsterSpawnCounter > 100) {
            // Spawn di bawah layar (sesuai PDF hal 7)
            int spawnX = rand.nextInt(screenWidth - 100);
            int tipe = (rand.nextInt(10) < 8) ? 1 : 2; // 20% Kelelawar, 80% Tanaman

            s.monsters.add(new Monster(spawnX, screenHeight, tipe));
            s.monsterSpawnCounter = 0;
            s.soundTrigger = (tipe == 1) ? 3 : 2;
        }

        for (Monster m : s.monsters) {
            if (m.isActive) {
                double angle = Math.atan2((s.playerY + 32) - m.y, (s.playerX + 32) - m.x);
                double monsterSpeed = (m.type == 2) ? 3.2 : 0.7; // Tipe 2: Bat, Tipe 1: Plant

                double nextMX = m.x + Math.cos(angle) * monsterSpeed;
                double nextMY = m.y + Math.sin(angle) * monsterSpeed;

                // --- PEMISAHAN LOGIKA DISINI ---
                if (m.type == 2) {
                    // KELELAWAR: Langsung pindah koordinat (Abaikan pohon / Terbang)
                    m.x = nextMX;
                    m.y = nextMY;
                }
                else {
                    // TANAMAN: Jalankan pengecekan tabrakan (Darat)
                    if (!isMonsterColliding(s, nextMX, nextMY, m)) {
                        // Jalur bersih
                        m.x = nextMX;
                        m.y = nextMY;
                    } else {
                        // Jalur mentok pohon, jalankan sistem "Geser" (Sliding)
                        if (!isMonsterColliding(s, nextMX, m.y, m)) {
                            m.x = nextMX; // Geser horizontal
                        } else if (!isMonsterColliding(s, m.x, nextMY, m)) {
                            m.y = nextMY; // Geser vertikal
                        }
                    }
                }

                // --- Logika animasi sprite tetap ---
                m.spriteCounter++;
                if (m.spriteCounter > 12) {
                    m.spriteNum = (m.spriteNum == 1) ? 2 : 1;
                    m.spriteCounter = 0;
                }

                // --- Logika menembak tetap ---
                m.shotCounter++;
                int batas = (m.type == 1) ? 100 : 250;
                if (m.shotCounter > batas) {
                    int pCX = s.playerX + tileSize/2;
                    int pCY = s.playerY + tileSize/2;
                    s.bullets.add(new Bullet((int)m.x + 32, (int)m.y + 32, pCX, pCY, false));
                    m.shotCounter = 0;
                }
            } else {
                s.monsters.remove(m);
            }
        }

        // --- 5. VERIFIKASI KONDISI GAME OVER ---
        Rectangle playerRect = new Rectangle(s.playerX + 24, s.playerY + 24, 48, 48); // Hitbox pemain [cite: 1]

        // Terkena Peluru Musuh [cite: 1]
        for (Bullet b : s.bullets) {
            if (!b.isActive || b.isPlayerBullet) continue;
            if (b.getBounds().intersects(playerRect)) {
                if (s.isShieldActive) { // Player selamat jika shield aktif [cite: 1]
                    b.isActive = false;
                    continue;
                }
                s.isGameOver = true;
                s.soundTrigger = 5; // Bunyi Game Over [cite: 1]
            }
        }

        // Tabrakan Fisik dengan Monster [cite: 1]
        for (Monster m : s.monsters) {
            if (m.isActive && m.getBounds().intersects(playerRect)) {
                if (s.isShieldActive) continue;
                s.isGameOver = true;
                s.soundTrigger = 5;
            }
        }
    }

    /**
     * Mengecek apakah posisi monster tertentu bersinggungan dengan pohon manapun.
     */
    private boolean isMonsterColliding(GameplayState s, double nx, double ny, Monster m) {
        // Buat hitbox untuk posisi yang akan dicek
        // Padding 15 agar hitbox sedikit lebih kecil dari gambar (lebih luwes)
        Rectangle monsterRect = new Rectangle((int)nx + 15, (int)ny + 15, m.width - 30, m.height - 30);

        for (Point p : s.trees) {
            // Hitbox batang pohon (disesuaikan dengan aset tree.png)
            Rectangle treeHitbox = new Rectangle(p.x + 98, p.y + 196, 60, 40);

            if (monsterRect.intersects(treeHitbox)) {
                return true; // Ada tabrakan
            }
        }
        return false; // Jalur aman
    }

    /**
     * Mengubah status arah atau aksi di State berdasarkan tombol yang ditekan.
     */
    private void handleKeyInput(GameplayState s, GameplayIntent.InputKey k) {
        int code = k.keyCode;
        boolean pressed = k.isPressed;

        if (code == KeyEvent.VK_W) s.upPressed = pressed;
        if (code == KeyEvent.VK_S) s.downPressed = pressed;
        if (code == KeyEvent.VK_A) s.leftPressed = pressed;
        if (code == KeyEvent.VK_D) s.rightPressed = pressed;

        if (pressed) {
            // Mengaktifkan perisai (Shield)
            if (code == KeyEvent.VK_F) {
                if (s.hasShield && !s.isShieldActive) {
                    s.hasShield = false;
                    s.isShieldActive = true;
                    s.shieldTimer = 0;
                    s.soundTrigger = 4;
                }
            }
            // Menyiapkan mode tembakan Plasma
            if (code == KeyEvent.VK_E) {
                if (s.ammo >= 20) {
                    s.isPlasmaReady = !s.isPlasmaReady;
                    s.soundTrigger = 4;
                }
            }
        }
    }

    /**
     * Menangani aksi penembakan peluru berdasarkan posisi klik mouse.
     */
    private void handleMouseInput(GameplayState s, GameplayIntent.InputMouse m) {
        if (m.isClick) {
            int pCX = s.playerX + tileSize/2;
            int pCY = s.playerY + tileSize/2;

            // Tembakan Plasma (Boros peluru tapi area luas)
            if (s.isPlasmaReady && s.ammo >= 20) {
                s.ammo -= 20;
                s.bullets.add(new Bullet(pCX, pCY, m.mouseX, m.mouseY, true, true));
                s.isPlasmaReady = false;
                s.soundTrigger = 7;
            }
            // Tembakan Biasa
            else if (s.ammo > 0) {
                s.ammo--;
                s.bullets.add(new Bullet(pCX, pCY, m.mouseX, m.mouseY, true));
                s.soundTrigger = 1;
            }
        }
    }

    /**
     * Memeriksa apakah koordinat tertentu bersinggungan dengan area batang pohon.
     */
    private boolean checkTreeCollision(GameplayState s, int x, int y) {
        Rectangle playerRect = new Rectangle(x + 16, y + 32, 32, 32);
        for (Point p : s.trees) {
            Rectangle treeRect = new Rectangle(p.x + 98, p.y + 196, 60, 40);
            if (playerRect.intersects(treeRect)) return true;
        }
        return false;
    }
}