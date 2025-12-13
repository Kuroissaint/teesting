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
    private JButton btnPlay;
    private JButton btnQuit;

    public GameView(GameReducer reducer) {
        this.reducer = reducer;

        // --- 1. SETUP JENDELA UTAMA ---
        setTitle("Hide and Seek The Challenge");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // --- 2. PASANG BACKGROUND (PENTING!) ---
        // Kita pakai class khusus "BackgroundPanel" di bawah
        // Pastikan nama file gambarmu "menu_bg.png" ada di folder assets
        BackgroundPanel mainPanel = new BackgroundPanel("/com/monster/game/assets/menu_bg.png");
        mainPanel.setLayout(new BorderLayout());
        setContentPane(mainPanel); // Jadikan ini alas utama layar

        // --- 3. JUDUL (Bagian Atas) ---
        JLabel titleLabel = new JLabel("HIDE AND SEEK THE CHALLENGE", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE); // Warna Putih biar kontras
        // Tambah efek bayangan hitam biar makin kebaca
        titleLabel.setText("<html><span style='color:white; text-shadow: 2px 2px #000000;'>HIDE AND SEEK THE CHALLENGE</span></html>");
        titleLabel.setBorder(BorderFactory.createEmptyBorder(30, 0, 10, 0));
        add(titleLabel, BorderLayout.NORTH);

        // --- 4. AREA TENGAH (Input & Tabel) ---
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false); // TRANSPARAN! Biar background kelihatan

        // A. Input Username
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        inputPanel.setOpaque(false); // Transparan

        JLabel lblUser = new JLabel("Username   ");
        lblUser.setFont(new Font("Serif", Font.BOLD, 16));
        lblUser.setForeground(Color.WHITE); // Teks Putih

        usernameField = new JTextField(15);

        inputPanel.add(lblUser);
        inputPanel.add(usernameField);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        centerPanel.add(inputPanel, BorderLayout.NORTH);

        // B. Tabel Skor
        String[] columnNames = {"Username", "Skor", "Peluru Meleset", "Sisa Peluru"};
        tableModel = new DefaultTableModel(columnNames, 0);
        scoreTable = new JTable(tableModel);

        scoreTable.setFillsViewportHeight(true);
        scoreTable.setRowHeight(25);
        // Background tabel agak transparan (putih 90%) biar tulisan hitam tetap kebaca
        scoreTable.setBackground(new Color(255, 255, 255, 230));
        scoreTable.getTableHeader().setBackground(Color.LIGHT_GRAY);

        JScrollPane scrollPane = new JScrollPane(scoreTable);
        scrollPane.getViewport().setOpaque(false); // Area dalam scroll transparan
        scrollPane.setOpaque(false); // Frame scroll transparan

        // Wadah Tabel (Untuk Padding Kiri-Kanan)
        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setOpaque(false); // Transparan
        tableWrapper.add(scrollPane, BorderLayout.CENTER);
        tableWrapper.setBorder(BorderFactory.createEmptyBorder(0, 50, 10, 50));

        centerPanel.add(tableWrapper, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // --- 5. TOMBOL (Bagian Bawah) ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 20));
        buttonPanel.setOpaque(false); // Transparan

        btnPlay = new JButton("Play");
        btnPlay.setPreferredSize(new Dimension(100, 35));
        btnPlay.setBackground(new Color(34, 139, 34)); // Hijau Hutan
        btnPlay.setForeground(Color.WHITE);
        btnPlay.setFont(new Font("Serif", Font.BOLD, 14));
        btnPlay.setFocusPainted(false); // Hilangkan garis fokus

        btnQuit = new JButton("Quit");
        btnQuit.setPreferredSize(new Dimension(100, 35));
        btnQuit.setBackground(new Color(178, 34, 34)); // Merah Bata
        btnQuit.setForeground(Color.WHITE);
        btnQuit.setFont(new Font("Serif", Font.BOLD, 14));
        btnQuit.setFocusPainted(false);

        buttonPanel.add(btnPlay);
        buttonPanel.add(btnQuit);

        add(buttonPanel, BorderLayout.SOUTH);

        // --- 6. LOGIKA MVI ---
        btnQuit.addActionListener(e -> System.exit(0));

        btnPlay.addActionListener(e -> {
            String user = usernameField.getText();
            if (!user.isEmpty()) {
                reducer.reduce(new StartGameIntent(user));
            } else {
                JOptionPane.showMessageDialog(this, "Username tidak boleh kosong!");
            }
        });

        reducer.setListener(this);
        reducer.reduce(new LoadDataIntent());
    }

    // Update Tampilan dari Database
    @Override
    public void onStateChanged(GameState newState) {

        // 1. Cek Status Loading/Error (Seperti biasa)
        if (newState.getStatus().equals("Loading...")) {
            // Opsional: tampilin loading spinner
        }
        else if (newState.getStatus().startsWith("Error")) {
            JOptionPane.showMessageDialog(this, newState.getStatus());
        }

        // 2. Cek Data Leaderboard (Buat Menu Awal)
        if (newState.getLeaderboard() != null) {
            updateTable(newState.getLeaderboard());
        }

        // --- 3. LOGIKA BARU: PINDAH KE GAMEPLAY ---
        // Kalau statusnya "Playing:PlayerOne", kita mulai game!
        if (newState.getStatus().startsWith("Playing:")) {
            // Ambil nama user dari status
            String username = newState.getStatus().split(":")[1];
            startGameplay(username);
        }
    }

    // Fungsi Pembantu: Ganti Layar Menu -> Game
    private void startGameplay(String username) {
        getContentPane().removeAll();

        GamePanel gamePanel = new GamePanel(username);
        add(gamePanel);

        // --- TAMBAHAN PENTING ---
        // Biar gamenya pas di tengah dan ukurannya pas dulu di awal
        pack();
        setLocationRelativeTo(null); // Center di layar monitor
        setResizable(true); // IZINKAN USER UBAH UKURAN JENDELA
        // ------------------------

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

    // --- CLASS RAHASIA: BACKGROUND PANEL ---
    // Ini class tambahan di dalam GameView untuk menangani gambar
    class BackgroundPanel extends JPanel {
        private Image backgroundImage;

        public BackgroundPanel(String resourcePath) {
            try {
                // Load gambar dari folder assets
                URL url = getClass().getResource(resourcePath);
                if (url != null) {
                    backgroundImage = new ImageIcon(url).getImage();
                } else {
                    System.err.println("GAMBAR TIDAK DITEMUKAN: " + resourcePath);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backgroundImage != null) {
                // Gambar image memenuhi seluruh panel (Stretch)
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        }
    }
}