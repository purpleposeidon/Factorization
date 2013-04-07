package factorization.common;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.util.DamageSource;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ISpecialArmor.ArmorProperties;
import factorization.api.Coord;
import factorization.api.IExoUpgrade;
import factorization.common.Core.TabType;

public class ExoMountedPiston extends Item implements IExoUpgrade {
    protected ExoMountedPiston(int par1) {
        super(par1);
        setUnlocalizedName("factorization:exo/mountedpiston");
        Core.tab(this, TabType.MISC);
        setMaxStackSize(1);
    }
    
    @Override
    public boolean canUpgradeArmor(ItemStack is, int armorIndex) {
        return armorIndex == 1;
    }

    boolean tryPush(Coord c, int orientation) {
        if (!c.isAir()) {
            return false;
        }
        if (c.copy().towardSide(orientation).isAir()) {
            return false;
        }
        c.w.isRemote = true; //oh god why
        try {
            Block.pistonBase.onBlockEventReceived(c.w, c.x, c.y, c.z, 0, orientation);
        } finally {
            c.w.isRemote = false;
            //Here's why: In 1.5, pistons started checking if they're powered.
            //But they don't check if the world's remote.
            //There's definitely a possibility that this could break something tho. v_v
            //This code only gets called when !w.isRemote, so we're not making the client think it's a server.
            //(Actually, there's only 4 lines I'd need to copy. But one of them is a call to a private method.)
        }
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
        head = new Coord(player).add(0, 1, 0);
        foot = new Coord(player);

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
    public void addInformation(ItemStack is, EntityPlayer player, List infoList, boolean verbose) {
        infoList.add("Exo-Upgrade");
        Core.brand(infoList);
    }
}
