package semmiedev.disc_jockey;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import semmiedev.disc_jockey.gui.screen.DiscJockeyScreen;

import java.util.Arrays;
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
                        .then(literal("speed")
                                .then(argument("speed", FloatArgumentType.floatArg(0.0001F, 15.0F))
                                        .suggests((context, builder) -> CommandSource.suggestMatching(Arrays.asList("0.5", "0.75", "1", "1.25", "1.5", "2"), builder))
                                        .executes(context -> {
                                            float newSpeed = FloatArgumentType.getFloat(context, "speed");
                                            Main.SONG_PLAYER.speed = newSpeed;
                                            context.getSource().sendFeedback(Text.translatable(Main.MOD_ID + ".speed_changed", Main.SONG_PLAYER.speed));
                                            return 0;
                                        })
                                )
                        )
                        .then(literal("info")
                                .executes(context -> {

                                    if (!Main.SONG_PLAYER.running) {
                                        context.getSource().sendFeedback(Text.translatable(Main.MOD_ID + ".info_not_running", Main.SONG_PLAYER.speed));
                                        return 0;
                                    }
                                    if (!Main.SONG_PLAYER.tuned) {
                                        context.getSource().sendFeedback(Text.translatable(Main.MOD_ID + ".info_tuning", Main.SONG_PLAYER.song.displayName, Main.SONG_PLAYER.speed));
                                        return 0;
                                    }else if(!Main.SONG_PLAYER.didSongReachEnd) {
                                        context.getSource().sendFeedback(Text.translatable(Main.MOD_ID + ".info_playing", formatTimestamp((int) Main.SONG_PLAYER.getSongElapsedSeconds()), formatTimestamp((int) Main.SONG_PLAYER.song.getLengthInSeconds()), Main.SONG_PLAYER.song.displayName, Main.SONG_PLAYER.speed));
                                        return 0;
                                    }else {
                                        context.getSource().sendFeedback(Text.translatable(Main.MOD_ID + ".info_finished", Main.SONG_PLAYER.song != null ? Main.SONG_PLAYER.song.displayName : "???", Main.SONG_PLAYER.speed));
                                        return 0;
                                    }
                                })
                        )

        );
    }

    private static boolean isLoading(CommandContext<FabricClientCommandSource> context) {
        if (SongLoader.loadingSongs) {
            context.getSource().sendError(Text.translatable(Main.MOD_ID + ".still_loading"));
            SongLoader.showToast = true;
            return true;
        }
        return false;
    }

    private static String padZeroes(int number, int length) {
        StringBuilder builder = new StringBuilder("" + number);
        while(builder.length() < length)
            builder.insert(0, '0');
        return builder.toString();
    }

    private static String formatTimestamp(int seconds) {
        return padZeroes(seconds / 60, 2) + ":" + padZeroes(seconds % 60, 2);
    }
}
