package semmiedev.disc_jockey;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.enums.Instrument;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import semmiedev.disc_jockey.gui.screen.DiscJockeyScreen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class DiscjockeyCommand {


    public static void register(CommandDispatcher<FabricClientCommandSource> commandDispatcher) {
        final ArrayList<String> instrumentNames = new ArrayList<>();
        for (Instrument instrument : Instrument.values()) {
            instrumentNames.add(instrument.toString().toLowerCase());
        }
        final ArrayList<String> instrumentNamesAndAll = new ArrayList<>(instrumentNames);
        instrumentNamesAndAll.add("all");

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
                        .then(literal("remapInstruments")
                                .executes(context -> {
                                    context.getSource().sendFeedback(Text.translatable(Main.MOD_ID + ".instrument_info"));
                                    return 0;
                                })
                                .then(literal("map")
                                        .then(argument("originalInstrument", StringArgumentType.word())
                                                .suggests((context, builder) -> CommandSource.suggestMatching(instrumentNamesAndAll, builder))
                                                .then(argument("newInstrument", StringArgumentType.word())
                                                        .suggests((context, builder) -> CommandSource.suggestMatching(instrumentNames, builder))
                                                        .executes(context -> {
                                                            String originalInstrumentStr = StringArgumentType.getString(context, "originalInstrument");
                                                            String newInstrumentStr = StringArgumentType.getString(context, "newInstrument");
                                                            Instrument originalInstrument = null, newInstrument = null;
                                                            for(Instrument maybeInstrument : Instrument.values()) {
                                                                if(maybeInstrument.toString().equalsIgnoreCase(originalInstrumentStr)) {
                                                                    originalInstrument = maybeInstrument;
                                                                }
                                                                if(maybeInstrument.toString().equalsIgnoreCase(newInstrumentStr)) {
                                                                    newInstrument = maybeInstrument;
                                                                }
                                                            }

                                                            if(originalInstrument == null && !originalInstrumentStr.equalsIgnoreCase("all")) {
                                                                context.getSource().sendFeedback(Text.translatable(Main.MOD_ID + ".invalid_instrument", originalInstrumentStr));
                                                                return 0;
                                                            }

                                                            if(newInstrument == null) {
                                                                context.getSource().sendFeedback(Text.translatable(Main.MOD_ID + ".invalid_instrument", newInstrumentStr));
                                                                return 0;
                                                            }

                                                            if(originalInstrument == null) {
                                                                // All instruments
                                                                for(Instrument instrument : Instrument.values()) {
                                                                    Main.SONG_PLAYER.instrumentMap.put(instrument, newInstrument);
                                                                }
                                                                context.getSource().sendFeedback(Text.translatable(Main.MOD_ID + ".instrument_mapped_all", newInstrumentStr.toLowerCase()));
                                                            }else {
                                                                Main.SONG_PLAYER.instrumentMap.put(originalInstrument, newInstrument);
                                                                context.getSource().sendFeedback(Text.translatable(Main.MOD_ID + ".instrument_mapped", originalInstrumentStr.toLowerCase(), newInstrumentStr.toLowerCase()));
                                                            }
                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                                .then(literal("unmap")
                                        .then(argument("instrument", StringArgumentType.word())
                                                .suggests((context, builder) -> CommandSource.suggestMatching(instrumentNames, builder))
                                                .executes(context -> {
                                                    String instrumentStr = StringArgumentType.getString(context, "instrument");

                                                    Instrument instrument = null;
                                                    for(Instrument maybeInstrument : Instrument.values()) {
                                                        if(maybeInstrument.toString().equalsIgnoreCase(instrumentStr)) {
                                                            instrument = maybeInstrument;
                                                            break;
                                                        }
                                                    }

                                                    if(instrument == null) {
                                                        context.getSource().sendFeedback(Text.translatable(Main.MOD_ID + ".invalid_instrument", instrumentStr));
                                                        return 0;
                                                    }

                                                    Main.SONG_PLAYER.instrumentMap.remove(instrument);
                                                    context.getSource().sendFeedback(Text.translatable(Main.MOD_ID + ".instrument_unmapped", instrumentStr.toLowerCase()));
                                                    return 1;
                                                })
                                        )
                                )
                                .then(literal("show")
                                        .executes(context -> {
                                            if(Main.SONG_PLAYER.instrumentMap.isEmpty()) {
                                                context.getSource().sendFeedback(Text.translatable(Main.MOD_ID + ".no_mapped_instruments"));
                                                return 1;
                                            }

                                            StringBuilder maps = new StringBuilder();
                                            for(Map.Entry<Instrument, Instrument> entry : Main.SONG_PLAYER.instrumentMap.entrySet()) {
                                                if(maps.length() > 0) {
                                                    maps.append(", ");
                                                }
                                                maps
                                                        .append(entry.getKey().toString().toLowerCase())
                                                        .append(" -> ")
                                                        .append(entry.getValue().toString().toLowerCase());
                                            }
                                            context.getSource().sendFeedback(Text.translatable(Main.MOD_ID + ".mapped_instruments", maps.toString()));
                                            return 1;
                                        })
                                )
                                .then(literal("clear")
                                        .executes(context -> {
                                            Main.SONG_PLAYER.instrumentMap.clear();
                                            context.getSource().sendFeedback(Text.translatable(Main.MOD_ID + ".instrument_maps_cleared"));
                                            return 1;
                                        })
                                )
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
