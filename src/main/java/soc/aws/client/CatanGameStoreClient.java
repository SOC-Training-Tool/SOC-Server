package soc.aws.client;

import io.circe.Json;
import soc.model.PlayerContext;

import java.util.List;

public interface CatanGameStoreClient
{
    void save(List<PlayerContext> playerList, Json moveSet, Json board);

    List<Json> getMoveSetsForPlayer(String player);

    List<Json> getBoardsForPlayer(String player);
}
