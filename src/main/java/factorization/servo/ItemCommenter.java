package factorization.servo;

import factorization.api.Coord;
import factorization.shared.Core.TabType;
import factorization.shared.ItemFactorization;
import factorization.shared.NetworkFactorization.MessageType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class ItemCommenter extends ItemFactorization {

    public ItemCommenter(String name) {
        super(name, TabType.SERVOS);
        setMaxStackSize(1);
    }

    @Override
    public boolean onItemUse(ItemStack is, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float vx,
            float vy, float vz) {
        if (player == null) {
            return false;
        }
        Coord at = new Coord(world, pos);
        TileEntityServoRail rail = at.getTE(TileEntityServoRail.class);
        if (rail == null) {
            return false;
        }
        if (!world.isRemote) {
            rail.broadcastMessage(player, MessageType.ServoRailEditComment, rail.comment == null ? "" : rail.comment);
        }
        return true;
    }
}
