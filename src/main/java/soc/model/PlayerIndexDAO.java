package soc.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import soc.aws.Constants;

@DynamoDBTable(tableName = "Player-MoveSet-Board")
public class PlayerIndexDAO
{
    @DynamoDBHashKey(attributeName = "Player")
    public String getPlayerName()
    {
        return mPlayerName;
    }

    public void setPlayerName(String playerName) {
        this.mPlayerName = playerName;
    }

    @DynamoDBRangeKey(attributeName = "TimeStamp")
    public Long getTimeStamp()
    {
        return mTimeStamp;
    }

    public void setTimeStamp(Long timeStamp)
    {
        this.mTimeStamp = timeStamp;
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

    @DynamoDBAttribute(attributeName = Constants.BOARD_S3KEY)
    public String getBoardKey() {
        return mBoardKey;
    }

    public void setBoardKey(String boardKey) {
        this.mBoardKey = boardKey;
    }

    @DynamoDBAttribute(attributeName = Constants.MOVESET_S3KEY)
    public String getMoveSetKey() {
        return mMoveSetKey;
    }

    public void setMoveSetKey(String moveSetKey) {
        this.mMoveSetKey = moveSetKey;
    }

    private Long mTimeStamp;
    private String mPlayerName;
    private Integer mVictoryPoints;
    private Integer mPosition;
    private String mBoardKey;
    private String mMoveSetKey;
}
