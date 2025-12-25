package com.monster.game.util;

import javax.swing.*;
import java.net.URL;
import javax.sound.sampled.*;

public class Sound {
    Clip clip;
    URL[] soundURL = new URL[10]; // Bisa simpan sampai 10 lagu/sfx

    public Sound() {
        // 0. MUSIK LATAR (Menu & Game Sama)
        soundURL[0] = getClass().getResource("/com/monster/game/assets/bgm_forest.wav");

        // 1. SFX TEMBAKAN PLAYER
        soundURL[1] = getClass().getResource("/com/monster/game/assets/sfx_bullet.wav");

        // 2. SFX KELELAWAR (Pas Mati)
        soundURL[2] = getClass().getResource("/com/monster/game/assets/sfx_batM.wav");

        // 3. SFX TANAMAN (Pas Nembak)
        soundURL[3] = getClass().getResource("/com/monster/game/assets/sfx_plantM.wav");

        // 4. SFX SHIELD (Pas Aktif)
        soundURL[4] = getClass().getResource("/com/monster/game/assets/sfx_shield.wav");

        // 5. SFX GAME OVER (Pas Aktif)
        soundURL[5] = getClass().getResource("/com/monster/game/assets/sfx_over.wav");
    }

    public void setFile(int i) {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(soundURL[i]);
            clip = AudioSystem.getClip();
            clip.open(ais);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void play() {
        if (clip != null) {
            clip.start();
        }
    }

    public void loop() {
        if (clip != null) {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public void stop() {
        if (clip != null) {
            clip.stop();
        }
    }
}