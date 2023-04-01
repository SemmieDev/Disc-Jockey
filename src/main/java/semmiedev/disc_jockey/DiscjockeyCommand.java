package semmiedev.disc_jockey;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import semmiedev.disc_jockey.gui.screen.DiscJockeyScreen;

import java.util.Optional;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class DiscjockeyCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> commandDispatcher) {
        commandDispatcher.register(
                literal("discjockey")
                        .executes(context -> {
                            FabricClientCommandSource source = context.getSource();
                            if (!isLoading(context)) {
                                MinecraftClient client = source.getClient();
                                client.send(() -> client.setScreen(new DiscJockeyScreen()));
                                return 1;
                            }
                            return 0;
                        })
                        .then(literal("reload")
                                .executes(context -> {
                                    if (!isLoading(context)) {
                                        context.getSource().sendFeedback(Text.translatable(Main.MOD_ID+".reloading"));
                                        SongLoader.loadSongs();
                                        return 1;
                                    }
                                    return 0;
                                })
                        )
                        .then(literal("play")
                                .then(argument("song", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> CommandSource.suggestMatching(SongLoader.SONG_SUGGESTIONS, builder))
                                        .executes(context -> {
                                            if (!isLoading(context)) {
                                                String songName = StringArgumentType.getString(context, "song");
                                                Optional<Song> song = SongLoader.SONGS.stream().filter(input -> input.displayName.equals(songName)).findAny();
                                                if (song.isPresent()) {
                                                    Main.SONG_PLAYER.start(song.get());
                                                    return 1;
                                                }
                                                context.getSource().sendError(Text.translatable(Main.MOD_ID+".song_not_found", songName));
                                                return 0;
                                            }
                                            return 0;
                                        })
                                )
                        )
                        .then(literal("stop")
                                .executes(context -> {
                                    if (Main.SONG_PLAYER.running) {
                                        Main.SONG_PLAYER.stop();
                                        context.getSource().sendFeedback(Text.translatable(Main.MOD_ID+".stopped_playing", Main.SONG_PLAYER.song));
                                        return 1;
                                    }
                                    context.getSource().sendError(Text.translatable(Main.MOD_ID+".not_playing"));
                                    return 0;
                                })
                        )
        );
    }

    private static boolean isLoading(CommandContext<FabricClientCommandSource> context) {
        if (SongLoader.loadingSongs) {
            context.getSource().sendError(Text.translatable(Main.MOD_ID+".still_loading"));
            SongLoader.showToast = true;
            return true;
        }
        return false;
    }
}
