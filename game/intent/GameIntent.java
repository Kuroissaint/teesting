package com.monster.game.intent;

// Interface kosong sebagai penanda
public interface GameIntent {}

// Niat 2: User ingin mulai main (nanti kita pakai)
public class StartGameIntent implements GameIntent {
    public StartGameIntent(String user) {
    }
}