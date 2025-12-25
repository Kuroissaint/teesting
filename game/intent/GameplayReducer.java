package com.monster.game.intent;

import com.monster.game.model.GameplayState;
import com.monster.game.view.Bullet;
import com.monster.game.view.Monster;

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
     */
    private void updatePhysics(GameplayState s) {

        // --- A. Logika Pergerakan Player ---
        int speed = 5;
        int nextX = s.playerX;
        int nextY = s.playerY;

        if (s.upPressed) nextY -= speed;
        if (s.downPressed) nextY += speed;
        if (s.leftPressed) nextX -= speed;
        if (s.rightPressed) nextX += speed;

        // Validasi apakah posisi tujuan menabrak pohon
        if (!checkTreeCollision(s, nextX, nextY)) {
            s.playerX = nextX;
            s.playerY = nextY;
        }

        // Memastikan player tidak keluar dari batas layar
        s.playerX = Math.max(0, Math.min(screenWidth - tileSize, s.playerX));
        s.playerY = Math.max(0, Math.min(screenHeight - tileSize, s.playerY));

        // --- B. Logika Durasi Shield ---
        if (s.isShieldActive) {
            s.shieldTimer++;
            if (s.shieldTimer > 180) { // Berakhir setelah 3 detik (60fps * 3)
                s.isShieldActive = false;
                s.shieldTimer = 0;
            }
        }

        // --- C. Update Status Peluru ---
        for (Bullet b : s.bullets) {
            if (b.isActive) {
                b.update(); // Pergerakan peluru

                // Cek apakah peluru menabrak batang pohon
                boolean hitTree = false;
                for (Point p : s.trees) {
                    Rectangle treeRect = new Rectangle(p.x + 78, p.y + 150, 100, 90);

                    if (b.getBounds().intersects(treeRect)) {
                        // Peluru Plasma memiliki kemampuan menembus pohon
                        if (b.isPlasma) continue;

                        b.isActive = false;
                        hitTree = true;
                        break;
                    }
                }

                if (hitTree) continue;

                // Logika Peluru Player mengenai Monster
                if (b.isPlayerBullet) {
                    for (Monster m : s.monsters) {
                        if (m.isActive && b.getBounds().intersects(m.getBounds())) {
                            m.isActive = false;
                            s.score += 10;
                            s.soundTrigger = 6; // Bunyi monster terkena hit

                            if (!b.isPlasma) {
                                b.isActive = false;
                                break;
                            }
                        }
                    }
                }

                // Menghapus peluru yang keluar layar dan menambah stat ammo musuh yang meleset
                if (b.x < -50 || b.x > screenWidth + 50 || b.y < -50 || b.y > screenHeight + 50) {
                    b.isActive = false;
                    if (!b.isPlayerBullet) {
                        s.ammo++;
                        s.dodged++;
                        s.shieldAmmoCounter++; // Mengumpulkan poin untuk shield baru
                        if (s.shieldAmmoCounter >= 10 && !s.hasShield) {
                            s.hasShield = true;
                            s.shieldAmmoCounter = 0;
                        }
                    }
                }
            } else {
                s.bullets.remove(b); // Pembersihan memori dari peluru tidak aktif
            }
        }

        // --- D. Logika Munculnya Monster Baru ---
        s.monsterSpawnCounter++;
        if (s.monsterSpawnCounter > 100) {
            int spawnX = rand.nextBoolean() ? rand.nextInt(screenWidth/3) : (screenWidth*2/3) + rand.nextInt(screenWidth/3);
            int tipe = rand.nextInt(10) < 8 ? 1 : 2; // 80% Kelelawar, 20% Tanaman

            s.monsters.add(new Monster(spawnX, screenHeight, tipe));
            s.monsterSpawnCounter = 0;
            s.soundTrigger = (tipe == 1) ? 3 : 2;
        }

        // --- E. Update Aksi Monster (Pengejaran & Menembak) ---
        for (Monster m : s.monsters) {
            if (m.isActive) {
                m.update(s.playerX, s.playerY, s.trees);

                m.shotCounter++;
                int batas = (m.type == 1) ? 100 : 300; // Kelelawar menembak lebih sering
                if (m.shotCounter > batas) {
                    int pCX = s.playerX + tileSize/2;
                    int pCY = s.playerY + tileSize/2;
                    s.bullets.add(new Bullet(m.x+32, m.y+32, pCX, pCY, false));
                    m.shotCounter = 0;
                }
            } else {
                s.monsters.remove(m);
            }
        }

        // --- F. Verifikasi Kondisi Kekalahan (Game Over) ---
        Rectangle playerRect = new Rectangle(s.playerX + 16, s.playerY + 16, 32, 48);

        // Terkena Peluru Musuh
        for (Bullet b : s.bullets) {
            if (!b.isActive || b.isPlayerBullet) continue;
            if (b.getBounds().intersects(playerRect)) {
                if (s.isShieldActive) {
                    b.isActive = false; continue; // Selamat jika shield aktif
                }
                s.isGameOver = true;
                s.soundTrigger = 5;
            }
        }

        // Tabrakan Fisik dengan Monster
        for (Monster m : s.monsters) {
            if (m.isActive && m.getBounds().intersects(playerRect)) {
                if (s.isShieldActive) continue;
                s.isGameOver = true;
                s.soundTrigger = 5;
            }
        }
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