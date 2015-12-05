package factorization.artifact;

import factorization.common.FactoryType;
import factorization.shared.Core;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class BlockForge extends BlockAnvil {
    public BlockForge() {
        Core.tab(this, Core.TabType.ARTIFACT);
        setUnlocalizedName("factorization:artifactForge");
        setHardness(5.0F).setResistance(2000.0F);
        setStepSound(new Block.SoundType("anvil", 1.0F, 0.125F) {
            public String getBreakSound() {
                return "dig.stone";
            }
            public String func_150496_b() {
                return "random.anvil_land";
            }
        });
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, EntityPlayer player, int side, float vx, float vy, float vz) {
        if (w.isRemote) return true;
        if (InspirationManager.canMakeArtifact(player)) {
            player.openGui(Core.instance, FactoryType.ARTIFACTFORGEGUI.gui, w, pos);
        } else {
            player.addChatMessage(new ChatComponentTranslation("factorization.forge.wait").setChatStyle(InspirationManager.aqua));
        }
        return true;
    }

    @SideOnly(Side.CLIENT)
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(item, 1, 0));
    }
}
