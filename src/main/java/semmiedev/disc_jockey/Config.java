package semmiedev.disc_jockey;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;

@me.shedaniel.autoconfig.annotation.Config(name = Main.MOD_ID)
@me.shedaniel.autoconfig.annotation.Config.Gui.Background("textures/block/note_block.png")
public class Config implements ConfigData {
    public boolean hideWarning;
    @ConfigEntry.Gui.Tooltip(count = 2) public boolean disableAsyncPlayback;
    @ConfigEntry.Gui.Excluded @ConfigEntry.Gui.Tooltip(count = 2) public boolean monoNoteBlocks;

    @ConfigEntry.Gui.Excluded
    public ArrayList<String> favorites = new ArrayList<>();
}
