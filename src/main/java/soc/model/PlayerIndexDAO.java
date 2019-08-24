package soc.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "Player-MoveSet-Board")
public class PlayerIndexDAO
{
    @DynamoDBIndexHashKey(attributeName = "Player")
    public String getPlayerName()
    {
        return mPlayerName;
    }

    public void setPlayerName(String playerName) {
        this.mPlayerName = playerName;
    }

    @DynamoDBAttribute(attributeName = "VictoryPoints")
    public Integer getVictoryPoints() {
        return mVictoryPoints;
    }

    public void setVictoryPoints(Integer victoryPoints) {
        this.mVictoryPoints = victoryPoints;
    }

    @DynamoDBAttribute(attributeName = "Position")
    public Integer getPosition() {
        return mPosition;
    }

    public void setPosition(Integer position) {
        this.mPosition = position;
    }

    @DynamoDBAttribute(attributeName = "Board_S3Key")
    public String getBoardKey() {
        return mBoardKey;
    }

    public void setBoardKey(String boardKey) {
        this.mBoardKey = boardKey;
    }

    @DynamoDBAttribute(attributeName = "MoveSet_S3Key")
    public String getMoveSetKey() {
        return mMoveSetKey;
    }

    public void setMoveSetKey(String moveSetKey) {
        this.mMoveSetKey = moveSetKey;
    }

    private String mPlayerName;
    private Integer mVictoryPoints;
    private Integer mPosition;
    private String mBoardKey;
    private String mMoveSetKey;
}
