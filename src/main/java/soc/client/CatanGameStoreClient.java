package soc.client;

import io.circe.Json;
import soc.model.PlayerContext;

import java.util.List;

public interface CatanGameStoreClient
{
    String save(List<PlayerContext> playerList, Json moveSet, Json board);
}
