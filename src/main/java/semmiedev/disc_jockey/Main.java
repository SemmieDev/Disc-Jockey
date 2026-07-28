package semmiedev.disc_jockey;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import semmiedev.disc_jockey.gui.hud.BlocksOverlay;
import semmiedev.disc_jockey.gui.screen.DiscJockeyScreen;
import com.mojang.blaze3d.platform.InputConstants;
import java.io.File;
import java.util.ArrayList;

public class Main implements ClientModInitializer {
    public static final String MOD_ID = "disc_jockey";
    public static final MutableComponent NAME = Component.literal("Disc Jockey");
    public static final Logger LOGGER = LogManager.getLogger("Disc Jockey");
    public static final ArrayList<ClientTickEvents.StartLevelTick> TICK_LISTENERS = new ArrayList<>();
    public static final Previewer PREVIEWER = new Previewer();
    public static final SongPlayer SONG_PLAYER = new SongPlayer();

    public static File songsFolder;
    public static Config config;
    public static ConfigHolder<Config> configHolder;

    @Override
    public void onInitializeClient() {
        configHolder = AutoConfig.register(Config.class, JanksonConfigSerializer::new);
        config = configHolder.getConfig();

        songsFolder = new File(FabricLoader.getInstance().getConfigDir()+File.separator+MOD_ID+File.separator+"songs");
        if (!songsFolder.isDirectory()) songsFolder.mkdirs();

        SongLoader.loadSongs();

        //KeyBinding openScreenKeyBind = KeyBindingHelper.registerKeyBinding(new KeyBinding(MOD_ID+".key_bind.open_screen", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.category."+MOD_ID));
        // 修复按键绑定
        KeyMapping openScreenKeyBind = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                MOD_ID + ".key_bind.open_screen",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                KeyMapping.Category.MISC
        ));

        ClientTickEvents.START_CLIENT_TICK.register(new ClientTickEvents.StartTick() {
            private ClientLevel prevWorld;

            @Override
            public void onStartTick(Minecraft client) {
                if (prevWorld != client.level) {
                    PREVIEWER.stop();
                    SONG_PLAYER.stop();
                }
                prevWorld = client.level;

                if (openScreenKeyBind.consumeClick()) {
                    if (SongLoader.loadingSongs) {
                        client.gui.hud.getChat().addClientSystemMessage(Component.translatable(Main.MOD_ID+".still_loading").withStyle(ChatFormatting.RED));
                        SongLoader.showToast = true;
                    } else {
                        client.setScreenAndShow(new DiscJockeyScreen());
                    }
                }
            }
        });

        ClientTickEvents.START_LEVEL_TICK.register(world -> {
            for (ClientTickEvents.StartLevelTick listener : TICK_LISTENERS) listener.onStartTick(world);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            DiscjockeyCommand.register(dispatcher);
        });

        ClientLoginConnectionEvents.DISCONNECT.register((handler, client) -> {
            PREVIEWER.stop();
            SONG_PLAYER.stop();
        });

        HudElementRegistry.addLast(net.minecraft.resources.Identifier.fromNamespaceAndPath(Main.MOD_ID, "blocks"), (extractor, ticker) -> BlocksOverlay.render(extractor, ticker));
    }
}
