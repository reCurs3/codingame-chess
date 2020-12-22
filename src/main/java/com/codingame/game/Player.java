package com.codingame.game;
import com.codingame.gameengine.core.AbstractMultiplayerPlayer;

public class Player extends AbstractMultiplayerPlayer {
    private String[] inputVariables;

    @Override
    public int getExpectedOutputLines() {
        return 1;
    }

    public String[] getInputVariables() { return inputVariables; }
    public void setInputVariables(String configuration) {
        this.inputVariables = configuration.split(" ");
    }
}
