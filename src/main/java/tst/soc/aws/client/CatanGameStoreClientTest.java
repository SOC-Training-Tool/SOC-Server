package tst.soc.aws.client;

import io.circe.Json;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import soc.aws.client.CatanGameStoreClient;
import soc.aws.client.CatanGameStoreClientFactory;
import soc.model.PlayerContext;

import java.util.ArrayList;
import java.util.List;

public class CatanGameStoreClientTest
{
    @Before
    public void init()
    {
        mCatanClient = CatanGameStoreClientFactory.createClient();
    }

    @Test
    public void testClient()
    {
        Json moveSetStaticJson = Json.fromString("TEST_JSON_MOVESET");
        Json boardStaticJson = Json.fromString("TEST_JSON_BOARD");

        mCatanClient.save(generatePlayerContextList(), moveSetStaticJson, boardStaticJson);

        List<Json> moveSetJsonList = mCatanClient.getMoveSetsForPlayer("TestPlayer1");
        List<Json> boardJsonList = mCatanClient.getBoardsForPlayer("TestPlayer1");

        for (Json moveSetJson : moveSetJsonList)
        {
            Assert.assertEquals(moveSetJson.asString().get(), "TEST_JSON_MOVESET");
        }

        for (Json boardJson : boardJsonList)
        {
            Assert.assertEquals(boardJson.asString().get(), "TEST_JSON_BOARD");
        }
    }

    private List<PlayerContext> generatePlayerContextList()
    {
        List<PlayerContext> playerContextList = new ArrayList<>(4);
        playerContextList.add(new PlayerContext("TestPlayer1", 1, 10));
        playerContextList.add(new PlayerContext("TestPlayer2", 2, 9));
        playerContextList.add(new PlayerContext("TestPlayer3", 3, 8));
        playerContextList.add(new PlayerContext("TestPlayer4", 4, 7));

        return playerContextList;
    }

    private CatanGameStoreClient mCatanClient;
}
