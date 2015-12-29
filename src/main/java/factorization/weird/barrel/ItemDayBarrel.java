package factorization.weird.barrel;

import factorization.common.FactoryType;
import factorization.shared.Core;
import factorization.shared.TileEntityCommon;
import factorization.util.LangUtil;
import factorization.weird.barrel.TileEntityDayBarrel.Type;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class ItemDayBarrel extends ItemBlock {
    public ItemDayBarrel(Block block) {
        super(block);
        setMaxDamage(0);
        setNoRepair();
        Core.tab(this, Core.TabType.BLOCKS);
    }

    @Override
    public boolean getShareTag() {
        return true;
    }

    @Override
    public String getItemStackDisplayName(ItemStack is) {
        Type upgrade = TileEntityDayBarrel.getUpgrade(is);
        String lookup = "factorization.factoryBlock.DAYBARREL.format";
        if (upgrade != Type.NORMAL) {
            lookup = "factorization.factoryBlock.DAYBARREL.format2";
        }
        String type = LangUtil.translate("factorization.factoryBlock.DAYBARREL." + upgrade);
        return LangUtil.translateWithCorrectableFormat(lookup, type, TileEntityDayBarrel.getLog(is).getDisplayName());
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer playerIn, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, playerIn, tooltip, advanced);
        addExtraInformation(stack, playerIn, tooltip, advanced);
    }

    @SideOnly(Side.CLIENT) // Invokes a client-only function getTooltip
    protected void addExtraInformation(ItemStack is, EntityPlayer player, List<String> list, boolean verbose) {
        Type upgrade = TileEntityDayBarrel.getUpgrade(is);
        if (upgrade == Type.SILKY) {
            list.add(LangUtil.translateThis("factorization.factoryBlock.DAYBARREL.SILKY.silkhint"));
            TileEntityDayBarrel db = (TileEntityDayBarrel) FactoryType.DAYBARREL.getRepresentative();
            db.loadFromStack(is);
            int count = db.getItemCount();
            if (count > 0 && db.item != null) {
                if (db.item.getItem() == this) {
                    list.add("?");
                    return;
                }
                List<String> sub = db.item.getTooltip/* Client-only */(player, false /* Propagating verbose would be natural, but let's keep the tool-tip short */);
                db.item.getItem().addInformation(db.item, player, sub, verbose);
                if (!sub.isEmpty()) {
                    Object first = sub.get(0);
                    sub.set(0, count + " " + first);
                    list.addAll(sub);
                }
            }
        }
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState) {
        if (!super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState)) return false;
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityCommon) {
            TileEntityCommon tec = (TileEntityCommon) te;
            tec.onPlacedBy(player, stack, side, hitX, hitY, hitZ);
        }
        return true;
    }
}
