package com.monster.game.view;

// Import dari package INTENT (Sesuai Diagram Dosen)
import com.monster.game.intent.GameReducer;
import com.monster.game.intent.GameState;
import com.monster.game.intent.LoadDataIntent;
import com.monster.game.intent.StartGameIntent;

// Import dari package MODEL
import com.monster.game.model.TBenefit;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class GameView extends JFrame implements GameReducer.StateListener {

    // Sekarang kita pakai istilah "Reducer"
    private GameReducer reducer;

    private JTable scoreTable;
    private DefaultTableModel tableModel;
    private JTextField usernameField;
    private JButton btnPlay;
    private JButton btnQuit;

    // Constructor terima GameReducer
    public GameView(GameReducer reducer) {
        this.reducer = reducer;

        // Setup Jendela Utama
        setTitle("Monster RPG: The Challenge");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 1. Bagian Judul
        JLabel titleLabel = new JLabel("MONSTER LEADERBOARD", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Monospaced", Font.BOLD, 24));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20,0,20,0));
        add(titleLabel, BorderLayout.NORTH);

        // 2. Bagian Tabel Skor
        String[] columnNames = {"Username", "Skor", "Meleset", "Sisa Peluru"};
        tableModel = new DefaultTableModel(columnNames, 0);
        scoreTable = new JTable(tableModel);
        add(new JScrollPane(scoreTable), BorderLayout.CENTER);

        // 3. Bagian Input & Tombol
        JPanel bottomPanel = new JPanel(new GridLayout(3, 1));

        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("Username: "));
        usernameField = new JTextField(15);
        inputPanel.add(usernameField);
        bottomPanel.add(inputPanel);

        JPanel buttonPanel = new JPanel();
        btnPlay = new JButton("PLAY");
        btnQuit = new JButton("QUIT");
        buttonPanel.add(btnPlay);
        buttonPanel.add(btnQuit);
        bottomPanel.add(buttonPanel);

        add(bottomPanel, BorderLayout.SOUTH);

        // --- SETUP AKSI (INTENT) ---

        btnQuit.addActionListener(e -> System.exit(0));

        btnPlay.addActionListener(e -> {
            String user = usernameField.getText();
            if (!user.isEmpty()) {
                // Kirim Intent "StartGame" ke Reducer
                // Pastikan class StartGameIntent public dan punya constructor(username)
                reducer.reduce(new StartGameIntent(user));
                JOptionPane.showMessageDialog(this, "Halo " + user + ", Game akan dimulai!");
            } else {
                JOptionPane.showMessageDialog(this, "Isi username dulu dong!");
            }
        });

        // Binding: View mendengarkan Reducer
        reducer.setListener(this);

        // Panggil data awal (LoadData)
        reducer.reduce(new LoadDataIntent());
    }

    // --- FUNGSI UPDATE TAMPILAN (Dari State) ---
    @Override
    public void onStateChanged(GameState newState) {
        // Cek Status
        if (newState.getStatus().equals("Loading...")) {
            setTitle("Loading Data...");
        }

        // Update Tabel jika ada data
        if (newState.getLeaderboard() != null) {
            updateTable(newState.getLeaderboard());
            setTitle("Monster RPG: Ready!");
        }

        // Tampilkan Error jika ada
        if (newState.getStatus().startsWith("Error")) {
            JOptionPane.showMessageDialog(this, newState.getStatus());
        }
    }

    private void updateTable(List<TBenefit> data) {
        tableModel.setRowCount(0);
        for (TBenefit row : data) {
            tableModel.addRow(new Object[]{
                    row.getUsername(),
                    row.getSkor(),
                    row.getPeluruMeleset(),
                    row.getSisaPeluru()
            });
        }
    }
}