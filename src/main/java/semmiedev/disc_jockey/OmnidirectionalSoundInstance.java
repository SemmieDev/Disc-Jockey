package semmiedev.disc_jockey;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public class OmnidirectionalSoundInstance extends AbstractSoundInstance {
    private final Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();

    public OmnidirectionalSoundInstance(SoundEvent sound, SoundCategory category, float volume, float pitch, Random random) {
        super(sound, category, random);
        this.volume = volume;
        this.pitch = pitch;
        this.attenuationType = AttenuationType.NONE;
    }

    @Override
    public double getX() {
        return getPos().x;
    }

    @Override
    public double getY() {
        return getPos().y;
    }

    @Override
    public double getZ() {
        return getPos().z;
    }

    private Vec3d getPos() {
        return camera.getPos();
    }
}
