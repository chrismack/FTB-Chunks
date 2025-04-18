package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.FTBChunks;
import me.shedaniel.architectury.networking.simple.MessageType;
import me.shedaniel.architectury.networking.simple.SimpleNetworkManager;

public interface FTBChunksNet {
	SimpleNetworkManager MAIN = SimpleNetworkManager.create(FTBChunks.MOD_ID);

	MessageType REQUEST_MAP_DATA = MAIN.registerC2S("request_map_data", RequestMapDataPacket::new);
	MessageType SEND_ALL_CHUNKS = MAIN.registerS2C("send_all_chunks", SendManyChunksPacket::new);
	MessageType LOGIN_DATA = MAIN.registerS2C("login_data", LoginDataPacket::new);
	MessageType REQUEST_CHUNK_CHANGE = MAIN.registerC2S("request_chunk_change", RequestChunkChangePacket::new);
	MessageType SEND_CHUNK = MAIN.registerS2C("send_chunk", SendChunkPacket::new);
	MessageType SEND_GENERAL_DATA = MAIN.registerS2C("send_general_data", SendGeneralDataPacket::new);
	MessageType TELEPORT_FROM_MAP = MAIN.registerC2S("teleport_from_map", TeleportFromMapPacket::new);
	MessageType PLAYER_DEATH = MAIN.registerS2C("player_death", PlayerDeathPacket::new);
	MessageType SEND_VISIBLE_PLAYER_LIST = MAIN.registerS2C("send_visible_player_list", SendVisiblePlayerListPacket::new);
	MessageType SYNC_TX = MAIN.registerC2S("sync_tx", SyncTXPacket::new);
	MessageType SYNC_RX = MAIN.registerS2C("sync_rx", SyncRXPacket::new);

	static void init() {
	}
}