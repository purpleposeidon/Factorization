package factorization.api;

import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

// It's just so right that they go together, like chocolate and peanutbutter. Not to be confused with InsaneCoord.
public interface ISaneCoord {
    World w();
    BlockPos toBlockPos();
}
