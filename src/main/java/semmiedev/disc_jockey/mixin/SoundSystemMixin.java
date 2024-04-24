package semmiedev.disc_jockey.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import semmiedev.disc_jockey.OmnidirectionalSoundInstance;

import java.util.Map;

@Mixin(SoundSystem.class)
public class SoundSystemMixin {
    @Shadow @Final private Map<SoundInstance, Channel.SourceManager> sources;

    @Inject(method = "updateListenerPosition", at = @At("TAIL"))
    private void updateOmnidirectionalSoundInstances(Camera camera, CallbackInfo ci) {
        Vec3d pos = camera.getPos();

        sources.forEach((soundInstance, sourceManager) -> {
            if (!(soundInstance instanceof OmnidirectionalSoundInstance)) return;

            sourceManager.run(source -> source.setPosition(pos));
        });
    }
}
