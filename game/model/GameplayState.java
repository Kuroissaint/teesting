package com.monster.game.model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * File ini berfungsi sebagai "State" untuk layar Gameplay dalam arsitektur MVI.
 * Tugas utamanya adalah menyimpan seluruh snapshot data permainan pada satu titik waktu.
 * Sesuai prinsip MVI, class ini hanya boleh berisi data (variabel) dan tidak boleh mengandung logika perhitungan.
 */
public class GameplayState {

    // --- DATA PLAYER ---
    public int playerX, playerY;
    public int score = 0;
    public int ammo = 0;
    public int dodged = 0;
    public boolean isGameOver = false;
    public boolean isPaused = false;

    // --- STATUS INPUT ---
    // Disimpan di state agar Reducer mengetahui arah pergerakan player yang diinginkan
    public boolean upPressed, downPressed, leftPressed, rightPressed;

    // --- STATUS SKILL & POWER-UP ---
    public boolean hasShield = false;
    public boolean isShieldActive = false;
    public int shieldTimer = 0;
    public int shieldAmmoCounter = 0; // Menghitung peluru yang dilewati untuk mendapatkan shield

    public boolean isPlasmaReady = false;

    // --- LIST OBJEK GAME ---
    // Menggunakan CopyOnWriteArrayList untuk menghindari ConcurrentModificationException saat update & render
    public CopyOnWriteArrayList<Bullet> bullets = new CopyOnWriteArrayList<>();
    public CopyOnWriteArrayList<Monster> monsters = new CopyOnWriteArrayList<>();
    public ArrayList<Point> trees = new ArrayList<>();

    public int monsterSpawnCounter = 0;

    // --- EVENT TRIGGER ---
    // Digunakan untuk mengirim sinyal efek suara (Sound SE) ke View
    // -1 = Tidak ada suara, >=0 = Index SFX di class Sound
    public int soundTrigger = -1;

    /**
     * Konstruktor untuk inisialisasi awal posisi player.
     * @param startX Koordinat X awal player.
     * @param startY Koordinat Y awal player.
     */
    public GameplayState(int startX, int startY) {
        this.playerX = startX;
        this.playerY = startY;
    }
}