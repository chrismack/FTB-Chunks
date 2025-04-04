package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.FTBChunks;
import me.shedaniel.architectury.networking.NetworkManager;
import me.shedaniel.architectury.networking.simple.BaseS2CMessage;
import me.shedaniel.architectury.networking.simple.MessageType;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * @author LatvianModder
 */
public class PlayerDeathPacket extends BaseS2CMessage {
	public final ResourceKey<Level> dimension;
	public final int x, y, z, number;

	public PlayerDeathPacket(ResourceKey<Level> dim, int _x, int _y, int _z, int num) {
		dimension = dim;
		x = _x;
		y = _y;
		z = _z;
		number = num;
	}

	PlayerDeathPacket(FriendlyByteBuf buf) {
		dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation());
		x = buf.readVarInt();
		y = buf.readVarInt();
		z = buf.readVarInt();
		number = buf.readVarInt();
	}

	@Override
	public MessageType getType() {
		return FTBChunksNet.PLAYER_DEATH;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeResourceLocation(dimension.location());
		buf.writeVarInt(x);
		buf.writeVarInt(y);
		buf.writeVarInt(z);
		buf.writeVarInt(number);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBChunks.PROXY.playerDeath(this);
	}
}