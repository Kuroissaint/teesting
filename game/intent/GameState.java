package com.monster.game.intent;
import com.monster.game.model.TBenefit;

import java.util.List;

public class GameState {
    // Data apa saja yang tampil di layar?
    private List<TBenefit> leaderboard; // Tabel skor
    private String status;              // Sedang "Loading", "Ready", atau "Error"

    public GameState(List<TBenefit> leaderboard, String status) {
        this.leaderboard = leaderboard;
        this.status = status;
    }

    public List<TBenefit> getLeaderboard() {
        return leaderboard;
    }
    public String getStatus() { return status; }
}