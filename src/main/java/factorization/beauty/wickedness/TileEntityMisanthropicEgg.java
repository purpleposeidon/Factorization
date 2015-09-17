package factorization.beauty.wickedness;

import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataOutNBT;
import factorization.api.datahelpers.Share;
import factorization.beauty.wickedness.api.EvilBit;
import factorization.beauty.wickedness.api.EvilRegistry;
import factorization.beauty.wickedness.api.IEvil;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.TileEntityCommon;
import factorization.util.EvilUtil;
import factorization.util.FzUtil;
import factorization.util.ItemUtil;
import factorization.util.PlayerUtil;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.io.IOException;
import java.util.Random;

public class TileEntityMisanthropicEgg extends TileEntityCommon {
    int power = 8;
    EvilBit evilBit = null;
    IEvil evil = null;

    void init(Random rand) {
        int color1 = randColor(rand);
        int color2 = randColor(rand);
        evilBit = new EvilBit(color1, color2);
        evil = EvilRegistry.find(EvilRegistry.NORMAL, color1, color2);
        if (evil == null) return;
        evil.setup(this, evilBit);
    }

    int randColor(Random rand) {
        // Logic from BlockRenderGreenware.getColor
        int c = 0;
        for (int i = 0; i < 3; i++) {
            c <<= 8;
            c += rand.nextInt(0x80) + 40;
        }
        return c;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        power = data.as(Share.VISIBLE, "power").putByte((byte) power);
        evilBit = data.as(Share.PRIVATE, "evilBit").put(evilBit);
        if (data.isReader() && data.isNBT()) {
            int evilId = data.as(Share.PRIVATE, "evilId").putInt(0);
            evil = EvilRegistry.get(EvilRegistry.NORMAL, evilId);
        } else {
            int evilId = EvilRegistry.getId(evil);
            evilId = data.as(Share.PRIVATE, "evilId").putInt(evilId);
        }
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.MISANTHROPIC_EGG;
    }

    @Override
    public void loadFromStack(ItemStack is) {
        NBTTagCompound tag = is.getTagCompound();
        if (tag == null) {
            return;
        }
        try {
            new DataInNBT(tag).put(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, int side, float hitX, float hitY, float hitZ) {
        super.onPlacedBy(player, is, side, hitX, hitY, hitZ);
        init(player.worldObj.rand);
    }

    @Override
    public void representYoSelf() {
        StandardEvil.load();
    }

    @Override
    public ItemStack getDroppedBlock() {
        ItemStack ret = super.getDroppedBlock();
        DataHelper data = new DataOutNBT(ItemUtil.getTag(ret));;
        try {
            putData(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    void broken(double p) {
        if (worldObj == null || worldObj.isRemote) return;
        if (rand.nextDouble() > p) return;
        if (evilBit == null) return;
        IEvil evil = EvilRegistry.find(EvilRegistry.ON_BREAK, evilBit.getMainColor(), evilBit.getSecondColor());
        if (evil == null) return;
        evil.setup(this, evilBit);
        if (evilBit.cost > power) return;
        if (evil.act(this, evilBit, null, rand)) {
            power = 0;
        }
    }

    @Override
    protected void onRemove() {
        broken(0.9);
    }

    @Override
    protected boolean removedByPlayer(EntityPlayer player, boolean willHarvest) {
        if (!willHarvest) return super.removedByPlayer(player, willHarvest);
        double p = 0.5 + EvilUtil.getMoonPower(worldObj);
        if (player.isSneaking()) {
            p *= 0.5;
        }
        if (EnchantmentHelper.getSilkTouchModifier(player)) {
            p *= 0.5;
        }
        broken(p);
        return super.removedByPlayer(player, willHarvest);
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;
        if (evilBit == null) return;
        EntityPlayer closestPlayer = null;
        if (evilBit.playerRange > 0) {
            closestPlayer = EvilUtil.getClosestPlayer(new Coord(this), evilBit.playerRange);
            if (closestPlayer == null) return;
        }
        if (evilBit.tickSpeed <= 1 || (evilBit.ticks++ % evilBit.tickSpeed) == 0) {
            if (evil.act(this, evilBit, closestPlayer, rand)) {
                power -= evilBit.cost;
                if (power <= 0) {
                    decay();
                }
            }
        }
    }

    void decay() {
        new Coord(this).setAir();
    }
}
