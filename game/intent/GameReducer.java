package com.monster.game.intent;

import com.monster.game.model.TBenefit;
import com.monster.game.util.DBConnection; // Pastikan ini sesuai lokasi file DBConnection kamu

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class GameReducer {

    // --- BAGIAN LISTENER (Jembatan ke View) ---
    public interface StateListener {
        void onStateChanged(GameState newState);
    }

    private StateListener listener;

    public void setListener(StateListener listener) {
        this.listener = listener;
    }

    // --- FUNGSI UTAMA (Ganti nama dari processIntent jadi reduce) ---
    public void reduce(GameIntent intent) {
        // Cek nama class dari Intent yang masuk
        String intentName = intent.getClass().getSimpleName();

        if (intentName.equals("LoadDataIntent")) {
            loadData();
        }
        else if (intentName.equals("StartGameIntent")) {
            // 1. Ambil data username dari Intent
            StartGameIntent startIntent = (StartGameIntent) intent;

            // 2. Beri tahu View untuk ganti status jadi "Playing"
            if (listener != null) {
                listener.onStateChanged(new GameState(null, "Playing:" + startIntent.username));
            }
        }
    }

    // Logika ambil data database
    private void loadData() {
        // 1. Loading
        if (listener != null) listener.onStateChanged(new GameState(null, "Loading..."));

        List<TBenefit> dataList = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) throw new Exception("Tidak bisa konek ke Database!");

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM tbenefit");

            while (rs.next()) {
                dataList.add(new TBenefit(
                        rs.getString("username"),
                        rs.getInt("skor"),
                        rs.getInt("peluru_meleset"),
                        rs.getInt("sisa_peluru")
                ));
            }
            // 2. Sukses
            if (listener != null) listener.onStateChanged(new GameState(dataList, "Ready"));

        } catch (Exception e) {
            // 3. Error
            e.printStackTrace(); // Biar muncul di console errornya apa
            if (listener != null) listener.onStateChanged(new GameState(null, "Error: " + e.getMessage()));
        }
    }
}