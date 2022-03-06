package semmieboy_yt.disc_jockey;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import semmieboy_yt.disc_jockey.gui.hud.BlocksOverlay;

import java.io.File;
import java.util.ArrayList;

public class Main implements ClientModInitializer {
    public static final String MOD_ID = "disc_jockey";
    public static final Logger LOGGER = LogManager.getLogger("Disc Jockey");
    public static final ArrayList<ClientTickEvents.StartWorldTick> TICK_LISTENERS = new ArrayList<>();
    public static final Previewer PREVIEWER = new Previewer();
    public static final SongPlayer SONG_PLAYER = new SongPlayer();

    public static File songsFolder;

    @Override
    public void onInitializeClient() {
        songsFolder = new File(FabricLoader.getInstance().getConfigDir()+File.separator+MOD_ID+File.separator+"songs");
        if (!songsFolder.isDirectory()) songsFolder.mkdirs();

        DiscjockeyCommand.register();

        SongLoader.loadSongs();

        ClientTickEvents.START_CLIENT_TICK.register(new ClientTickEvents.StartTick() {
            private ClientWorld prevWorld;

            @Override
            public void onStartTick(MinecraftClient client) {
                if (prevWorld != client.world) {
                    PREVIEWER.stop();
                    SONG_PLAYER.stop();
                }
                prevWorld = client.world;
            }
        });
        ClientTickEvents.START_WORLD_TICK.register(world -> {
            for (ClientTickEvents.StartWorldTick listener : TICK_LISTENERS) listener.onStartTick(world);
        });
        ClientLoginConnectionEvents.DISCONNECT.register((handler, client) -> {
            PREVIEWER.stop();
            SONG_PLAYER.stop();
        });
        HudRenderCallback.EVENT.register(BlocksOverlay::render);
    }
}
