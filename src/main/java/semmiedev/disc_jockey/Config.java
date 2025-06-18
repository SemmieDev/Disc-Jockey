package semmiedev.disc_jockey;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;

@me.shedaniel.autoconfig.annotation.Config(name = Main.MOD_ID)
@me.shedaniel.autoconfig.annotation.Config.Gui.Background("textures/block/note_block.png")
public class Config implements ConfigData {
    public boolean hideWarning;
    @ConfigEntry.Gui.Tooltip(count = 2) public boolean disableAsyncPlayback;
    @ConfigEntry.Gui.Tooltip(count = 2) public boolean omnidirectionalNoteBlockSounds = true;

    public enum ExpectedServerVersion {
        All,
        v1_20_4_Or_Earlier,
        v1_20_5_Or_Later;

        @Override
        public String toString() {
            return switch (this) {
                case All -> "All (universal)";
                case v1_20_4_Or_Earlier -> "≤1.20.4";
                case v1_20_5_Or_Later -> "≥1.20.5";
            };
        }
    }

    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    @ConfigEntry.Gui.Tooltip(count = 4)
    public ExpectedServerVersion expectedServerVersion = ExpectedServerVersion.All;

    public enum TuningSpeed {
        Snail,
        Safe,
        Spigot,
        Flash;

        @Override
        public String toString() {
            return switch(this) {
                case Snail -> "Snail (10/sec)";
                case Safe -> "Safe (20/sec)";
                case Spigot -> "Spigot (recommended)";
                case Flash -> "Flash";
            };
        }
    }

    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    @ConfigEntry.Gui.Tooltip(count = 7)
    public TuningSpeed tuningSpeed = TuningSpeed.Spigot;

    public enum PlaybackPacketRatelimit {
        Limit100,
        Limit200,
        Limit300,
        Limit500,
        NoLimit;

        @Override
        public String toString() {
            return switch(this) {
                case Limit100 -> "100 Packets/sec";
                case Limit200 -> "200 Packets/sec";
                case Limit300 -> "300 Packets/sec";
                case Limit500 -> "500 Packets/sec";
                case NoLimit -> "No Limit";
            };
        }

        public int getReducePacketsPer100Millis() {
            return switch(this) {
                case Limit100 -> 30 / 10;
                case Limit200 -> 130 / 10;
                case Limit300 -> 200 / 10;
                case Limit500 -> 300 / 10;
                case NoLimit -> Integer.MAX_VALUE;
            };
        }

        public int getMaxPacketsPer100Millis() {
            return switch(this) {
                case Limit100 -> 70 / 10;
                case Limit200 -> 150 / 10;
                case Limit300 -> 250 / 10;
                case Limit500 -> 450 / 10;
                case NoLimit -> Integer.MAX_VALUE;
            };
        }

    }

    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    @ConfigEntry.Gui.Tooltip(count = 4)
    public PlaybackPacketRatelimit playbackPacketRatelimit = PlaybackPacketRatelimit.Limit500;


    @ConfigEntry.Gui.Tooltip(count = 1)
    public float delayPlaybackStartBySecs = 0.0f;

    @ConfigEntry.Gui.Tooltip(count = 3) public boolean instrumentDetectionWorkaround = true;

    @ConfigEntry.Gui.Excluded
    public ArrayList<String> favorites = new ArrayList<>();

}
