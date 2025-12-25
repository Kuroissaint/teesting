package com.monster.game.intent;

import com.monster.game.model.TBenefit;
import com.monster.game.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * File ini bertindak sebagai Reducer utama untuk bagian sistem di luar gameplay (Menu Utama).
 * Tugas utamanya adalah menangani Intent yang berkaitan dengan manajemen data,
 * seperti memuat leaderboard dan mengatur sesi mulai/lanjutkan permainan (Resume/Restart).
 */
public class GameReducer {

    /**
     * Interface untuk mendengarkan perubahan state.
     * View (seperti GameView) akan mengimplementasikan ini untuk memperbarui UI saat data siap.
     */
    public interface StateListener {
        void onStateChanged(GameState newState);
    }

    private StateListener listener;

    public void setListener(StateListener listener) {
        this.listener = listener;
    }

    /**
     * Fungsi utama untuk memproses Intent sistem.
     * Mengarahkan aksi berdasarkan tipe Intent yang diterima (Load Data atau Start Game).
     */
    public void reduce(GameIntent intent) {
        String intentName = intent.getClass().getSimpleName();

        // Menangani permintaan memuat data leaderboard
        if (intentName.equals("LoadDataIntent")) {
            loadData();
        }
        // Menangani permintaan untuk memulai permainan
        else if (intentName.equals("StartGameIntent")) {
            StartGameIntent startIntent = (StartGameIntent) intent;
            String userToCheck = startIntent.username;

            // 1. Cek status terakhir user di Database
            TBenefit existingData = getUserData(userToCheck);

            if (existingData == null) {
                // KASUS A: USER BARU -> Simpan username pertama kali dan mulai dari nol
                createInitialSave(userToCheck);
                sendPlayingSignal(userToCheck, 0, 0, 0);
            }
            else {
                // KASUS B: USER LAMA -> Tentukan apakah harus Resume atau Restart
                if ("ONGOING".equalsIgnoreCase(existingData.getStatus())) {
                    // Jika status masih ONGOING, lanjutkan permainan (Resume)
                    sendPlayingSignal(userToCheck, existingData.getSkor(), existingData.getSisaPeluru(), existingData.getPeluruMeleset());
                } else {
                    // Jika status sudah GAMEOVER, mulai dari awal (Restart)
                    // Tetap mempertahankan Highscore di DB, hanya mengubah status menjadi aktif kembali
                    resetUserStats(userToCheck);
                    sendPlayingSignal(userToCheck, 0, 0, 0);
                }
            }
        }
    }

    // --- HELPER METHODS (Logika Database) ---

    /**
     * Mengirim sinyal ke View untuk berpindah ke layar Gameplay dengan data yang ditentukan.
     */
    private void sendPlayingSignal(String user, int skor, int ammo, int dodged) {
        // Format string digunakan untuk mengirimkan paket data awal sesi ke GamePanel
        String status = "Playing:" + user + ":" + skor + ":" + ammo + ":" + dodged;
        if (listener != null) listener.onStateChanged(new GameState(null, status));
    }

    /**
     * Mengambil profil lengkap user berdasarkan username dari database.
     */
    private TBenefit getUserData(String username) {
        TBenefit data = null;
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM tbenefit WHERE username = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                data = new TBenefit();
                data.setUsername(rs.getString("username"));
                data.setSkor(rs.getInt("skor"));
                data.setPeluruMeleset(rs.getInt("peluru_meleset"));
                data.setSisaPeluru(rs.getInt("sisa_peluru"));
                data.setStatus(rs.getString("status"));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return data;
    }

    /**
     * Menyimpan username baru ke database saat pertama kali tombol Play diklik.
     */
    private void createInitialSave(String username) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "INSERT INTO tbenefit (username, skor, peluru_meleset, sisa_peluru, status) VALUES (?, 0, 0, 0, 'ONGOING')";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Mengubah status user yang sudah mati menjadi aktif kembali tanpa menghapus Highscore.
     */
    private void resetUserStats(String username) {
        try (Connection conn = DBConnection.getConnection()) {
            // Kita hanya mengubah status menjadi ONGOING agar sesi dianggap baru,
            // skor tetap dibiarkan agar tidak merusak Highscore yang sudah ada.
            String sql = "UPDATE tbenefit SET status='ONGOING' WHERE username=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Memuat daftar peringkat (Leaderboard) dari database berdasarkan skor tertinggi.
     */
    private void loadData() {
        List<TBenefit> data = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM tbenefit ORDER BY skor DESC";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TBenefit item = new TBenefit();
                item.setUsername(rs.getString("username"));
                item.setSkor(rs.getInt("skor"));
                item.setPeluruMeleset(rs.getInt("peluru_meleset"));
                item.setSisaPeluru(rs.getInt("sisa_peluru"));
                item.setStatus(rs.getString("status"));
                data.add(item);
            }
            // Memberitahu View bahwa data leaderboard telah siap ditampilkan
            if (listener != null) listener.onStateChanged(new GameState(data, "Data Loaded"));
        } catch (Exception e) { e.printStackTrace(); }
    }
}