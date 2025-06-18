package semmiedev.disc_jockey.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import semmiedev.disc_jockey.Main;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "playSound(DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZJ)V", at = @At("HEAD"), cancellable = true)
    private void makeNoteBlockSoundsOmnidirectional(double x, double y, double z, SoundEvent event, SoundCategory category, float volume, float pitch, boolean useDistance, long seed, CallbackInfo ci) {
        if (((Main.config.omnidirectionalNoteBlockSounds && Main.SONG_PLAYER.running) || Main.PREVIEWER.running) && event.id().getPath().startsWith("block.note_block")) {
            ci.cancel();
            client.getSoundManager().play(new PositionedSoundInstance(event.id(), category, volume, pitch, Random.create(seed), false, 0, SoundInstance.AttenuationType.NONE, 0, 0, 0, true));
        }
    }
}
