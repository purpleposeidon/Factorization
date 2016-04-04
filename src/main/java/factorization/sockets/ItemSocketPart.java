package factorization.sockets;

import factorization.shared.Core.TabType;
import factorization.shared.ItemFactorization;
import factorization.util.LangUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

import java.util.List;

public class ItemSocketPart extends ItemFactorization {
    public ItemSocketPart(String name, TabType tabType) {
        super(name, tabType);
        setHasSubtypes(true);
        setMaxDamage(0);
    }
    
    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ) {
        return true;
    }

    @Override
    protected void addExtraInformation(ItemStack is, EntityPlayer player, List<String> list, boolean verbose) {
        list.add(LangUtil.translate("item.factorization:socket_info"));
    }
}
