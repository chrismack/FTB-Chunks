package dev.ftb.mods.ftbchunks.client.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.MinimapRenderer;
import dev.ftb.mods.ftbchunks.client.RegionMapPanel;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class FTBChunksClientImpl {
	public static void registerPlatform() {
		WorldRenderEvents.AFTER_TRANSLUCENT.register(FTBChunksClientImpl::renderWorldLastFabric);
	}

	private static void renderWorldLastFabric(WorldRenderContext context) {
		((FTBChunksClient) FTBChunks.PROXY).renderWorldLast(context.matrixStack());
	}

	public static void renderMinimap(MapDimension dimension, MinimapRenderer renderer) {
	}

	public static void addWidgets(RegionMapPanel panel) {
	}

}
