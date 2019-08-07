package soc.model;

public class PlayerContext
{
    public PlayerContext(String playerName, int position)
    {
        this(playerName, position, 0);
    }

    public PlayerContext(String playerName, int position, int victoryPoints)
    {
        mPlayerName = playerName;
        mPosition = position;
        mVictoryPoints = victoryPoints;
    }

    public String getPlayerName() {
        return mPlayerName;
    }

    public void setPlayerName(String playerName) {
        this.mPlayerName = playerName;
    }

    public int getPosition() {
        return mPosition;
    }

    public void setPosition(int position) {
        this.mPosition = position;
    }

    public int getVictoryPoints() {
        return mVictoryPoints;
    }

    public void setVictoryPoints(int victoryPoints) {
        this.mVictoryPoints = victoryPoints;
    }

    private String mPlayerName;
    private int mPosition;
    private int mVictoryPoints;
}
