package tst.soc.aws.client;

import io.circe.Json;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import soc.aws.client.CatanGameStoreClient;
import soc.aws.client.CatanGameStoreClientFactory;

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
        byte[] moveSetStaticJson = "TEST_JSON_MOVESET".getBytes();
        byte[] boardStaticJson = "TEST_JSON_BOARD".getBytes();

        mCatanClient.save("test-gameId-0", moveSetStaticJson, boardStaticJson);

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



    private CatanGameStoreClient mCatanClient;
}
