package semmieboy_yt.disc_jockey;

import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.minecraft.client.MinecraftClient;
import semmieboy_yt.disc_jockey.gui.screen.DiscJockeyScreen;

import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.literal;

public class DiscjockeyCommand {
    public static void register() {
        ClientCommandManager.DISPATCHER.register(
                literal("discjockey")
                        .executes(context -> {
                            MinecraftClient client = context.getSource().getClient();
                            client.send(() -> client.setScreen(new DiscJockeyScreen()));
                            return 1;
                        })
                        .then(literal("reload")
                                .executes(context -> {
                                    SongLoader.loadSongs();
                                    return 1;
                                })
                        )
        );
    }
}
