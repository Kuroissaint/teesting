package com.monster.game;

import com.monster.game.view.GameView;
import com.monster.game.viewmodel.GameViewModel;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Swing harus dijalankan di Thread khusus UI
        SwingUtilities.invokeLater(() -> {
            // 1. Siapkan Otak (ViewModel)
            GameViewModel vm = new GameViewModel();

            // 2. Siapkan Wajah (View), masukkan Otaknya
            GameView window = new GameView(vm);

            // 3. Tampilkan!
            window.setVisible(true);
        });
    }
}