package com.monster.game.view;

import com.monster.game.intent.GameReducer;
import com.monster.game.intent.GameState;
import com.monster.game.intent.LoadDataIntent;
import com.monster.game.intent.StartGameIntent;
import com.monster.game.model.TBenefit;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URL;
import java.util.List;

public class GameView extends JFrame implements GameReducer.StateListener {

    private GameReducer reducer;
    private JTable scoreTable;
    private DefaultTableModel tableModel;
    private JTextField usernameField;

    public GameView(GameReducer reducer) {
        this.reducer = reducer;
        reducer.setListener(this);

        // Setup Dasar Jendela
        setTitle("Hide and Seek The Challenge");
        setSize(800, 600); // Saya gedein dikit biar lega
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false); // Menu awal jangan di-resize dulu biar rapi

        // Tampilkan Menu Awal
        showMainMenu();
    }

    // --- FUNGSI BARU: MENAMPILKAN MENU UTAMA ---
    public void showMainMenu() {
        // 1. Bersihkan layar
        getContentPane().removeAll();

        // --- 2. SETUP UI & TABEL DULU (PENTING: Jangan Load Data di sini) ---

        BackgroundPanel mainPanel = new BackgroundPanel("/com/monster/game/assets/menu_bg.png");
        mainPanel.setLayout(new BorderLayout());
        setContentPane(mainPanel);

        // Judul
        JLabel titleLabel = new JLabel("<html><span style='color:white; text-shadow: 2px 2px #000000;'>HIDE AND SEEK THE CHALLENGE</span></html>", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 28));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(30, 0, 10, 0));
        add(titleLabel, BorderLayout.NORTH);

        // Panel Tengah
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        inputPanel.setOpaque(false);
        JLabel lblUser = new JLabel("Username   ");
        lblUser.setFont(new Font("Serif", Font.BOLD, 16));
        lblUser.setForeground(Color.WHITE);
        usernameField = new JTextField(15);
        inputPanel.add(lblUser);
        inputPanel.add(usernameField);
        centerPanel.add(inputPanel, BorderLayout.NORTH);

        // --- INISIALISASI TABLE MODEL (INI YANG BIKIN ERROR TADI) ---
        // Kita buat ini DULUAN sebelum minta data
        String[] columnNames = {"Username", "Skor", "Meleset", "Sisa Peluru"};
        tableModel = new DefaultTableModel(columnNames, 0);
        scoreTable = new JTable(tableModel);
        scoreTable.setRowHeight(25);
        scoreTable.setBackground(new Color(255, 255, 255, 230));
        JScrollPane scrollPane = new JScrollPane(scoreTable);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);

        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setOpaque(false);
        tableWrapper.add(scrollPane, BorderLayout.CENTER);
        tableWrapper.setBorder(BorderFactory.createEmptyBorder(0, 50, 10, 50));
        centerPanel.add(tableWrapper, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // Tombol Bawah
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 20));
        buttonPanel.setOpaque(false);

        JButton btnPlay = new JButton("Play");
        styleButton(btnPlay, new Color(34, 139, 34));

        JButton btnQuit = new JButton("Quit");
        styleButton(btnQuit, new Color(178, 34, 34));

        buttonPanel.add(btnPlay);
        buttonPanel.add(btnQuit);
        add(buttonPanel, BorderLayout.SOUTH);

        // Action Listeners
        btnQuit.addActionListener(e -> System.exit(0));
        btnPlay.addActionListener(e -> {
            String user = usernameField.getText();
            if (!user.trim().isEmpty()) {
                reducer.reduce(new StartGameIntent(user));
            } else {
                JOptionPane.showMessageDialog(this, "Username tidak boleh kosong!");
            }
        });

        // Refresh UI
        revalidate();
        repaint();

        // --- 3. BARU LOAD DATA SEKARANG ---
        // Karena tableModel sudah dibuat di atas, sekarang aman.
        reducer.reduce(new LoadDataIntent());
    }

    private void styleButton(JButton btn, Color color) {
        btn.setPreferredSize(new Dimension(100, 35));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Serif", Font.BOLD, 14));
        btn.setFocusPainted(false);
    }

    @Override
    public void onStateChanged(GameState newState) {
        if (newState.getStatus().startsWith("Error")) {
            JOptionPane.showMessageDialog(this, newState.getStatus().replace("Error: ", ""));
        }
        else if (newState.getLeaderboard() != null) {
            updateTable(newState.getLeaderboard());
        }
        else if (newState.getStatus().startsWith("Playing:")) {
            String username = newState.getStatus().split(":")[1];
            startGameplay(username);
        }
    }

    private void startGameplay(String username) {
        getContentPane().removeAll();

        // PENTING: Kita kirim 'this' (GameView) ke GamePanel
        // Supaya GamePanel bisa manggil fungsi showMainMenu() nanti
        GamePanel gamePanel = new GamePanel(username, this);

        add(gamePanel);
        pack();
        setLocationRelativeTo(null);
        revalidate();
        repaint();
        gamePanel.requestFocusInWindow();
        gamePanel.startGameThread();
    }

    private void updateTable(List<TBenefit> data) {
        tableModel.setRowCount(0);
        for (TBenefit row : data) {
            tableModel.addRow(new Object[]{
                    row.getUsername(), row.getSkor(), row.getPeluruMeleset(), row.getSisaPeluru()
            });
        }
    }

    // Inner Class BackgroundPanel (Tetap Sama)
    class BackgroundPanel extends JPanel {
        private Image backgroundImage;
        public BackgroundPanel(String resourcePath) {
            try {
                URL url = getClass().getResource(resourcePath);
                if (url != null) backgroundImage = new ImageIcon(url).getImage();
            } catch (Exception e) { e.printStackTrace(); }
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backgroundImage != null) g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}