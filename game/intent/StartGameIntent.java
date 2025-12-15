package com.monster.game.intent;

public class StartGameIntent implements GameIntent {

    public String username;

    public StartGameIntent(String user) {
        this.username = user;
    }
}