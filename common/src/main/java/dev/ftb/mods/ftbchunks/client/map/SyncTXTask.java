package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftbchunks.net.PartialPackets;
import me.shedaniel.architectury.networking.simple.BaseC2SMessage;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.zip.DeflaterOutputStream;

/**
 * @author LatvianModder
 */
public class SyncTXTask implements MapTask {
	public final MapRegion region;
	public final long now;

	public SyncTXTask(MapRegion r, long s) {
		region = r;
		now = s;
	}

	@Override
	public void runMapTask(MapManager m) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try (DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new DeflaterOutputStream(out)))) {
			/*
			MapRegion.Images images = region.getImages();

			byte[] dataImgBytes = images.data.getBytes();
			stream.writeInt(dataImgBytes.length);
			stream.write(dataImgBytes);

			byte[] blockImgBytes = images.blocks.getBytes();
			stream.writeInt(blockImgBytes.length);
			stream.write(blockImgBytes);

			stream.writeShort(region.getChunks().size());

			for (MapChunk chunk : region.getChunks().values())
			{
				stream.writeByte(chunk.pos.x);
				stream.writeByte(chunk.pos.z);
				stream.writeLong(now - chunk.modified);
			}
			 */
		} catch (Exception ex) {
			ex.printStackTrace();
			return;
		}

		PartialPackets.REGION.write(region.getSyncKey(), out.toByteArray()).forEach(BaseC2SMessage::sendToServer);
	}

	@Override
	public boolean cancelOtherTasks() {
		return true;
	}
}