package com.monster.game.intent;

/**
 * File ini mendefinisikan berbagai kategori aksi atau event spesifik yang terjadi selama gameplay.
 * Dalam arsitektur MVI, kelas-kelas di dalam interface ini digunakan sebagai pesan (Intent)
 * yang dikirim dari View (GamePanel) ke Reducer (GameplayReducer) untuk memperbarui state permainan.
 */
public interface GameplayIntent {

    /**
     * Intent untuk menangani interaksi input dari pengguna melalui keyboard.
     */
    class InputKey implements GameplayIntent {
        public int keyCode;
        public boolean isPressed; // True jika tombol ditekan, False jika dilepas

        public InputKey(int keyCode, boolean isPressed) {
            this.keyCode = keyCode;
            this.isPressed = isPressed;
        }
    }

    /**
     * Intent untuk menangani interaksi mouse, baik posisi koordinat maupun status klik.
     */
    class InputMouse implements GameplayIntent {
        public int mouseX, mouseY; // Posisi Mouse di dalam koordinat dunia game
        public boolean isClick;    // Menandakan apakah aksi tersebut adalah klik atau sekadar pergerakan

        public InputMouse(int x, int y, boolean click) {
            this.mouseX = x;
            this.mouseY = y;
            this.isClick = click;
        }
    }

    /**
     * Intent utama yang bertindak sebagai "detak jantung" permainan.
     * Digunakan oleh Timer untuk memberitahu Reducer bahwa sudah waktunya memperbarui logika fisika.
     */
    class GameTick implements GameplayIntent {
        // Kosong, hanya digunakan sebagai sinyal pemicu update sistem.
    }

    /**
     * Intent untuk beralih antara status permainan aktif (Resume) dan berhenti sejenak (Pause).
     */
    class TogglePause implements GameplayIntent { }

    /**
     * Intent yang digunakan untuk inisialisasi awal atau reset data permainan.
     * Membawa data dasar seperti dimensi layar dan ukuran tile.
     */
    class InitGame implements GameplayIntent {
        public int screenW, screenH, tileSize;

        public InitGame(int w, int h, int size) {
            this.screenW = w;
            this.screenH = h;
            this.tileSize = size;
        }
    }
}