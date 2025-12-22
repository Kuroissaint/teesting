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

            // 1. Cek data save terakhir
            TBenefit savedData = getLastSaveData(userToCheck);

            if (savedData != null) {
                // Kalo ada data ONGOING -> Kirim sinyal RESUME
                // Format: "Resume:USERNAME:SKOR:AMMO:DODGED"
                String resumeStatus = "Resume:" + userToCheck + ":" +
                        savedData.getSkor() + ":" +
                        savedData.getSisaPeluru() + ":" +
                        savedData.getPeluruMeleset();

                if (listener != null) listener.onStateChanged(new GameState(null, resumeStatus));

            } else {
                // Kalo gak ada data atau GAMEOVER -> Kirim sinyal NEW GAME
                if (listener != null) listener.onStateChanged(new GameState(null, "New:" + userToCheck));
            }
        }
    }

    private void loadData() {
        List<TBenefit> data = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            // Ambil Top 10 Highscore
            String sql = "SELECT * FROM tbenefit ORDER BY skor DESC LIMIT 10";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TBenefit item = new TBenefit();
                item.setId(rs.getInt("id"));
                item.setUsername(rs.getString("username"));
                item.setSkor(rs.getInt("skor"));
                item.setPeluruMeleset(rs.getInt("peluru_meleset"));
                item.setSisaPeluru(rs.getInt("sisa_peluru"));
                data.add(item);
            }
            if (listener != null) {
                listener.onStateChanged(new GameState(data, "Data Loaded"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- INI FUNGSI YANG KAMU CARI (getLastSaveData) ---
    // Taruh di paling bawah tapi masih di dalam class GameReducer
    private TBenefit getLastSaveData(String username) {
        TBenefit data = null;
        try (Connection conn = DBConnection.getConnection()) {

            // Ambil 1 data terakhir milik user ini
            String sql = "SELECT * FROM tbenefit WHERE username = ? ORDER BY id DESC LIMIT 1";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // CEK STATUSNYA DULU!
                String statusTerakhir = rs.getString("status");

                // Kalau statusnya "ONGOING" (Keluar pas main) -> BISA DI-LOAD
                if ("ONGOING".equalsIgnoreCase(statusTerakhir)) {
                    data = new TBenefit();
                    data.setUsername(rs.getString("username"));
                    data.setSkor(rs.getInt("skor"));
                    data.setPeluruMeleset(rs.getInt("peluru_meleset"));
                    data.setSisaPeluru(rs.getInt("sisa_peluru"));
                }
                else {
                    // Kalau "GAMEOVER" -> Anggap data kosong (Bikin baru)
                    return null;
                }
            }
        } catch (Exception e) {
            // Kalau error kolom 'status' tidak ditemukan, berarti kamu belum update DB
            // Jalankan perintah SQL: ALTER TABLE tbenefit ADD COLUMN status VARCHAR(20) DEFAULT 'GAMEOVER';
            e.printStackTrace();
        }
        return data;
    }
}