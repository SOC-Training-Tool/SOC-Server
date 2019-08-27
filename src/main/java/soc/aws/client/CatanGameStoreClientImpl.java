package soc.aws.client;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import io.circe.Json;
import soc.aws.Constants;
import soc.model.PlayerContext;
import soc.model.PlayerIndexDAO;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class CatanGameStoreClientImpl implements CatanGameStoreClient
{
    public CatanGameStoreClientImpl(
            final AmazonS3 s3Client,
            final DynamoDBMapper dynamoDBMapper)
    {
        mS3Client = s3Client;
        mDynamoDb = dynamoDBMapper;
    }

    @Override
    public void save(List<PlayerContext> playerList, Json moveSet, Json board)
    {
        String moveSetKey = generateS3Key(MOVE_SET);
        ByteArrayInputStream moveSetData = convertJsonToBytes(moveSet);

        String boardKey = generateS3Key(BOARD);
        ByteArrayInputStream boardData = convertJsonToBytes(board);

        indexInDynamo(playerList, moveSetKey, boardKey);

        s3Put(moveSetKey, moveSetData, Constants.MOVESET_BUCKET);
        s3Put(boardKey, boardData, Constants.BOARD_BUCKET);
    }

    private void indexInDynamo(List<PlayerContext> playerList, String moveSetKey, String boardKey)
    {
        for (PlayerContext player : playerList)
        {
            PlayerIndexDAO dao = new PlayerIndexDAO();
            dao.setBoardKey(boardKey);
            dao.setMoveSetKey(moveSetKey);
            dao.setPlayerName(player.getPlayerName());
            dao.setPosition(player.getPosition());
            dao.setVictoryPoints(player.getVictoryPoints());
            dao.setTimeStamp(new Date().getTime());

            mDynamoDb.save(dao);
        }
    }

    private void s3Put(String key, ByteArrayInputStream data, String bucket)
    {
        ObjectMetadata metadata = new ObjectMetadata();
        PutObjectRequest putRequest = new PutObjectRequest(bucket, key, data, metadata);
        mS3Client.putObject(putRequest);
    }

    @Override
    public List<Json> getMoveSetsForPlayer(String player)
    {
        return queryAndGet(player, MOVE_SET);
    }

    @Override
    public List<Json> getBoardsForPlayer(String player)
    {
        return queryAndGet(player, BOARD);
    }

    private List<Json> queryAndGet(String player, String type)
    {
        PlayerIndexDAO hashKeyValues = new PlayerIndexDAO();
        hashKeyValues.setPlayerName(player);

        String queryType = type.equals(MOVE_SET) ? Constants.MOVESET_S3KEY : Constants.BOARD_S3KEY;
        String bucket = type.equals(MOVE_SET) ? Constants.MOVESET_BUCKET : Constants.BOARD_BUCKET;

        List<PlayerIndexDAO> queryList =
                mDynamoDb.query(PlayerIndexDAO.class, buildQueryExpression(hashKeyValues, queryType));

        List<Json> jsonList = new ArrayList<>();

        try
        {
            for (PlayerIndexDAO playerDao : queryList)
            {
                S3Object s3Object =
                        type.equals(MOVE_SET)
                                ? mS3Client.getObject(bucket, playerDao.getMoveSetKey())
                                : mS3Client.getObject(bucket, playerDao.getBoardKey());

                jsonList.add(Json.fromString(new String(IOUtils.toByteArray(s3Object.getObjectContent()))));
            }
        }
        catch (IOException e)
        {
            System.err.println(e.getMessage());
        }

        return jsonList;
    }

    private <T extends PlayerIndexDAO> DynamoDBQueryExpression<T> buildQueryExpression(T hashKeyValues, String attribute)
    {
        return new DynamoDBQueryExpression<T>()
                .withHashKeyValues(hashKeyValues)
                .withProjectionExpression(attribute);
    }

    private String generateS3Key(String prefix)
    {
        long timeStamp = new Date().getTime();
        String uuid = UUID.randomUUID().toString();
        return String.format("%s-%s-%s", prefix, timeStamp, uuid);
    }

    /**
     * TODO: build a serialization / compression module for this
     */
    private ByteArrayInputStream convertJsonToBytes(Json json)
    {
        return new ByteArrayInputStream(json.toString().getBytes());
    }

    private AmazonS3 mS3Client;
    private DynamoDBMapper mDynamoDb;
    private static final String MOVE_SET = "moveset";
    private static final String BOARD = "board";

}
