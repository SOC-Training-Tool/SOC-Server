package soc.client;

import io.circe.Json;

import java.util.List;

public interface CatanGameStoreClient
{
    String save(String gameId, Json moveSet, Json board);
}
