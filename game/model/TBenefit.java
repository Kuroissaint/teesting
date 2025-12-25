package com.monster.game.model;

public class TBenefit {
    // 1. Pastikan variabel ID ada di sini
    private int id;
    private String username;
    private int skor;
    private int peluruMeleset;
    private int sisaPeluru;
    private String status;
    // 2. CONSTRUCTOR KOSONG (Wajib ada biar gak error di GameReducer)
    public TBenefit() {
    }

    // 3. CONSTRUCTOR LENGKAP
    public TBenefit(String username, int skor, int peluruMeleset, int sisaPeluru) {
        this.username = username;
        this.skor = skor;
        this.peluruMeleset = peluruMeleset;
        this.sisaPeluru = sisaPeluru;
    }

    // --- GETTER & SETTER (Pastikan setId ada!) ---

    // INI YANG BIKIN ERROR TADI (setId & getId)
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getSkor() { return skor; }
    public void setSkor(int skor) { this.skor = skor; }

    public int getPeluruMeleset() { return peluruMeleset; }
    public void setPeluruMeleset(int peluruMeleset) { this.peluruMeleset = peluruMeleset; }

    public int getSisaPeluru() { return sisaPeluru; }
    public void setSisaPeluru(int sisaPeluru) { this.sisaPeluru = sisaPeluru; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}