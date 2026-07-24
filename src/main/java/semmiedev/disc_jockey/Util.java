package semmiedev.disc_jockey;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.NotImplementedException;

public class Util {

    public static final long TIMESTAMP_UNINITIALIZED = -1L; // Magic timestamp

    // Get current timestamp, suited for measuring time, in millis
    public static long now() {
        return net.minecraft.util.Util.getMillis();
    }

    // Before 1.20.5, the server limits interacts to 6 Blocks from Player Eye to Block Center
    // With 1.20.5 and later, the server does a more complex check, to the closest point of a full block hitbox
    // (max distance is BlockInteractRange + 1.0).
    public static boolean canInteractWith(LocalPlayer player, BlockPos blockPos) {
        if (player == null) return false;

        final Vec3 eyePos = player.getEyePosition();
        if (Main.config.expectedServerVersion == Config.ExpectedServerVersion.v1_20_4_Or_Earlier) {
            return eyePos.distanceToSqr(Vec3.atCenterOf(blockPos)) <= 6.0 * 6.0;
        } else if (Main.config.expectedServerVersion == Config.ExpectedServerVersion.v1_20_5_Or_Later) {
            double blockInteractRange = player.blockInteractionRange() + 1.0;
            return new AABB(blockPos).distanceToSqr(eyePos) < blockInteractRange * blockInteractRange;
        } else if(Main.config.expectedServerVersion == Config.ExpectedServerVersion.All) {
            // Require both checks to succeed (aka use worst distance)
            double blockInteractRange = player.blockInteractionRange() + 1.0;
            return eyePos.distanceToSqr(Vec3.atCenterOf(blockPos)) <= 6.0 * 6.0
                    && new AABB(blockPos).distanceToSqr(eyePos) < blockInteractRange * blockInteractRange;
        } else {
            throw new NotImplementedException("ExpectedServerVersion Value not implemented: " + Main.config.expectedServerVersion.name());
        }
    }

}
