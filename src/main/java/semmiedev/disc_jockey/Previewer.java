package semmiedev.disc_jockey;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundCategory;

public class Previewer implements ClientTickEvents.StartWorldTick {
    public boolean running;

    private int i;
    private float tick;
    private Song song;

    public void start(Song song) {
        this.song = song;
        Main.TICK_LISTENERS.add(this);
        running = true;
    }

    public void stop() {
        MinecraftClient.getInstance().send(() -> Main.TICK_LISTENERS.remove(this));
        running = false;
        i = 0;
        tick = 0;
    }

    @Override
    public void onStartTick(ClientWorld world) {
        while (running) {
            long note = song.notes[i];
            if ((short)note == Math.round(tick)) {
                world.playSoundFromEntity(MinecraftClient.getInstance().player, MinecraftClient.getInstance().player, Note.INSTRUMENTS[(byte)(note >> Note.INSTRUMENT_SHIFT)].getSound().value(), SoundCategory.RECORDS, 3, (float)Math.pow(2.0, ((byte)(note >> Note.NOTE_SHIFT) - 12) / 12.0));
                i++;
                if (i >= song.notes.length) {
                    stop();
                    break;
                }
            } else {
                break;
            }
        }

        tick += song.tempo / 100f / 20f;
    }
}
