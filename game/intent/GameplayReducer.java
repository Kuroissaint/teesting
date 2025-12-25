package com.monster.game.intent;

import com.monster.game.model.GameplayState;
import com.monster.game.view.Bullet;
import com.monster.game.view.Monster;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Random;

public class GameplayReducer {

    private int screenWidth, screenHeight, tileSize;
    private Random rand = new Random();

    public GameplayState reduce(GameplayState oldState, GameplayIntent intent) {
        // Kita pakai referensi state lama biar performa terjaga (Pragmatic MVI)
        GameplayState state = oldState;
        state.soundTrigger = -1; // Reset sound trigger tiap frame

        if (intent instanceof GameplayIntent.InitGame) {
            GameplayIntent.InitGame init = (GameplayIntent.InitGame) intent;
            this.screenWidth = init.screenW;
            this.screenHeight = init.screenH;
            this.tileSize = init.tileSize;
            // Generate Trees logic bisa ditaruh sini atau di helper
            return state;
        }

        if (state.isGameOver) return state; // Kalau mati, gak usah update fisika

        // --- HANDLE INPUT ---
        if (intent instanceof GameplayIntent.InputKey) {
            handleKeyInput(state, (GameplayIntent.InputKey) intent);
            return state;
        }

        if (intent instanceof GameplayIntent.InputMouse) {
            handleMouseInput(state, (GameplayIntent.InputMouse) intent);
            return state;
        }

        // --- HANDLE GAME LOOP (PHYSICS) ---
        if (intent instanceof GameplayIntent.GameTick) {
            updatePhysics(state);
            return state;
        }

        return state;
    }

    // --- LOGIKA UPDATE FISIKA (PINDAHAN DARI GAMEPANEL) ---
    private void updatePhysics(GameplayState s) {

        // 1. Update Posisi Player
        int speed = 5;
        int nextX = s.playerX;
        int nextY = s.playerY;

        if (s.upPressed) nextY -= speed;
        if (s.downPressed) nextY += speed;
        if (s.leftPressed) nextX -= speed;
        if (s.rightPressed) nextX += speed;

        // Cek Tabrak Pohon (Sederhana)
        if (!checkTreeCollision(s, nextX, nextY)) {
            s.playerX = nextX;
            s.playerY = nextY;
        }
        // Clamp Screen
        s.playerX = Math.max(0, Math.min(screenWidth - tileSize, s.playerX));
        s.playerY = Math.max(0, Math.min(screenHeight - tileSize, s.playerY));

        // 2. Update Shield Timer
        if (s.isShieldActive) {
            s.shieldTimer++;
            if (s.shieldTimer > 180) { // 3 Detik
                s.isShieldActive = false;
                s.shieldTimer = 0;
            }
        }

        // 3. Update Peluru
        for (Bullet b : s.bullets) {
            if (b.isActive) {
                b.update();

                // --- [BARU] CEK TABRAKAN DENGAN POHON ---
                boolean hitTree = false; // Flag penanda
                for (Point p : s.trees) {
                    // Hitbox Batang Pohon (Angka ini harus sama dengan logic tabrakan Player)
                    // x+98, y+196, w60, h40 adalah area batang bawah pohon
                    Rectangle treeRect = new Rectangle(p.x + 78, p.y + 150, 100, 90);

                    if (b.getBounds().intersects(treeRect)) {
                        if (b.isPlasma) {
                            continue; // Lanjut aja, abaikan pohon ini (Gak rusak, gak ilang)
                        }
                        b.isActive = false; // Peluru Hancur
                        hitTree = true;     // Tandai kena pohon
                        // s.soundTrigger = 8; // (Opsional) Kalau punya SFX 'Tuk' kena kayu
                        break; // Stop loop pohon (hemat kinerja)
                    }
                }

                // Kalau sudah kena pohon, skip logic kena monster (biar gak double kill)
                if (hitTree) continue;
                // ----------------------------------------

                // Cek Tabrakan Peluru Player vs Monster
                if (b.isPlayerBullet) {
                    for (Monster m : s.monsters) {
                        if (m.isActive && b.getBounds().intersects(m.getBounds())) {
                            m.isActive = false;
                            s.score += 10;
                            s.soundTrigger = 6; // SFX HIT

                            if (!b.isPlasma) {
                                b.isActive = false;
                                break;
                            }
                        }
                    }
                }

                // Cek Keluar Layar (Logic lama tetep dipake)
                if (b.x < -50 || b.x > screenWidth + 50 || b.y < -50 || b.y > screenHeight + 50) {
                    b.isActive = false;
                    if (!b.isPlayerBullet) {
                        s.ammo++;
                        s.dodged++;
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

        // 4. Spawn Monster
        s.monsterSpawnCounter++;
        if (s.monsterSpawnCounter > 100) {
            int spawnX = rand.nextBoolean() ? rand.nextInt(screenWidth/3) : (screenWidth*2/3) + rand.nextInt(screenWidth/3);
            int tipe = rand.nextInt(10) < 8 ? 1 : 2; // 1=Kelelawar, 2=Tanaman

            s.monsters.add(new Monster(spawnX, screenHeight, tipe));
            s.monsterSpawnCounter = 0;
            s.soundTrigger = (tipe == 1) ? 3 : 2; // Trigger SFX Spawn
        }

        // 5. Update Monster (Gerak & Nembak)
        for (Monster m : s.monsters) {
            if (m.isActive) {
                m.update(s.playerX, s.playerY, s.trees); // Monster butuh info player & pohon

                m.shotCounter++;
                int batas = (m.type == 1) ? 100 : 300;
                if (m.shotCounter > batas) {
                    // Monster Nembak
                    int pCX = s.playerX + tileSize/2;
                    int pCY = s.playerY + tileSize/2;
                    s.bullets.add(new Bullet(m.x+32, m.y+32, pCX, pCY, false));
                    m.shotCounter = 0;
                    if (m.type == 1) s.soundTrigger = 3;
                }
            } else {
                s.monsters.remove(m);
            }
        }

        // 6. Cek Game Over (Player kena hit)
        Rectangle playerRect = new Rectangle(s.playerX + 16, s.playerY + 16, 32, 48);

        // Kena Peluru
        for (Bullet b : s.bullets) {
            if (!b.isActive || b.isPlayerBullet) continue;
            if (b.getBounds().intersects(playerRect)) {
                if (s.isShieldActive) {
                    b.isActive = false; continue;
                }
                s.isGameOver = true;
                s.soundTrigger = 5; // SFX Game Over
            }
        }

        // Kena Badan Monster
        for (Monster m : s.monsters) {
            if (m.isActive && m.getBounds().intersects(playerRect)) {
                if (s.isShieldActive) continue;
                s.isGameOver = true;
                s.soundTrigger = 5;
            }
        }
    }

    private void handleKeyInput(GameplayState s, GameplayIntent.InputKey k) {
        int code = k.keyCode;
        boolean pressed = k.isPressed;

        if (code == KeyEvent.VK_W) s.upPressed = pressed;
        if (code == KeyEvent.VK_S) s.downPressed = pressed;
        if (code == KeyEvent.VK_A) s.leftPressed = pressed;
        if (code == KeyEvent.VK_D) s.rightPressed = pressed;

        if (pressed) {
            // Logic Tombol Sekali Tekan
            if (code == KeyEvent.VK_F) {
                if (s.hasShield && !s.isShieldActive) {
                    s.hasShield = false;
                    s.isShieldActive = true;
                    s.shieldTimer = 0;
                    s.soundTrigger = 4; // SFX Shield
                }
            }
            if (code == KeyEvent.VK_E) {
                if (s.ammo >= 20) {
                    s.isPlasmaReady = !s.isPlasmaReady;
                    s.soundTrigger = 4;
                }
            }
        }
    }

    private void handleMouseInput(GameplayState s, GameplayIntent.InputMouse m) {
        if (m.isClick) {
            int pCX = s.playerX + tileSize/2;
            int pCY = s.playerY + tileSize/2;

            if (s.isPlasmaReady && s.ammo >= 20) {
                s.ammo -= 20;
                s.bullets.add(new Bullet(pCX, pCY, m.mouseX, m.mouseY, true, true)); // Plasma
                s.isPlasmaReady = false;
                s.soundTrigger = 7; // SFX Shoot
            } else if (s.ammo > 0) {
                s.ammo--;
                s.bullets.add(new Bullet(pCX, pCY, m.mouseX, m.mouseY, true)); // Biasa
                s.soundTrigger = 1;
            }
        }
    }

    private boolean checkTreeCollision(GameplayState s, int x, int y) {
        Rectangle playerRect = new Rectangle(x + 16, y + 32, 32, 32);
        for (Point p : s.trees) {
            // Logika hitbox pohon (copy dari panel lama)
            Rectangle treeRect = new Rectangle(p.x + 98, p.y + 196, 60, 40);
            if (playerRect.intersects(treeRect)) return true;
        }
        return false;
    }

}