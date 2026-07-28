package semmiedev.disc_jockey;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public class Previewer implements ClientTickEvents.StartLevelTick {
    public boolean running;

    private int i;
    private float tick;
    private Song song;

    public void start(Song song) {
        if (song == null) return;
        Main.TICK_LISTENERS.remove(this);
        this.song = song;
        running = true;
        i = 0;
        tick = 0;
        Main.TICK_LISTENERS.add(this);
    }

    public void stop() {
        Main.TICK_LISTENERS.remove(this);
        running = false;
        i = 0;
        tick = 0;
    }

    @Override
    public void onStartTick(ClientLevel world) {
        if (!running || song == null || i >= song.notes.length) {
            return;
        }

        while (running && i < song.notes.length) {
            long note = song.notes[i];
            if ((short)note <= Math.round(tick)) {
                Vec3 pos = Minecraft.getInstance().gameRenderer.mainCamera().position();
                world.playLocalSound(pos.x, pos.y, pos.z, Note.INSTRUMENTS[(byte)(note >> Note.INSTRUMENT_SHIFT)].getSoundEvent().value(), SoundSource.RECORDS, 3, (float)Math.pow(2.0, ((byte)(note >> Note.NOTE_SHIFT) - 12) / 12.0), false);
                i++;
                if (i >= song.notes.length) {
                    stop();
                    break;
                }
            } else {
                break;
            }
        }

        if (running) {
            tick += song.tempo / 100f / 20f;
        }
    }
}
