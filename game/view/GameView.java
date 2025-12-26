package com.monster.game.view;

import com.monster.game.intent.GameReducer;
import com.monster.game.intent.GameState;
import com.monster.game.intent.LoadDataIntent;
import com.monster.game.intent.StartGameIntent;
import com.monster.game.model.TBenefit;
import com.monster.game.util.Sound;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.List;

/**
 * File ini bertindak sebagai "View" atau Frame utama dalam arsitektur MVI.
 * Tugas utamanya adalah:
 * 1. Menjadi wadah (Container) bagi seluruh layar aplikasi (Menu Utama dan Gameplay).
 * 2. Menampilkan antarmuka Menu Utama, termasuk tabel Leaderboard dan input pemain.
 * 3. Menghubungkan interaksi pengguna di menu (seperti klik tombol Play) ke Reducer melalui Intent.
 * 4. Mendengarkan perubahan State (StateListener) untuk memperbarui tampilan secara otomatis.
 */
public class GameView extends JFrame implements GameReducer.StateListener {

    private GameReducer reducer;
    private JTable scoreTable;
    private DefaultTableModel tableModel;
    private JTextField usernameField;
    private Sound menuMusic = new Sound();

    // --- KONFIGURASI TEMA VISUAL ---
    private final Color PRIMARY_COLOR = new Color(0, 120, 215); // Biru Modern
    private final Color ACCENT_COLOR = new Color(0, 200, 83);   // Hijau Aksen
    private final Color DANGER_COLOR = new Color(220, 53, 69);  // Merah
    private final Font MAIN_FONT = new Font("Segoe UI", Font.BOLD, 14);

    /**
     * Konstruktor: Inisialisasi frame utama dan menghubungkannya dengan Reducer.
     */
    public GameView(GameReducer reducer) {
        this.reducer = reducer;
        reducer.setListener(this); // Mendaftarkan diri sebagai pengamat (Observer) perubahan State

        setTitle("Hide and Seek: The Challenge");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Setup musik latar untuk menu
        menuMusic.setMusic(0);

        showMainMenu();
    }

    /**
     * Membangun dan menampilkan layar Menu Utama (Main Menu).
     */
    public void showMainMenu() {
        getContentPane().removeAll();

        // 1. Menyiapkan Panel Background Utama
        BackgroundPanel mainPanel = new BackgroundPanel("/com/monster/game/assets/menu_bg.png");
        mainPanel.setLayout(new GridBagLayout());
        setContentPane(mainPanel);

        // Menjalankan musik menu
        menuMusic.playMusic();
        menuMusic.loopMusic();

        // --- GLASS CONTAINER (Panel Transparan Tengah) ---
        JPanel glassPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 180)); // Efek kaca gelap transparan
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                super.paintComponent(g);
            }
        };
        glassPanel.setOpaque(false);
        glassPanel.setLayout(new BorderLayout(0, 20));
        glassPanel.setBorder(new EmptyBorder(30, 40, 30, 40));
        glassPanel.setPreferredSize(new Dimension(600, 500));

        // --- A. BAGIAN JUDUL (HEADER) ---
        JLabel titleLabel = new JLabel("HIDE AND SEEK", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);

        JLabel subTitleLabel = new JLabel("THE CHALLENGE", SwingConstants.CENTER);
        subTitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        subTitleLabel.setForeground(Color.WHITE);
        subTitleLabel.setBorder(new EmptyBorder(0, 0, 20, 0));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subTitleLabel, BorderLayout.CENTER);
        glassPanel.add(headerPanel, BorderLayout.NORTH);

        // --- B. TABEL LEADERBOARD ---
        String[] columnNames = {"#", "USERNAME", "SCORE", "MISSED", "AMMO"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        scoreTable = new JTable(tableModel);
        styleTable(scoreTable); // Memberikan gaya visual modern pada tabel

        // Event: Mengisi username otomatis saat baris tabel diklik
        scoreTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selectedRow = scoreTable.getSelectedRow();
                if (selectedRow != -1) {
                    String selectedUser = (String) scoreTable.getValueAt(selectedRow, 1);
                    usernameField.setText(selectedUser);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(scoreTable);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        glassPanel.add(scrollPane, BorderLayout.CENTER);

        // --- C. PANEL INPUT & TOMBOL (FOOTER) ---
        JPanel bottomPanel = new JPanel(new GridLayout(2, 1, 0, 15));
        bottomPanel.setOpaque(false);

        // Input Identity (Username)
        JPanel inputWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        inputWrapper.setOpaque(false);

        JLabel lblUser = new JLabel("Identity: ");
        lblUser.setForeground(Color.LIGHT_GRAY);
        lblUser.setFont(MAIN_FONT);

        usernameField = new JTextField(15);
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameField.setHorizontalAlignment(JTextField.CENTER);
        usernameField.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, PRIMARY_COLOR));
        usernameField.setOpaque(false);
        usernameField.setForeground(Color.WHITE);
        usernameField.setCaretColor(Color.CYAN);

        inputWrapper.add(lblUser);
        inputWrapper.add(usernameField);
        bottomPanel.add(inputWrapper);

        // Tombol Start & Quit
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        btnPanel.setOpaque(false);

        JButton btnPlay = createStyledButton("START MISSION", ACCENT_COLOR);
        JButton btnQuit = createStyledButton("ABORT", DANGER_COLOR);

        btnPanel.add(btnPlay);
        btnPanel.add(btnQuit);
        bottomPanel.add(btnPanel);

        glassPanel.add(bottomPanel, BorderLayout.SOUTH);
        mainPanel.add(glassPanel);

        // --- DEFINISI AKSI (INTENT) ---
        btnQuit.addActionListener(e -> System.exit(0));

        // Mengirim StartGameIntent ke Reducer saat tombol diklik
        btnPlay.addActionListener(e -> {
            String user = usernameField.getText();
            if (!user.trim().isEmpty()) {
                reducer.reduce(new StartGameIntent(user));
            } else {
                JOptionPane.showMessageDialog(this, "Please identify yourself, Soldier!");
            }
        });

        revalidate(); repaint();

        // Pemicu awal: Mengambil data leaderboard saat menu terbuka
        reducer.reduce(new LoadDataIntent());
    }

    /**
     * Method callback yang dipanggil oleh Reducer setiap kali ada perubahan State.
     * Ini adalah inti dari reaktivitas View dalam MVI.
     */
    @Override
    public void onStateChanged(GameState newState) {
        // 1. Menampilkan pesan error jika ada
        if (newState.getStatus().startsWith("Error")) {
            JOptionPane.showMessageDialog(this, newState.getStatus().replace("Error: ", ""));
        }
        // 2. Memperbarui isi tabel jika ada data leaderboard baru
        else if (newState.getLeaderboard() != null) {
            updateTable(newState.getLeaderboard());
        }
        // 3. Berpindah ke layar Gameplay jika status berubah menjadi "Playing"
        else if (newState.getStatus().startsWith("Playing:")) {
            String[] parts = newState.getStatus().split(":");
            String user = parts[1];
            int skor = (parts.length > 2) ? Integer.parseInt(parts[2]) : 0;
            int ammo = (parts.length > 3) ? Integer.parseInt(parts[3]) : 0;
            int dodged = (parts.length > 4) ? Integer.parseInt(parts[4]) : 0;

            startGameplay(user, skor, ammo, dodged);
        }
    }

    /**
     * Berpindah dari layar Menu Utama ke layar Permainan (Game Engine).
     */
    private void startGameplay(String username, int score, int ammo, int dodged) {
        menuMusic.stopMusic();
        getContentPane().removeAll(); // Membersihkan menu

        // Memasang GamePanel (Engine Gameplay) sebagai layar aktif
        GamePanel gamePanel = new GamePanel(username, this);
        gamePanel.loadSession(score, ammo, dodged); // Memuat statistik terakhir (Resume)

        add(gamePanel);
        pack();
        setLocationRelativeTo(null);
        revalidate(); repaint();

        gamePanel.requestFocusInWindow();
        gamePanel.startGameThread(); // Menjalankan loop permainan
    }

    /**
     * Memperbarui baris tabel leaderboard dengan data terbaru dari database.
     */
    private void updateTable(List<TBenefit> data) {
        tableModel.setRowCount(0);
        int rank = 1;
        for (TBenefit row : data) {
            tableModel.addRow(new Object[]{
                    rank++,
                    row.getUsername(),
                    row.getSkor(),
                    row.getPeluruMeleset(),
                    row.getSisaPeluru()
            });
        }
    }

    /**
     * Helper: Menciptakan tombol dengan desain modern dan efek hover.
     */
    private JButton createStyledButton(String text, Color baseColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? baseColor.brighter() : baseColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                super.paintComponent(g);
            }
        };
        btn.setPreferredSize(new Dimension(140, 40));
        btn.setForeground(Color.WHITE);
        btn.setFont(MAIN_FONT);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Helper: Mengatur tampilan visual JTable agar selaras dengan tema gelap transparan.
     */
    private void styleTable(JTable table) {
        table.setRowHeight(30);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(255, 255, 255, 50));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(0, 120, 215, 100));
        table.setSelectionForeground(Color.WHITE);
        table.setOpaque(false);
        ((DefaultTableCellRenderer)table.getDefaultRenderer(Object.class)).setOpaque(false);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(0, 0, 0, 0));
        header.setForeground(Color.BLACK);
        header.setOpaque(false);
        ((DefaultTableCellRenderer)header.getDefaultRenderer()).setOpaque(false);
    }

    /**
     * Komponen kustom untuk menampilkan gambar latar belakang pada panel.
     */
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