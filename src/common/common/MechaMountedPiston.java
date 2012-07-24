package factorization.common;

import net.minecraft.src.Block;
import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MathHelper;
import net.minecraft.src.forge.ArmorProperties;
import net.minecraft.src.forge.ITextureProvider;
import factorization.api.Coord;
import factorization.api.IMechaUpgrade;

public class MechaMountedPiston extends Item implements IMechaUpgrade, ITextureProvider {
    protected MechaMountedPiston(int par1) {
        super(par1);
        setItemName("mecha.mountedpiston");
        setIconIndex(16 * 10 + 2);
    }
    
    @Override
    public String getTextureFile() {
        return Core.texture_file_item;
    }

    private static int determineOrientation(EntityPlayer player) {
        if (player.rotationPitch > 75) {
            return 0;
        }
        if (player.rotationPitch <= -75) {
            return 1;
        }
        //stolen from BlockPistonBase.determineOrientation. It was reversed, & we handle the y-axis differently
        int var7 = MathHelper.floor_double((double) ((180 + player.rotationYaw) * 4.0F / 360.0F) + 0.5D) & 3;
        return var7 == 0 ? 2 : (var7 == 1 ? 5 : (var7 == 2 ? 3 : (var7 == 3 ? 4 : 0)));
    }

    boolean tryPush(Coord c, int orientation) {
        if (!c.isAir()) {
            return false;
        }
        if (c.copy().towardSide(orientation).isAir()) {
            return false;
        }
        Block.pistonBase.receiveClientEvent(c.w, c.x, c.y, c.z, 0, orientation);
        c.setId(0);
        if (Core.instance.isServer()) {
            Core.network.broadcastMessage(null, c, NetworkFactorization.MessageType.PistonPush, orientation);
        }
        return true;
    }

    @Override
    public ItemStack tickUpgrade(EntityPlayer player, ItemStack armor, ItemStack upgrade, boolean isEnabled) {
        if (!isEnabled) {
            return null;
        }
        if (!Core.instance.isCannonical(player.worldObj)) {
            return null;
        }

        if (!FactorizationUtil.itemCanFire(player.worldObj, upgrade, 15)) {
            return null;
        }

        Coord head;
        Coord foot;
        if (Core.instance.isServer()) {
            //...?
            //man, what's up with this?
            head = new Coord(player).add(0, 1, 0).add(-1, 0, 0);
            foot = new Coord(player).add(-1, 0, 0);
        }
        else {
            head = new Coord(player).add(0, -2, 0);
            foot = new Coord(player).add(0, -3, 0);
        }

        Coord order[];
        if (player.rotationPitch <= 45) {
            order = new Coord[] { head, foot };
        }
        else {
            order = new Coord[] { foot, head };
        }
        for (Coord c : order) {
            if (tryPush(c, determineOrientation(player))) {
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
    public boolean damageArmor(EntityLiving entity, ItemStack stack, DamageSource source, int damage, int slot) {
        return false;
    }

    @Override
    public String getDescription() {
        return "Push blocks";
    }
    
}