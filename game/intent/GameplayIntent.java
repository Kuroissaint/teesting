package com.monster.game.intent;

public interface GameplayIntent {
    // 1. Intent untuk Input Keyboard/Mouse
    class InputKey implements GameplayIntent {
        public int keyCode;
        public boolean isPressed; // True = ditekan, False = dilepas
        public InputKey(int keyCode, boolean isPressed) {
            this.keyCode = keyCode;
            this.isPressed = isPressed;
        }
    }

    class InputMouse implements GameplayIntent {
        public int mouseX, mouseY; // Posisi Mouse di World Game
        public boolean isClick;    // Apakah ini klik atau cuma gerak?
        public InputMouse(int x, int y, boolean click) {
            this.mouseX = x;
            this.mouseY = y;
            this.isClick = click;
        }
    }

    // 2. Intent Utama: Game Loop (Detak Jantung Game)
    class GameTick implements GameplayIntent {
        // Kosong, cuma penanda "Waktunya update fisika!"
    }

    // 3. Intent Reset/Start
    class InitGame implements GameplayIntent {
        public int screenW, screenH, tileSize;
        public InitGame(int w, int h, int size) {
            this.screenW = w; this.screenH = h; this.tileSize = size;
        }
    }
}