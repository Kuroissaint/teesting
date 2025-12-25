package com.monster.game.model;

import com.monster.game.view.Bullet;
import com.monster.game.view.Monster;
import java.awt.Point;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

// CLASS INI CUMA BOLEH ISI DATA, GAK BOLEH ADA LOGIKA
public class GameplayState {

    // Data Player
    public int playerX, playerY;
    public int score = 0;
    public int ammo = 0;
    public int dodged = 0;
    public boolean isGameOver = false;

    // Status Input (Disimpan di state biar Reducer tau player lagi mau gerak kemana)
    public boolean upPressed, downPressed, leftPressed, rightPressed;

    // Status Skill
    public boolean hasShield = false;
    public boolean isShieldActive = false;
    public int shieldTimer = 0;
    public int shieldAmmoCounter = 0;

    public boolean isPlasmaReady = false;

    // List Objek Game
    public CopyOnWriteArrayList<Bullet> bullets = new CopyOnWriteArrayList<>();
    public CopyOnWriteArrayList<Monster> monsters = new CopyOnWriteArrayList<>();
    public ArrayList<Point> trees = new ArrayList<>();

    public int monsterSpawnCounter = 0;

    // Event Trigger (Untuk Sound di View)
    // 0 = No Sound, >0 = Index Sound
    public int soundTrigger = -1;

    // Constructor Default
    public GameplayState(int startX, int startY) {
        this.playerX = startX;
        this.playerY = startY;
    }
}