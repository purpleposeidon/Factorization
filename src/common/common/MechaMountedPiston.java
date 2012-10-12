package factorization.common;

import java.util.List;

import net.minecraft.src.Block;
import net.minecraft.src.CreativeTabs;
import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MathHelper;
import net.minecraftforge.common.ISpecialArmor.ArmorProperties;
import factorization.api.Coord;
import factorization.api.IMechaUpgrade;
import factorization.common.Core.TabType;

public class MechaMountedPiston extends Item implements IMechaUpgrade {
    protected MechaMountedPiston(int par1) {
        super(par1);
        setItemName("mecha.mountedpiston");
        setIconIndex(16 * 10 + 2);
        Core.tab(this, TabType.MISC);
        setMaxStackSize(1);
    }

    @Override
    public String getTextureFile() {
        return Core.texture_file_item;
    }

    boolean tryPush(Coord c, int orientation) {
        if (!c.isAir()) {
            return false;
        }
        if (c.copy().towardSide(orientation).isAir()) {
            return false;
        }
        Block.pistonBase.onBlockEventReceived(c.w, c.x, c.y, c.z, 0, orientation);
        c.setId(0);
        Core.network.broadcastMessage(null, c, NetworkFactorization.MessageType.PistonPush, orientation);
        return true;
    }

    @Override
    public ItemStack tickUpgrade(EntityPlayer player, ItemStack armor, ItemStack upgrade,
            boolean isEnabled) {
        if (!isEnabled) {
            return null;
        }
        if (player.worldObj.isRemote) {
            return null;
        }

        if (!FactorizationUtil.itemCanFire(player.worldObj, upgrade, 15)) {
            return null;
        }

        Coord head;
        Coord foot;
        //TODO: Figure out which one to use, since this'll always be on a server...
        head = new Coord(player).add(-1, 1, -1);
        foot = new Coord(player).add(-1, 0, -1);

        Coord order[];
        if (player.rotationPitch <= 45) {
            order = new Coord[] { head, foot };
        }
        else {
            order = new Coord[] { foot, head };
        }
        for (Coord c : order) {
            if (tryPush(c, FactorizationUtil.determineOrientation(player))) {
                return upgrade;
            }
        }

        return null;
    }

    @Override
    public void addArmorProperties(ItemStack is, ArmorProperties armor) {
    }

    @Override
    public int getArmorDisplay(ItemStack is) {
        return 0;
    }

    @Override
    public boolean damageArmor(EntityLiving entity, ItemStack stack, DamageSource source,
            int damage, int slot) {
        return false;
    }

    @Override
    public String getDescription() {
        return "Push blocks";
    }

    @Override
    public void addInformation(ItemStack is, List list) {
        list.add("Mecha-Upgrade");
        Core.brand(list);
    }
}