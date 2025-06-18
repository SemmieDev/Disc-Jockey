package semmiedev.disc_jockey.gui;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import semmiedev.disc_jockey.Main;

public class SongTimeSliderWidget extends SliderWidget {

    public SongTimeSliderWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Text.empty(), 0);
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

    @Override
    protected void updateMessage() {
        if(Main.SONG_PLAYER.song == null)
            setMessage(Text.empty());
        else
            setMessage(Text.literal(formatTimestamp((int) Main.SONG_PLAYER.getSongElapsedSeconds()) + " / " + formatTimestamp((int) Main.SONG_PLAYER.song.getLengthInSeconds())));
    }

    @Override
    protected void applyValue() {
        if(Main.SONG_PLAYER.song == null) return;
        double total = Main.SONG_PLAYER.song.getLengthInSeconds();
        double seconds = value * total;
        Main.SONG_PLAYER.setSongElapsedSeconds(seconds);
    }

    public void update() {
        if(Main.SONG_PLAYER.song == null) return;
        double elapsed = Main.SONG_PLAYER.getSongElapsedSeconds();
        double total = Main.SONG_PLAYER.song == null ? 1 : Main.SONG_PLAYER.song.getLengthInSeconds();
        value = elapsed / total;
        updateMessage();
    }
}
