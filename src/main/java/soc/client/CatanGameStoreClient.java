package soc.client;

import io.circe.Json;
import soc.model.PlayerContext;

import java.util.List;

public interface CatanGameStoreClient
{
    void save(List<PlayerContext> playerList, Json moveSet, Json board);

    Json getMoveSetForPlayer(PlayerContext player);

    Json getBoardForPlayer(PlayerContext player);
}
