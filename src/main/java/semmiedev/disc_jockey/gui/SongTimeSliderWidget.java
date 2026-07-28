package semmiedev.disc_jockey.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import semmiedev.disc_jockey.Main;

public class SongTimeSliderWidget extends AbstractSliderButton {

    public SongTimeSliderWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty(), 0);
    }

    private static String padZeroes(int number, int length) {
        StringBuilder builder = new StringBuilder("" + number);
        while (builder.length() < length) {
            builder.insert(0, '0');
        } return builder.toString();
    }

    private static String formatTimestamp(int seconds) {
        if (seconds < 0) {
            return "-" + formatTimestamp(-seconds);
        }
        return padZeroes(seconds / 60, 2) + ":" + padZeroes(seconds % 60, 2);
    }

    @Override
    protected void updateMessage() {
        if (Main.SONG_PLAYER.song == null) {
            setMessage(Component.empty());
        } else {
            setMessage(Component.literal(formatTimestamp((int) Main.SONG_PLAYER.getSongElapsedSeconds()) + " / " + formatTimestamp((int) Main.SONG_PLAYER.song.getLengthInSeconds())));
        }
    }

    @Override
    protected void applyValue() {
        if(Main.SONG_PLAYER.song == null) return;
        double total = Main.SONG_PLAYER.song.getLengthInSeconds();
        double seconds = value * total;
        Main.SONG_PLAYER.setSongElapsedSeconds(seconds);
    }

    public void update() {
        if (Main.SONG_PLAYER.song == null) return;
        double total = Main.SONG_PLAYER.song.getLengthInSeconds();
        if (total <= 0) {
            value = 0.0;
        } else {
            double elapsed = Main.SONG_PLAYER.getSongElapsedSeconds();
            value = Math.max(0.0, Math.min(1.0, elapsed / total));
        }
        updateMessage();
    }
}
