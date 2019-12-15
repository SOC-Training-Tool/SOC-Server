package soc.aws.client;

import io.circe.Json;
import java.util.List;

public interface CatanGameStoreClient
{
    void save(String gameId, byte[] moveSet, byte[] board);

    List<Json> getMoveSetsForPlayer(String player);

    List<Json> getBoardsForPlayer(String player);
}
