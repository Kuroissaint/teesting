package com.monster.game.intent;

import com.monster.game.model.TBenefit;
import com.monster.game.util.DBConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class GameReducer {
    public void reduce(LoadDataIntent loadDataIntent) {
    }

    // Interface biar View bisa mendengar kalau ada perubahan
    public interface StateListener {
        void onStateChanged(GameState newState);
    }

    private StateListener listener;

    public void setListener(StateListener listener) {
        this.listener = listener;
    }

    // FUNGSI UTAMA: Menerima Intent, menghasilkan State
    public void processIntent(GameIntent intent) {
        // Jika user minta Load Data
        // (Kita cek nama classnya karena LoadDataIntent ada di file lain/package intent)
        if (intent.getClass().getSimpleName().equals("LoadDataIntent")) {
            loadData();
        }
    }

    private void loadData() {
        // 1. Kasih tau View: "Lagi Loading..."
        if (listener != null) listener.onStateChanged(new GameState(null, "Loading..."));

        List<TBenefit> dataList = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
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
            // 2. Sukses! Kirim data ke View
            if (listener != null) listener.onStateChanged(new GameState(dataList, "Ready"));

        } catch (Exception e) {
            // 3. Gagal! Kirim error ke View
            if (listener != null) listener.onStateChanged(new GameState(null, "Error: " + e.getMessage()));
        }
    }
}