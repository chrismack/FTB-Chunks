package dev.ftb.mods.ftbchunks.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import dev.ftb.mods.ftbchunks.ColorMapLoader;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksCommon;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.client.map.MapChunk;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.MapManager;
import dev.ftb.mods.ftbchunks.client.map.MapRegion;
import dev.ftb.mods.ftbchunks.client.map.MapRegionData;
import dev.ftb.mods.ftbchunks.client.map.MapTask;
import dev.ftb.mods.ftbchunks.client.map.RegionSyncKey;
import dev.ftb.mods.ftbchunks.client.map.ReloadChunkFromLevelPacketTask;
import dev.ftb.mods.ftbchunks.client.map.ReloadChunkTask;
import dev.ftb.mods.ftbchunks.client.map.UpdateChunkFromServerTask;
import dev.ftb.mods.ftbchunks.client.map.Waypoint;
import dev.ftb.mods.ftbchunks.client.map.WaypointType;
import dev.ftb.mods.ftbchunks.client.map.color.ColorUtils;
import dev.ftb.mods.ftbchunks.core.ClientboundSectionBlocksUpdatePacketFTBC;
import dev.ftb.mods.ftbchunks.data.PlayerLocation;
import dev.ftb.mods.ftbchunks.net.LoginDataPacket;
import dev.ftb.mods.ftbchunks.net.PartialPackets;
import dev.ftb.mods.ftbchunks.net.PlayerDeathPacket;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket;
import dev.ftb.mods.ftbchunks.net.SendManyChunksPacket;
import dev.ftb.mods.ftbchunks.net.SendVisiblePlayerListPacket;
import dev.ftb.mods.ftblibrary.icon.FaceIcon;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftblibrary.ui.CustomClickEvent;
import dev.ftb.mods.ftblibrary.util.ClientUtils;
import dev.ftb.mods.ftbteams.data.ClientTeam;
import dev.ftb.mods.ftbteams.event.ClientTeamPropertiesChangedEvent;
import dev.ftb.mods.ftbteams.event.TeamEvent;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import me.shedaniel.architectury.annotations.ExpectPlatform;
import me.shedaniel.architectury.event.events.GuiEvent;
import me.shedaniel.architectury.event.events.client.ClientPlayerEvent;
import me.shedaniel.architectury.event.events.client.ClientRawInputEvent;
import me.shedaniel.architectury.event.events.client.ClientScreenInputEvent;
import me.shedaniel.architectury.event.events.client.ClientTickEvent;
import me.shedaniel.architectury.platform.Platform;
import me.shedaniel.architectury.registry.KeyBindings;
import me.shedaniel.architectury.registry.ReloadListeners;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class FTBChunksClient extends FTBChunksCommon {
	private static final ResourceLocation BUTTON_ID = new ResourceLocation("ftbchunks:open_gui");
	public static final ResourceLocation CIRCLE_MASK = new ResourceLocation("ftbchunks:textures/circle_mask.png");
	public static final ResourceLocation CIRCLE_BORDER = new ResourceLocation("ftbchunks:textures/circle_border.png");
	public static final ResourceLocation PLAYER = new ResourceLocation("ftbchunks:textures/player.png");
	public static final ResourceLocation[] COMPASS = {
			new ResourceLocation("ftbchunks:textures/compass_e.png"),
			new ResourceLocation("ftbchunks:textures/compass_n.png"),
			new ResourceLocation("ftbchunks:textures/compass_w.png"),
			new ResourceLocation("ftbchunks:textures/compass_s.png"),
	};

	private static final List<Component> MINIMAP_TEXT_LIST = new ArrayList<>(3);

	private static final ArrayDeque<MapTask> taskQueue = new ArrayDeque<>();
	public static long taskQueueTicks = 0L;
	public static Map<ChunkPos, IntOpenHashSet> rerenderCache = new HashMap<>();

	public static void queue(MapTask task) {
		taskQueue.addLast(task);
	}

	public static KeyMapping openMapKey;
	public static KeyMapping zoomInKey;
	public static KeyMapping zoomOutKey;

	public static int minimapTextureId = -1;
	private int currentPlayerChunkX, currentPlayerChunkZ;
	private static int renderedDebugCount = 0;

	public static boolean updateMinimap = false;
	public static boolean alwaysRenderChunksOnMap = false;
	public static SendGeneralDataPacket generalData;
	private long nextRegionSave = 0L;
	private double prevZoom = FTBChunksClientConfig.MINIMAP_ZOOM.get();
	private long lastZoomTime = 0L;

	@Override
	public void init() {
		FTBChunksClientConfig.init();
		registerKeys();

		ReloadListeners.registerReloadListener(PackType.CLIENT_RESOURCES, new EntityIcons());
		ReloadListeners.registerReloadListener(PackType.CLIENT_RESOURCES, new ColorMapLoader());
		ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(this::loggedOut);
		CustomClickEvent.EVENT.register(this::customClick);
		ClientRawInputEvent.KEY_PRESSED.register(this::keyPressed);
		ClientScreenInputEvent.KEY_PRESSED_PRE.register(this::keyPressed);
		GuiEvent.RENDER_HUD.register(this::renderHud);
		GuiEvent.INIT_PRE.register(this::screenOpened);
		ClientTickEvent.CLIENT_PRE.register(this::clientTick);
		TeamEvent.CLIENT_PROPERTIES_CHANGED.register(this::teamPropertiesChanged);
		registerPlatform();
	}

	private static void registerKeys() {
		// Keybinding to open Large map screen
		openMapKey = new KeyMapping("key.ftbchunks.map", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.ui");
		KeyBindings.registerKeyBinding(openMapKey);

		// Keybindings to zoom in minimap
		zoomInKey = new KeyMapping("key.ftbchunks.minimap.zoomIn", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UP, "key.categories.ui");
		KeyBindings.registerKeyBinding(zoomInKey);

		zoomOutKey = new KeyMapping("key.ftbchunks.minimap.zoomOut", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_DOWN, "key.categories.ui");
		KeyBindings.registerKeyBinding(zoomOutKey);
	}

	@ExpectPlatform
	public static void registerPlatform() {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static void renderMinimap(MapDimension dimension, MinimapRenderer renderer) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static void addWidgets(RegionMapPanel panel) {
		throw new AssertionError();
	}

	public static void openGui() {
		new LargeMapScreen().openGui();
	}

	public static void saveAllRegions() {
		if (MapManager.inst == null) {
			return;
		}

		for (MapDimension dimension : MapManager.inst.getDimensions().values()) {
			for (MapRegion region : dimension.getLoadedRegions()) {
				if (region.saveData) {
					queue(region);
					region.saveData = false;
				}
			}

			if (dimension.saveData) {
				queue(dimension);
				dimension.saveData = false;
			}
		}

		if (MapManager.inst.saveData) {
			queue(MapManager.inst);
			MapManager.inst.saveData = false;
		}
	}

	@Override
	public void login(LoginDataPacket loginData) {
		FTBChunksWorldConfig.CONFIG.read(loginData.config);

		Path dir = Platform.getGameFolder().resolve("local/ftbchunks/data/" + loginData.serverId);

		if (Files.notExists(dir)) {
			try {
				Files.createDirectories(dir);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		MapManager.inst = new MapManager(loginData.serverId, dir);
		updateMinimap = true;
		renderedDebugCount = 0;
	}

	public void loggedOut(@Nullable LocalPlayer player) {
		if (MapManager.inst != null) {
			saveAllRegions();

			MapTask t;

			while ((t = taskQueue.pollFirst()) != null) {
				t.runMapTask(MapManager.inst);
			}

			MapDimension.updateCurrent();
			MapManager.inst.release();
			MapManager.inst = null;
		}
	}

	@Override
	public void updateGeneralData(SendGeneralDataPacket packet) {
		generalData = packet;
	}

	@Override
	public void updateChunk(SendChunkPacket packet) {
		if (MapManager.inst == null) {
			return;
		}

		MapDimension dimension = MapManager.inst.getDimension(packet.dimension);
		Date now = new Date();
		queue(new UpdateChunkFromServerTask(dimension, packet.chunk, packet.teamId, now));
	}

	@Override
	public void updateAllChunks(SendManyChunksPacket packet) {
		if (MapManager.inst == null) {
			return;
		}

		MapDimension dimension = MapManager.inst.getDimension(packet.dimension);
		Date now = new Date();

		for (SendChunkPacket.SingleChunk c : packet.chunks) {
			queue(new UpdateChunkFromServerTask(dimension, c, packet.teamId, now));
		}
	}

	@Override
	public void updateVisiblePlayerList(SendVisiblePlayerListPacket packet) {
		PlayerLocation.CLIENT_LIST.clear();
		PlayerLocation.currentDimension = packet.dim;
		PlayerLocation.CLIENT_LIST.addAll(packet.players);
	}

	@Override
	public void syncRegion(RegionSyncKey key, int offset, int total, byte[] data) {
		PartialPackets.REGION.read(key, offset, total, data);
	}

	@Override
	public void playerDeath(PlayerDeathPacket packet) {
		if (FTBChunksClientConfig.DEATH_WAYPOINTS.get()) {
			MapDimension dimension = MapManager.inst.getDimension(packet.dimension);

			for (Waypoint w : dimension.getWaypoints()) {
				if (w.type == WaypointType.DEATH) {
					w.hidden = true;
				}
			}

			Waypoint w = new Waypoint(dimension);
			w.name = "Death #" + packet.number;
			w.x = packet.x;
			w.y = packet.y;
			w.z = packet.z;
			w.type = WaypointType.DEATH;
			w.color = 0xFF0000;
			dimension.getWaypoints().add(w);
			dimension.saveData = true;
		}
	}

	@Override
	public int blockColor() {
		Minecraft mc = Minecraft.getInstance();
		mc.submit(() -> {
			if (mc.hitResult instanceof BlockHitResult && mc.options.hideGui) {
				ResourceLocation id = Registry.BLOCK.getKey(mc.level.getBlockState(((BlockHitResult) mc.hitResult).getBlockPos()).getBlock());
				NativeImage image = Screenshot.takeScreenshot(mc.getWindow().getWidth(), mc.getWindow().getHeight(), mc.getMainRenderTarget());
				int col = image.getPixelRGBA(image.getWidth() / 2, image.getHeight() / 2);
				String s = String.format("\"%s\": \"#%06X\"", id.getPath(), ColorUtils.convertFromNative(col) & 0xFFFFFF);
				mc.player.sendMessage(new TextComponent(id.getNamespace() + " - " + s).withStyle(Style.EMPTY.applyFormat(ChatFormatting.GOLD).withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, s)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Click to copy")))), Util.NIL_UUID);
			} else {
				mc.player.sendMessage(new TextComponent("You must be looking at a block in F1 mode!"), Util.NIL_UUID);
			}
		});

		return 1;
	}

	public InteractionResult customClick(CustomClickEvent event) {
		if (event.getId().equals(BUTTON_ID)) {
			openGui();
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	public InteractionResult keyPressed(Minecraft client, int keyCode, int scanCode, int action, int modifiers) {
		if (openMapKey.isDown()) {
			if (Screen.hasControlDown()) {
				SNBTCompoundTag tag = new SNBTCompoundTag();
				tag.putBoolean(FTBChunksClientConfig.MINIMAP_ENABLED.key, !FTBChunksClientConfig.MINIMAP_ENABLED.get());
				FTBChunksClientConfig.MINIMAP_ENABLED.read(tag);
				FTBChunksClientConfig.saveConfig();
			} else if (FTBChunksClientConfig.DEBUG_INFO.get() && Screen.hasAltDown()) {
				FTBChunks.LOGGER.info("=== Task Queue: " + taskQueue.size());

				for (MapTask task : taskQueue) {
					FTBChunks.LOGGER.info(task.toString());
				}

				FTBChunks.LOGGER.info("===");
			} else {
				openGui();
				return InteractionResult.SUCCESS;
			}
		} else if (zoomInKey.isDown()) {
			return changeZoom(true);
		} else if (zoomOutKey.isDown()) {
			return changeZoom(false);
		}

		return InteractionResult.PASS;
	}

	public InteractionResult keyPressed(Minecraft client, Screen screen, int keyCode, int scanCode, int modifiers) {
		if (openMapKey.isDown()) {
			LargeMapScreen gui = ClientUtils.getCurrentGuiAs(LargeMapScreen.class);

			if (gui != null) {
				gui.closeGui(false);
				return InteractionResult.SUCCESS;
			}
		}

		return InteractionResult.PASS;
	}

	private InteractionResult changeZoom(boolean zoomIn) {
		prevZoom = FTBChunksClientConfig.MINIMAP_ZOOM.get();
		double zoom = prevZoom;
		double zoomFactor = zoomIn ? 1D : -1D;

		if (zoom + zoomFactor > 4D) {
			zoom = 4D;
		} else if (zoom + zoomFactor < 1D) {
			zoom = 1D;
		} else {
			zoom += zoomFactor;
		}

		lastZoomTime = System.currentTimeMillis();
		FTBChunksClientConfig.MINIMAP_ZOOM.set(zoom);
		return InteractionResult.SUCCESS;
	}

	public float getZoom() {
		double z = FTBChunksClientConfig.MINIMAP_ZOOM.get();

		if (prevZoom != z) {
			long max = (long) (400D / z);
			long t = Mth.clamp(System.currentTimeMillis() - lastZoomTime, 0L, max);

			if (t == max) {
				lastZoomTime = 0L;
				return (float) z;
			}

			return (float) Mth.lerp(t / (double) max, prevZoom, z);
		}

		return (float) z;
	}

	public void renderHud(PoseStack matrixStack, float tickDelta) {
		Minecraft mc = Minecraft.getInstance();

		if (mc.player == null || mc.level == null || MapManager.inst == null) {
			return;
		}

		MapDimension dim = MapDimension.getCurrent();

		if (dim.dimension != mc.level.dimension()) {
			MapDimension.updateCurrent();
			dim = MapDimension.getCurrent();
		}

		long now = System.currentTimeMillis();

		if (nextRegionSave == 0L || now >= nextRegionSave) {
			nextRegionSave = now + 60000L;
			saveAllRegions();
		}

		if (minimapTextureId == -1) {
			minimapTextureId = TextureUtil.generateTextureId();
			TextureUtil.prepareImage(minimapTextureId, FTBChunks.MINIMAP_SIZE, FTBChunks.MINIMAP_SIZE);
			updateMinimap = true;
		}

		RenderSystem.enableTexture();
		RenderSystem.bindTexture(minimapTextureId);

		boolean minimapBlur = FTBChunksClientConfig.MINIMAP_BLUR.get();

		if (minimapBlur) {
			RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
			RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		} else {
			RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
			RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		}

		int cx = mc.player.xChunk;
		int cz = mc.player.zChunk;

		if (cx != currentPlayerChunkX || cz != currentPlayerChunkZ) {
			updateMinimap = true;
		}

		if (updateMinimap) {
			updateMinimap = false;

			// TODO: More math here to upload from (up to) 4 regions instead of all chunks inside them, to speed things up

			for (int mz = 0; mz < FTBChunks.TILES; mz++) {
				for (int mx = 0; mx < FTBChunks.TILES; mx++) {
					int ox = cx + mx - FTBChunks.TILE_OFFSET;
					int oz = cz + mz - FTBChunks.TILE_OFFSET;

					MapRegion region = dim.getRegion(XZ.regionFromChunk(ox, oz));
					region.getRenderedMapImage().upload(0, mx * 16, mz * 16, (ox & 31) * 16, (oz & 31) * 16, 16, 16, minimapBlur, false, false, false);
				}
			}

			currentPlayerChunkX = cx;
			currentPlayerChunkZ = cz;
		}

		if (mc.options.renderDebug || !FTBChunksClientConfig.MINIMAP_ENABLED.get() || FTBChunksClientConfig.MINIMAP_VISIBILITY.get() == 0 || FTBChunksWorldConfig.FORCE_DISABLE_MINIMAP.get()) {
			return;
		}

		float zoom = getZoom();
		float scale = (float) (FTBChunksClientConfig.MINIMAP_SCALE.get() * 4D / mc.getWindow().getGuiScale());
		float minimapRotation = (FTBChunksClientConfig.MINIMAP_LOCKED_NORTH.get() ? 180F : -mc.player.yRot) % 360F;

		int s = (int) (64D * scale);
		int x = FTBChunksClientConfig.MINIMAP_POSITION.get().getX(mc.getWindow().getGuiScaledWidth(), s);
		int y = FTBChunksClientConfig.MINIMAP_POSITION.get().getY(mc.getWindow().getGuiScaledHeight(), s);
		int z = 0;

		float border = 0F;
		int alpha = FTBChunksClientConfig.MINIMAP_VISIBILITY.get();

		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder buffer = tessellator.getBuilder();

		float f0 = 1F / (float) FTBChunks.TILES;
		float f1 = 1F - f0;

		float offX = (float) ((mc.player.getX() / 16D - currentPlayerChunkX - 0.5D) / (double) FTBChunks.TILES);
		float offY = (float) ((mc.player.getZ() / 16D - currentPlayerChunkZ - 0.5D) / (double) FTBChunks.TILES);

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableCull();
		RenderSystem.enableTexture();
		RenderSystem.enableDepthTest();
		RenderSystem.enableAlphaTest();

		matrixStack.pushPose();
		matrixStack.translate(x + s / 2D, y + s / 2D, -10);

		matrixStack.translate(0, 0, 500);

		Matrix4f m = matrixStack.last().pose();

		// See AdvancementTabGui
		RenderSystem.colorMask(false, false, false, false);
		mc.getTextureManager().bind(CIRCLE_MASK);
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		buffer.vertex(m, -s / 2F + border, -s / 2F + border, z).color(255, 255, 255, 255).uv(0F, 0F).endVertex();
		buffer.vertex(m, -s / 2F + border, s / 2F - border, z).color(255, 255, 255, 255).uv(0F, 1F).endVertex();
		buffer.vertex(m, s / 2F - border, s / 2F - border, z).color(255, 255, 255, 255).uv(1F, 1F).endVertex();
		buffer.vertex(m, s / 2F - border, -s / 2F + border, z).color(255, 255, 255, 255).uv(1F, 0F).endVertex();
		tessellator.end();
		RenderSystem.colorMask(true, true, true, true);

		matrixStack.mulPose(Vector3f.ZP.rotationDegrees(minimapRotation + 180F));

		RenderSystem.depthFunc(GL11.GL_GEQUAL);
		RenderSystem.bindTexture(minimapTextureId);
		matrixStack.scale(zoom, zoom, 1);

		m = matrixStack.last().pose();

		buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		buffer.vertex(m, -s / 2F + border, -s / 2F + border, z).color(255, 255, 255, alpha).uv(f0 + offX, f0 + offY).endVertex();
		buffer.vertex(m, -s / 2F + border, s / 2F - border, z).color(255, 255, 255, alpha).uv(f0 + offX, f1 + offY).endVertex();
		buffer.vertex(m, s / 2F - border, s / 2F - border, z).color(255, 255, 255, alpha).uv(f1 + offX, f1 + offY).endVertex();
		buffer.vertex(m, s / 2F - border, -s / 2F + border, z).color(255, 255, 255, alpha).uv(f1 + offX, f0 + offY).endVertex();
		tessellator.end();

		matrixStack.popPose();

		RenderSystem.disableDepthTest();
		RenderSystem.depthFunc(GL11.GL_LEQUAL);
		RenderSystem.defaultBlendFunc();

		m = matrixStack.last().pose();

		mc.getTextureManager().bind(CIRCLE_BORDER);
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		buffer.vertex(m, x, y, z).color(255, 255, 255, alpha).uv(0F, 0F).endVertex();
		buffer.vertex(m, x, y + s, z).color(255, 255, 255, alpha).uv(0F, 1F).endVertex();
		buffer.vertex(m, x + s, y + s, z).color(255, 255, 255, alpha).uv(1F, 1F).endVertex();
		buffer.vertex(m, x + s, y, z).color(255, 255, 255, alpha).uv(1F, 0F).endVertex();
		tessellator.end();

		RenderSystem.disableTexture();
		buffer.begin(GL11.GL_LINES, DefaultVertexFormat.POSITION_COLOR);
		buffer.vertex(m, x + s / 2F, y + 0, z).color(0, 0, 0, 30).endVertex();
		buffer.vertex(m, x + s / 2F, y + s, z).color(0, 0, 0, 30).endVertex();
		buffer.vertex(m, x + 0, y + s / 2F, z).color(0, 0, 0, 30).endVertex();
		buffer.vertex(m, x + s, y + s / 2F, z).color(0, 0, 0, 30).endVertex();
		tessellator.end();

		RenderSystem.enableTexture();

		if (FTBChunksClientConfig.MINIMAP_COMPASS.get()) {
			for (int face = 0; face < 4; face++) {
				double d = s / 2.2D;

				double angle = (minimapRotation + 180D - face * 90D) * Math.PI / 180D;

				float wx = (float) (x + s / 2D + Math.cos(angle) * d);
				float wy = (float) (y + s / 2D + Math.sin(angle) * d);
				float ws = s / 32F;

				m = matrixStack.last().pose();

				mc.getTextureManager().bind(COMPASS[face]);
				buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
				buffer.vertex(m, wx - ws, wy - ws, z).color(255, 255, 255, 255).uv(0F, 0F).endVertex();
				buffer.vertex(m, wx - ws, wy + ws, z).color(255, 255, 255, 255).uv(0F, 1F).endVertex();
				buffer.vertex(m, wx + ws, wy + ws, z).color(255, 255, 255, 255).uv(1F, 1F).endVertex();
				buffer.vertex(m, wx + ws, wy - ws, z).color(255, 255, 255, 255).uv(1F, 0F).endVertex();
				tessellator.end();
			}
		}

		double magicNumber = 3.2D;

		if (FTBChunksClientConfig.MINIMAP_WAYPOINTS.get() && !dim.getWaypoints().isEmpty()) {
			for (Waypoint waypoint : dim.getWaypoints()) {
				if (waypoint.hidden) {
					continue;
				}

				double distance = MathUtils.dist(mc.player.getX(), mc.player.getZ(), waypoint.x + 0.5D, waypoint.z + 0.5D);

				if (distance > waypoint.minimapDistance) {
					continue;
				}

				double d = distance / magicNumber * scale * zoom;

				if (d > s / 2D) {
					d = s / 2D;
				}

				double angle = Math.atan2(mc.player.getZ() - waypoint.z - 0.5D, mc.player.getX() - waypoint.x - 0.5D) + minimapRotation * Math.PI / 180D;

				float wx = (float) (x + s / 2D + Math.cos(angle) * d);
				float wy = (float) (y + s / 2D + Math.sin(angle) * d);
				float ws = s / 32F;

				int r = (waypoint.color >> 16) & 0xFF;
				int g = (waypoint.color >> 8) & 0xFF;
				int b = (waypoint.color >> 0) & 0xFF;

				m = matrixStack.last().pose();

				mc.getTextureManager().bind(waypoint.type.texture);
				buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
				buffer.vertex(m, wx - ws, wy - ws, z).color(r, g, b, 255).uv(0F, 0F).endVertex();
				buffer.vertex(m, wx - ws, wy + ws, z).color(r, g, b, 255).uv(0F, 1F).endVertex();
				buffer.vertex(m, wx + ws, wy + ws, z).color(r, g, b, 255).uv(1F, 1F).endVertex();
				buffer.vertex(m, wx + ws, wy - ws, z).color(r, g, b, 255).uv(1F, 0F).endVertex();
				tessellator.end();
			}
		}

		renderMinimap(dim, (px, pz, color, maxDistance) -> {
			double distance = MathUtils.dist(mc.player.getX(), mc.player.getZ(), px + 0.5D, pz + 0.5D);

			double d = distance / magicNumber * scale * zoom;
			if (maxDistance > 0 && distance > maxDistance) {
				return;
			}

			if (d > s / 2D) {
				if (maxDistance == 0) {
					return;
				}
				d = s / 2D;
			}

			double angle = Math.atan2(mc.player.getZ() - pz - 0.5D, mc.player.getX() - px - 0.5D) + minimapRotation * Math.PI / 180D;

			float wx = (float) (x + s / 2D + Math.cos(angle) * d);
			float wy = (float) (y + s / 2D + Math.sin(angle) * d);
			float ws = s / 32F;

			int r = (color >> 16) & 0xFF;
			int g = (color >> 8) & 0xFF;
			int b = (color >> 0) & 0xFF;

			Matrix4f matrix = matrixStack.last().pose();

			mc.getTextureManager().bind(WaypointType.WAYSTONE.texture);
			buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
			buffer.vertex(matrix, wx - ws, wy - ws, 0).color(r, g, b, 255).uv(0F, 0F).endVertex();
			buffer.vertex(matrix, wx - ws, wy + ws, 0).color(r, g, b, 255).uv(0F, 1F).endVertex();
			buffer.vertex(matrix, wx + ws, wy + ws, 0).color(r, g, b, 255).uv(1F, 1F).endVertex();
			buffer.vertex(matrix, wx + ws, wy - ws, 0).color(r, g, b, 255).uv(1F, 0F).endVertex();
			tessellator.end();
		});

		if (FTBChunksClientConfig.MINIMAP_ENTITIES.get()) {
			for (Entity entity : mc.level.entitiesForRendering()) {
				if (entity instanceof AbstractClientPlayer || entity.getType().getCategory() == MobCategory.MISC || entity.getY() < entity.level.getHeight(Heightmap.Types.WORLD_SURFACE, Mth.floor(entity.getX()), Mth.floor(entity.getZ())) - 10) {
					continue;
				}

				double d = MathUtils.dist(mc.player.getX(), mc.player.getZ(), entity.getX(), entity.getZ()) / magicNumber * scale * zoom;
				if (d > s / 2D) {
					continue;
				}

				ResourceLocation texture = EntityIcons.ENTITY_ICONS.get(entity.getType());

				if (texture == EntityIcons.INVISIBLE) {
					continue;
				} else if (texture == null || !FTBChunksClientConfig.MINIMAP_ENTITY_HEADS.get()) {
					if (entity instanceof Enemy) {
						texture = EntityIcons.HOSTILE;
					} else {
						texture = EntityIcons.NORMAL;
					}
				}

				double angle = Math.atan2(mc.player.getZ() - entity.getZ(), mc.player.getX() - entity.getX()) + minimapRotation * Math.PI / 180D;

				float wx = (float) (x + s / 2D + Math.cos(angle) * d);
				float wy = (float) (y + s / 2D + Math.sin(angle) * d);
				float ws = s / (FTBChunksClientConfig.MINIMAP_LARGE_ENTITIES.get() ? 32F : 48F);

				m = matrixStack.last().pose();

				mc.getTextureManager().bind(texture);
				RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
				RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

				buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
				buffer.vertex(m, wx - ws, wy - ws, z).color(255, 255, 255, 255).uv(0F, 0F).endVertex();
				buffer.vertex(m, wx - ws, wy + ws, z).color(255, 255, 255, 255).uv(0F, 1F).endVertex();
				buffer.vertex(m, wx + ws, wy + ws, z).color(255, 255, 255, 255).uv(1F, 1F).endVertex();
				buffer.vertex(m, wx + ws, wy - ws, z).color(255, 255, 255, 255).uv(1F, 0F).endVertex();
				tessellator.end();
			}
		}

		if (FTBChunksClientConfig.MINIMAP_PLAYER_HEADS.get() && mc.level.players().size() > 1) {
			for (AbstractClientPlayer player : mc.level.players()) {
				if (player == mc.player || player.isInvisibleTo(mc.player)) {
					continue;
				}

				double d = MathUtils.dist(mc.player.getX(), mc.player.getZ(), player.getX(), player.getZ()) / magicNumber * scale * zoom;

				if (d > s / 2D) {
					d = s / 2D;
				}

				double angle = Math.atan2(mc.player.getZ() - player.getZ(), mc.player.getX() - player.getX()) + minimapRotation * Math.PI / 180D;
				float wx = (float) (x + s / 2D + Math.cos(angle) * d);
				float wy = (float) (y + s / 2D + Math.sin(angle) * d);
				float ws = s / 32F;

				matrixStack.pushPose();
				matrixStack.translate(wx, wy, z);
				matrixStack.scale(ws, ws, 1F);
				FaceIcon.getFace(player.getGameProfile()).draw(matrixStack, -1, -1, 2, 2);
				matrixStack.popPose();
			}
		}

		if (FTBChunksClientConfig.MINIMAP_LOCKED_NORTH.get()) {
			mc.getTextureManager().bind(PLAYER);
			matrixStack.pushPose();
			matrixStack.translate(x + s / 2D, y + s / 2D, z);
			matrixStack.mulPose(Vector3f.ZP.rotationDegrees(mc.player.yRot + 180F));
			matrixStack.scale(s / 16F, s / 16F, 1F);
			m = matrixStack.last().pose();

			buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
			buffer.vertex(m, -1, -1, 0).color(255, 255, 255, 200).uv(0F, 0F).endVertex();
			buffer.vertex(m, -1, 1, 0).color(255, 255, 255, 200).uv(0F, 1F).endVertex();
			buffer.vertex(m, 1, 1, 0).color(255, 255, 255, 200).uv(1F, 1F).endVertex();
			buffer.vertex(m, 1, -1, 0).color(255, 255, 255, 200).uv(1F, 0F).endVertex();
			tessellator.end();

			matrixStack.popPose();
		}

		MINIMAP_TEXT_LIST.clear();

		if (FTBChunksClientConfig.MINIMAP_ZONE.get()) {
			MapRegionData data = dim.getRegion(XZ.regionFromChunk(currentPlayerChunkX, currentPlayerChunkZ)).getData();

			if (data != null) {
				ClientTeam team = data.getChunk(XZ.of(currentPlayerChunkX, currentPlayerChunkZ)).getTeam();

				if (team != null) {
					MINIMAP_TEXT_LIST.add(team.getColoredName());
				}
			}
		}

		if (FTBChunksClientConfig.MINIMAP_XYZ.get()) {
			MINIMAP_TEXT_LIST.add(new TextComponent(Mth.floor(mc.player.getX()) + " " + Mth.floor(mc.player.getY()) + " " + Mth.floor(mc.player.getZ())));
		}

		if (FTBChunksClientConfig.MINIMAP_BIOME.get()) {
			ResourceKey<Biome> biome = mc.level.getBiomeName(mc.player.blockPosition()).orElse(null);

			if (biome != null) {
				MINIMAP_TEXT_LIST.add(new TranslatableComponent("biome." + biome.location().getNamespace() + "." + biome.location().getPath()));
			}
		}

		if (FTBChunksClientConfig.DEBUG_INFO.get()) {
			XZ r = XZ.regionFromChunk(currentPlayerChunkX, currentPlayerChunkZ);
			MINIMAP_TEXT_LIST.add(new TextComponent("Queued tasks: " + taskQueue.size()));
			MINIMAP_TEXT_LIST.add(new TextComponent(r.toRegionString()));
			MINIMAP_TEXT_LIST.add(new TextComponent("Total updates: " + renderedDebugCount));
		}

		if (!MINIMAP_TEXT_LIST.isEmpty()) {
			matrixStack.pushPose();
			matrixStack.translate(x + s / 2D, y + s + 3D, 0D);
			matrixStack.scale((float) (0.5D * scale), (float) (0.5D * scale), 1F);

			for (int i = 0; i < MINIMAP_TEXT_LIST.size(); i++) {
				FormattedCharSequence bs = MINIMAP_TEXT_LIST.get(i).getVisualOrderText();
				int bsw = mc.font.width(bs);
				mc.font.drawShadow(matrixStack, bs, -bsw / 2F, i * 11, 0xFFFFFFFF);
			}

			matrixStack.popPose();
		}

		RenderSystem.enableDepthTest();
	}

	public void renderWorldLast(PoseStack ms) {
		Minecraft mc = Minecraft.getInstance();

		if (mc.options.hideGui || !FTBChunksClientConfig.IN_WORLD_WAYPOINTS.get() || MapManager.inst == null || mc.level == null || mc.player == null) {
			return;
		}

		MapDimension dim = MapDimension.getCurrent();

		if (dim.getWaypoints().isEmpty()) {
			return;
		}

		List<Waypoint> visibleWaypoints = new ArrayList<>();

		for (Waypoint waypoint : dim.getWaypoints()) {
			if (waypoint.hidden) {
				continue;
			}

			waypoint.distance = MathUtils.dist(mc.player.getX(), mc.player.getZ(), waypoint.x + 0.5D, waypoint.z + 0.5D);

			if (waypoint.distance <= 8D || waypoint.distance > waypoint.inWorldDistance) {
				continue;
			}

			waypoint.alpha = 150;

			if (waypoint.distance < 12D) {
				waypoint.alpha = (int) (waypoint.alpha * ((waypoint.distance - 8D) / 4D));
			}

			if (waypoint.alpha <= 0) {
				continue;
			}

			visibleWaypoints.add(waypoint);
		}

		if (visibleWaypoints.isEmpty()) {
			return;
		}

		if (visibleWaypoints.size() >= 2) {
			visibleWaypoints.sort(Comparator.comparingDouble(value -> -value.distance));
		}

		Camera camera = Minecraft.getInstance().getEntityRenderDispatcher().camera;
		Vec3 cameraPos = camera.getPosition();
		ms.pushPose();
		ms.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

		VertexConsumer depthBuffer = mc.renderBuffers().bufferSource().getBuffer(FTBChunksRenderTypes.WAYPOINTS_DEPTH);

		float h = (float) (cameraPos.y + 30D);
		float h2 = h + 70F;

		for (Waypoint waypoint : visibleWaypoints) {
			double angle = Math.atan2(cameraPos.z - waypoint.z - 0.5D, cameraPos.x - waypoint.x - 0.5D) * 180D / Math.PI;

			int r = (waypoint.color >> 16) & 0xFF;
			int g = (waypoint.color >> 8) & 0xFF;
			int b = (waypoint.color >> 0) & 0xFF;

			ms.pushPose();
			ms.translate(waypoint.x + 0.5D, 0, waypoint.z + 0.5D);
			ms.mulPose(Vector3f.YP.rotationDegrees((float) (-angle - 135D)));

			float s = 0.6F;

			Matrix4f m = ms.last().pose();

			depthBuffer.vertex(m, -s, 0, s).color(r, g, b, waypoint.alpha).uv(0F, 1F).endVertex();
			depthBuffer.vertex(m, -s, h, s).color(r, g, b, waypoint.alpha).uv(0F, 0F).endVertex();
			depthBuffer.vertex(m, s, h, -s).color(r, g, b, waypoint.alpha).uv(1F, 0F).endVertex();
			depthBuffer.vertex(m, s, 0, -s).color(r, g, b, waypoint.alpha).uv(1F, 1F).endVertex();

			depthBuffer.vertex(m, -s, h, s).color(r, g, b, waypoint.alpha).uv(0F, 1F).endVertex();
			depthBuffer.vertex(m, -s, h2, s).color(r, g, b, 0).uv(0F, 0F).endVertex();
			depthBuffer.vertex(m, s, h2, -s).color(r, g, b, 0).uv(1F, 0F).endVertex();
			depthBuffer.vertex(m, s, h, -s).color(r, g, b, waypoint.alpha).uv(1F, 1F).endVertex();

			ms.popPose();
		}

		ms.popPose();

		mc.renderBuffers().bufferSource().endBatch(FTBChunksRenderTypes.WAYPOINTS_DEPTH);
	}

	public InteractionResult screenOpened(Screen screen, List<AbstractWidget> widgets, List<GuiEventListener> children) {
		if (screen instanceof PauseScreen) {
			nextRegionSave = System.currentTimeMillis() + 60000L;
			saveAllRegions();
		}

		return InteractionResult.PASS;
	}

	public void clientTick(Minecraft client) {
		MapManager manager = MapManager.inst;

		if (manager != null && Minecraft.getInstance().level != null) {
			if (taskQueueTicks % FTBChunksClientConfig.RERENDER_QUEUE_TICKS.get() == 0L) {
				if (!rerenderCache.isEmpty()) {
					Level level = Minecraft.getInstance().level;

					for (Map.Entry<ChunkPos, IntOpenHashSet> pos : rerenderCache.entrySet()) {
						ChunkAccess chunkAccess = level.getChunk(pos.getKey().x, pos.getKey().z, ChunkStatus.FULL, false);

						if (chunkAccess != null) {
							queue(new ReloadChunkTask(level, chunkAccess, pos.getKey(), pos.getValue()));
						}
					}

					rerenderCache = new HashMap<>();
				}
			}

			if (taskQueueTicks % FTBChunksClientConfig.TASK_QUEUE_TICKS.get() == 0L) {
				int s = Math.min(taskQueue.size(), FTBChunksClientConfig.TASK_QUEUE_MAX.get());

				if (s > 0) {
					MapTask[] tasks = new MapTask[s];

					for (int i = 0; i < s; i++) {
						tasks[i] = taskQueue.pollFirst();

						if (tasks[i] == null || tasks[i].cancelOtherTasks()) {
							break;
						}
					}

					for (MapTask task : tasks) {
						if (task != null) {
							task.runMapTask(manager);
						}
					}
				}
			}

			taskQueueTicks++;
		}
	}

	public void teamPropertiesChanged(ClientTeamPropertiesChangedEvent event) {
		if (MapManager.inst != null) {
			MapManager.inst.updateAllRegions(false);
		}
	}

	public static void rerender(MapChunk chunk, BlockPos pos, BlockState state) {
		ChunkPos chunkPos = new ChunkPos(pos);
		IntOpenHashSet set = rerenderCache.get(chunkPos);

		if (set == null) {
			set = new IntOpenHashSet();
			rerenderCache.put(chunkPos, set);
		}

		if (set.add((pos.getX() & 15) + ((pos.getZ() & 15) * 16))) {
			if (FTBChunksClientConfig.DEBUG_INFO.get()) {
				renderedDebugCount++;
			}
		}
	}

	public static void handlePacket(ClientboundSectionBlocksUpdatePacketFTBC p) {
		if (MapManager.inst == null) {
			return;
		}

		SectionPos sectionPos = p.getSectionPosFTBC();
		MapChunk chunk = MapDimension.getCurrent().getRegion(XZ.regionFromChunk(sectionPos.chunk())).getDataBlocking().getChunk(XZ.of(sectionPos.chunk()));

		short[] positions = p.getPositionsFTBC();
		BlockState[] states = p.getStatesFTBC();

		for (int i = 0; i < positions.length; ++i) {
			rerender(chunk, sectionPos.relativeToBlockPos(positions[i]), states[i]);
		}
	}

	public static void handlePacket(ClientboundLevelChunkPacket p) {
		Level level = Minecraft.getInstance().level;

		if (level != null && p.isFullChunk()) {
			ChunkAccess chunkAccess = level.getChunk(p.getX(), p.getZ(), ChunkStatus.FULL, false);

			if (chunkAccess != null) {
				queue(new ReloadChunkFromLevelPacketTask(level, chunkAccess, p));
			}
		}
	}

	public static void handlePacket(ClientboundBlockUpdatePacket p) {
		if (MapManager.inst == null) {
			return;
		}

		MapChunk chunk = MapDimension.getCurrent().getRegion(XZ.regionFromBlock(p.getPos())).getDataBlocking().getChunk(XZ.chunkFromBlock(p.getPos()));
		rerender(chunk, p.getPos(), p.getBlockState());
	}
}
