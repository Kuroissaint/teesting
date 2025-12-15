package com.monster.game;

import com.monster.game.intent.GameReducer; // Import yang baru
import com.monster.game.view.GameView;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Ganti GameViewModel jadi GameReducer
            GameReducer reducer = new GameReducer();

            // Masukkan reducer ke View
            GameView window = new GameView(reducer);

            window.setVisible(true);
        });
    }
}