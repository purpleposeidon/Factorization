package factorization.common.servo;

import factorization.common.FactorizationUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemServoComponent extends Item {
    public ItemServoComponent(int itemId) {
        super(itemId);
    }
    
    ServoComponent get(ItemStack is) {
        if (!is.hasTagCompound()) {
            return null;
        }
        return ServoComponent.load(is.getTagCompound());
    }
    
    void update(ItemStack is, ServoComponent sc) {
        if (sc != null) {
            sc.save(FactorizationUtil.getTag(is));
        } else {
            is.setTagCompound(null);
        }
    }
    
    @Override
    public ItemStack onItemRightClick(ItemStack is, World world, EntityPlayer player) {
        ServoComponent sc = get(is);
        if (sc == null) {
            return is;
        }
        if (sc instanceof Actuator) {
            Actuator ac = (Actuator) sc;
            ac.onUse(player, player.isSneaking());
        }
        return super.onItemRightClick(is, world, player);
    }
    
    @Override
    public boolean onItemUse(ItemStack is, EntityPlayer player, World world, int x, int y, int z, int side, float vx, float vy, float vz) {
        ServoComponent sc = get(is);
        if (sc == null) {
            return false;
        }
        if (sc instanceof Decorator) {
            Decorator dec = (Decorator) sc;
        }
        return super.onItemUse(is, player, world, x, y, z, side, vx, vy, vz);
    }
    
    
}
