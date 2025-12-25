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

public class GameView extends JFrame implements GameReducer.StateListener {

    private GameReducer reducer;
    private JTable scoreTable;
    private DefaultTableModel tableModel;
    private JTextField usernameField;
    private Sound menuMusic = new Sound();

    // Warna Tema
    private final Color PRIMARY_COLOR = new Color(0, 120, 215); // Biru Modern
    private final Color ACCENT_COLOR = new Color(0, 200, 83);   // Hijau Aksen
    private final Color DANGER_COLOR = new Color(220, 53, 69);  // Merah
    private final Font MAIN_FONT = new Font("Segoe UI", Font.BOLD, 14);

    public GameView(GameReducer reducer) {
        this.reducer = reducer;
        reducer.setListener(this);
        setTitle("Hide and Seek: The Challenge");
        setSize(900, 650); // Agak diperlebar dikit
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Setup Musik
        menuMusic.setMusic(0);

        showMainMenu();
    }

    public void showMainMenu() {
        getContentPane().removeAll();

        // 1. Background Full Image
        BackgroundPanel mainPanel = new BackgroundPanel("/com/monster/game/assets/menu_bg.png");
        mainPanel.setLayout(new GridBagLayout()); // Pakai GridBag biar container di tengah
        setContentPane(mainPanel);

        // Nyalakan Musik
        menuMusic.playMusic();
        menuMusic.loopMusic();

        // --- GLASS CONTAINER (Panel Transparan di Tengah) ---
        JPanel glassPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 180)); // Hitam Transparan (Glass Effect)
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30); // Sudut Melengkung
                super.paintComponent(g);
            }
        };
        glassPanel.setOpaque(false);
        glassPanel.setLayout(new BorderLayout(0, 20));
        glassPanel.setBorder(new EmptyBorder(30, 40, 30, 40)); // Padding dalam panel
        glassPanel.setPreferredSize(new Dimension(600, 500));

        // --- A. JUDUL ---
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

        // --- B. TABEL HIGHSCORE (Tanpa Status) ---
        // Kolom: Rank, Username, Score, Missed, Ammo
        String[] columnNames = {"#", "USERNAME", "SCORE", "MISSED", "AMMO"};

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        scoreTable = new JTable(tableModel);
        styleTable(scoreTable); // Panggil fungsi styling biar cantik

        // Listener Klik Tabel
        scoreTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selectedRow = scoreTable.getSelectedRow();
                if (selectedRow != -1) {
                    String selectedUser = (String) scoreTable.getValueAt(selectedRow, 1); // Ambil Kolom 1 (Username)
                    usernameField.setText(selectedUser);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(scoreTable);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        glassPanel.add(scrollPane, BorderLayout.CENTER);

        // --- C. INPUT & TOMBOL ---
        JPanel bottomPanel = new JPanel(new GridLayout(2, 1, 0, 15));
        bottomPanel.setOpaque(false);

        // Input Username
        JPanel inputWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        inputWrapper.setOpaque(false);

        JLabel lblUser = new JLabel("Identity: ");
        lblUser.setForeground(Color.LIGHT_GRAY);
        lblUser.setFont(MAIN_FONT);

        usernameField = new JTextField(15);
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameField.setHorizontalAlignment(JTextField.CENTER);
        usernameField.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, PRIMARY_COLOR)); // Garis bawah aja
        usernameField.setOpaque(false);
        usernameField.setForeground(Color.WHITE);
        usernameField.setCaretColor(Color.CYAN);
        usernameField.setToolTipText("Select form table or type new");

        inputWrapper.add(lblUser);
        inputWrapper.add(usernameField);
        bottomPanel.add(inputWrapper);

        // Tombol Action
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        btnPanel.setOpaque(false);

        JButton btnPlay = createStyledButton("START MISSION", ACCENT_COLOR);
        JButton btnQuit = createStyledButton("ABORT", DANGER_COLOR);

        btnPanel.add(btnPlay);
        btnPanel.add(btnQuit);
        bottomPanel.add(btnPanel);

        glassPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Tambahkan Glass Panel ke Main Panel (Center)
        mainPanel.add(glassPanel);

        // --- ACTIONS ---
        btnQuit.addActionListener(e -> System.exit(0));
        btnPlay.addActionListener(e -> {
            String user = usernameField.getText();
            if (!user.trim().isEmpty()) {
                reducer.reduce(new StartGameIntent(user));
            } else {
                JOptionPane.showMessageDialog(this, "Please identify yourself, Soldier!");
            }
        });

        revalidate(); repaint();
        reducer.reduce(new LoadDataIntent());
    }

    // --- HELPER UNTUK STYLING TOMBOL ---
    private JButton createStyledButton(String text, Color baseColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(baseColor.brighter()); // Terang pas di hover
                } else {
                    g2.setColor(baseColor);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20); // Tombol Bulat
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

    // --- HELPER UNTUK STYLING TABEL ---
    private void styleTable(JTable table) {
        table.setRowHeight(30);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(255, 255, 255, 50));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(0, 120, 215, 100)); // Biru transparan pas diklik
        table.setSelectionForeground(Color.WHITE);
        table.setOpaque(false);
        ((DefaultTableCellRenderer)table.getDefaultRenderer(Object.class)).setOpaque(false);

        // Header Style
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(0, 0, 0, 0)); // Header transparan
        header.setForeground(Color.BLACK);
        header.setOpaque(false);
        ((DefaultTableCellRenderer)header.getDefaultRenderer()).setOpaque(false);
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
            String[] parts = newState.getStatus().split(":");
            String user = parts[1];
            int skor = (parts.length > 2) ? Integer.parseInt(parts[2]) : 0;
            int ammo = (parts.length > 3) ? Integer.parseInt(parts[3]) : 0;
            int dodged = (parts.length > 4) ? Integer.parseInt(parts[4]) : 0;

            startGameplay(user, skor, ammo, dodged);
        }
    }

    private void startGameplay(String username, int score, int ammo, int dodged) {
        menuMusic.stopMusic();
        getContentPane().removeAll();
        GamePanel gamePanel = new GamePanel(username, this);
        gamePanel.loadSession(score, ammo, dodged);
        add(gamePanel);
        pack();
        setLocationRelativeTo(null);
        revalidate(); repaint();
        gamePanel.requestFocusInWindow();
        gamePanel.startGameThread();
    }

    private void updateTable(List<TBenefit> data) {
        tableModel.setRowCount(0);
        int rank = 1;
        // Kita loop datanya, tapi JANGAN ambil statusnya
        for (TBenefit row : data) {
            tableModel.addRow(new Object[]{
                    rank++, // Kolom 1: Ranking (1, 2, 3...)
                    row.getUsername(),
                    row.getSkor(),
                    row.getPeluruMeleset(),
                    row.getSisaPeluru()
                    // Status TIDAK dimasukkan di sini
            });
        }
    }

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