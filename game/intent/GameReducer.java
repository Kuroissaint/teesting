package com.monster.game.intent;

import com.monster.game.model.TBenefit;
import com.monster.game.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class GameReducer {
    public interface StateListener {
        void onStateChanged(GameState newState);
    }

    private StateListener listener;

    public void setListener(StateListener listener) {
        this.listener = listener;
    }

    public void reduce(GameIntent intent) {
        String intentName = intent.getClass().getSimpleName();

        if (intentName.equals("LoadDataIntent")) {
            loadData();
        }
        else if (intentName.equals("StartGameIntent")) {
            StartGameIntent startIntent = (StartGameIntent) intent;
            String userToCheck = startIntent.username;

            // 1. Cek apakah user sudah ada di DB?
            TBenefit existingData = getUserData(userToCheck);

            if (existingData == null) {
                // KASUS A: USER BARU -> INSERT AWAL
                createInitialSave(userToCheck);
                // Main dari 0
                sendPlayingSignal(userToCheck, 0, 0, 0);
            }
            else {
                // KASUS B: USER LAMA -> CEK STATUS
                if ("ONGOING".equalsIgnoreCase(existingData.getStatus())) {
                    // Masih hidup -> RESUME
                    sendPlayingSignal(userToCheck, existingData.getSkor(), existingData.getSisaPeluru(), existingData.getPeluruMeleset());
                } else {
                    // Sudah mati (GAMEOVER) -> RESTART DARI 0
                    // Kita update statusnya jadi ONGOING lagi biar aktif
                    resetUserStats(userToCheck);
                    sendPlayingSignal(userToCheck, 0, 0, 0);
                }
            }
        }
    }

    // --- HELPER METHODS ---

    private void sendPlayingSignal(String user, int skor, int ammo, int dodged) {
        // Format: "Playing:User:Skor:Ammo:Meleset"
        String status = "Playing:" + user + ":" + skor + ":" + ammo + ":" + dodged;
        if (listener != null) listener.onStateChanged(new GameState(null, status));
    }

    // Ambil data user spesifik
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

    // Insert user baru (Sesuai spek: "Username disimpan jika tombol Play diklik")
    private void createInitialSave(String username) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "INSERT INTO tbenefit (username, skor, peluru_meleset, sisa_peluru, status) VALUES (?, 0, 0, 0, 'ONGOING')";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Reset user yang sudah Game Over jadi 0 lagi
    private void resetUserStats(String username) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "UPDATE tbenefit SET skor=0, peluru_meleset=0, sisa_peluru=0, status='ONGOING' WHERE username=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

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
            if (listener != null) listener.onStateChanged(new GameState(data, "Data Loaded"));
        } catch (Exception e) { e.printStackTrace(); }
    }
}