package soc.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import soc.aws.Constants;

@DynamoDBTable(tableName = "GameTable")
public class GameIndexDAO
{
    @DynamoDBHashKey(attributeName = "GameID")
    public String getGameID()
    {
        return mGameID;
    }

    public void setGameID(String gameId) {
        this.mGameID = gameId;
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
    private String mGameID;
    private String mBoardKey;
    private String mMoveSetKey;
}
