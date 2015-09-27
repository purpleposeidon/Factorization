package factorization.artifact;

import factorization.common.FactoryType;
import factorization.shared.Core;
import net.minecraft.block.BlockAnvil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

public class BlockForge extends BlockAnvil {
    public BlockForge() {
        Core.tab(this, Core.TabType.ARTIFACT);
        setBlockName("factorization:artifactForge");
    }

    @Override
    public boolean onBlockActivated(World w, int x, int y, int z, EntityPlayer player, int side, float vx, float vy, float vz) {
        if (w.isRemote) return true;
        if (InspirationManager.canMakeArtifact(player)) {
            player.openGui(Core.instance, FactoryType.ARTIFACTFORGEGUI.gui, w, x, y, z);
        } else {
            player.addChatMessage(new ChatComponentTranslation("factorization.forge.wait"));
        }
        return true;
    }
}
