package com.monster.game.model;

public class TBenefit {
    // Sesuai kolom database
    private String username;
    private int skor;
    private int peluruMeleset;
    private int sisaPeluru;

    public TBenefit(String username, int skor, int peluruMeleset, int sisaPeluru) {
        this.username = username;
        this.skor = skor;
        this.peluruMeleset = peluruMeleset;
        this.sisaPeluru = sisaPeluru;
    }

    // Getter (Biar bisa dibaca)
    public String getUsername() { return username; }
    public int getSkor() { return skor; }
    public int getPeluruMeleset() { return peluruMeleset; }
    public int getSisaPeluru() { return sisaPeluru; }
}