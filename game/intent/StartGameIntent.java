package com.monster.game.intent;

public class StartGameIntent implements GameIntent {
    public String username;

    public StartGameIntent(String username) {
        this.username = username;
    }
}