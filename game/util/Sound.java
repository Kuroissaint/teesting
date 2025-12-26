package com.monster.game.util;

import java.net.URL;
import javax.sound.sampled.*;

/**
 * File ini berfungsi sebagai manajer audio untuk seluruh permainan.
 * Tugas utamanya adalah memuat file suara dari folder assets, mengelola pemutaran
 * musik latar (BGM) agar bisa berulang (loop), serta memutar efek suara (SFX) secara instan.
 */
public class Sound {
    // Clip khusus untuk musik latar (BGM) agar bisa dikontrol (stop/loop) secara terpisah
    private Clip musicClip;

    // Clip untuk efek suara (SFX) pendek
    private Clip sfxClip;

    // Array untuk menyimpan alamat URL dari file-file audio
    URL[] soundURL = new URL[10];

    /**
     * Konstruktor untuk mendaftarkan semua file audio yang ada di folder assets.
     * Setiap indeks mewakili jenis suara tertentu yang akan dipanggil oleh Reducer.
     */
    public Sound() {
        soundURL[0] = getClass().getResource("/com/monster/game/assets/bgm_forest.wav"); // Musik Latar
        soundURL[1] = getClass().getResource("/com/monster/game/assets/sfx_bullet.wav"); // Suara Tembakan
        soundURL[2] = getClass().getResource("/com/monster/game/assets/sfx_batM.wav");   // Spawn Kelelawar
        soundURL[3] = getClass().getResource("/com/monster/game/assets/sfx_plantM.wav"); // Spawn Tanaman
        soundURL[4] = getClass().getResource("/com/monster/game/assets/sfx_shield.wav"); // Aktivasi Shield
        soundURL[5] = getClass().getResource("/com/monster/game/assets/sfx_over.wav");   // Game Over
        soundURL[6] = getClass().getResource("/com/monster/game/assets/sfx_hit.wav");    // Monster Terkena Hit
        soundURL[7] = getClass().getResource("/com/monster/game/assets/sfx_plasma.wav"); // Tembakan Plasma
    }

    // --- MANAJEMEN MUSIK (BGM) ---

    /**
     * Mempersiapkan file audio tertentu untuk dijadikan musik latar.
     * @param i Indeks file audio dalam array soundURL.
     */
    public void setMusic(int i) {
        try {
            if (musicClip != null && musicClip.isOpen()) musicClip.close();
            AudioInputStream ais = AudioSystem.getAudioInputStream(soundURL[i]);
            musicClip = AudioSystem.getClip();
            musicClip.open(ais);
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Memulai pemutaran musik latar dari posisi awal.
     */
    public void playMusic() {
        if (musicClip != null) {
            musicClip.setFramePosition(0);
            musicClip.start();
        }
    }

    /**
     * Mengatur agar musik latar diputar secara terus-menerus.
     */
    public void loopMusic() {
        if (musicClip != null) musicClip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    /**
     * Menghentikan pemutaran musik latar.
     */
    public void stopMusic() {
        if (musicClip != null) musicClip.stop();
    }

    // --- MANAJEMEN EFEK SUARA (SFX) ---

    /**
     * Memutar efek suara pendek secara instan (sekali putar).
     * @param i Indeks file audio dalam array soundURL.
     */
    public void playSE(int i) {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(soundURL[i]);
            sfxClip = AudioSystem.getClip();
            sfxClip.open(ais);
            sfxClip.start();
        } catch (Exception e) { e.printStackTrace(); }
    }
}