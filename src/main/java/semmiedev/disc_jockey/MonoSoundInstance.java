package semmiedev.disc_jockey;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

// TODO: 6/1/2022 Make it actually mono
public class MonoSoundInstance extends AbstractSoundInstance {
    private final Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();

    public MonoSoundInstance(SoundEvent sound, SoundCategory category, float volume, float pitch, Random random) {
        super(sound, category, random);
        this.volume = volume;
        this.pitch = pitch;
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
        return camera.getPos().add(Vec3d.fromPolar(camera.getPitch(), camera.getYaw()).multiply(0, 0, 1));
    }
}
