package com.monster.game.util;

import java.net.URL;
import javax.sound.sampled.*;

public class Sound {
    private Clip musicClip; // Khusus untuk BGM
    private Clip sfxClip;   // Khusus untuk SFX
    URL[] soundURL = new URL[10];

    public Sound() {
        soundURL[0] = getClass().getResource("/com/monster/game/assets/bgm_forest.wav");
        soundURL[1] = getClass().getResource("/com/monster/game/assets/sfx_bullet.wav");
        soundURL[2] = getClass().getResource("/com/monster/game/assets/sfx_batM.wav");
        soundURL[3] = getClass().getResource("/com/monster/game/assets/sfx_plantM.wav");
        soundURL[4] = getClass().getResource("/com/monster/game/assets/sfx_shield.wav");
        soundURL[5] = getClass().getResource("/com/monster/game/assets/sfx_over.wav");
        soundURL[6] = getClass().getResource("/com/monster/game/assets/sfx_hit.wav");
        soundURL[7] = getClass().getResource("/com/monster/game/assets/sfx_plasma.wav");
    }

    // Untuk Musik (BGM)
    public void setMusic(int i) {
        try {
            if (musicClip != null && musicClip.isOpen()) musicClip.close();
            AudioInputStream ais = AudioSystem.getAudioInputStream(soundURL[i]);
            musicClip = AudioSystem.getClip();
            musicClip.open(ais);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void playMusic() {
        if (musicClip != null) {
            musicClip.setFramePosition(0); // Restart dari awal
            musicClip.start();
        }
    }

    public void loopMusic() {
        if (musicClip != null) musicClip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    public void stopMusic() {
        if (musicClip != null) musicClip.stop();
    }

    // Untuk SFX (Efek Suara)
    public void playSE(int i) {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(soundURL[i]);
            sfxClip = AudioSystem.getClip();
            sfxClip.open(ais);
            sfxClip.start(); // Mainkan sekali saja
        } catch (Exception e) { e.printStackTrace(); }
    }
}