package semmiedev.disc_jockey;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;

@me.shedaniel.autoconfig.annotation.Config(name = Main.MOD_ID)
public class Config implements ConfigData {
    @ConfigEntry.Gui.Tooltip
    public boolean monoNoteBlocks;
    public boolean hideWarning;

    @ConfigEntry.Gui.Excluded
    public ArrayList<String> favorites = new ArrayList<>();
}
