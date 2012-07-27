package factorization.common;

import java.util.Random;

import net.minecraft.src.Block;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.NBTTagCompound;
import factorization.api.Charge;
import factorization.api.Coord;
import factorization.api.IChargeConductor;

public class TileEntitySolarTurbine extends TileEntityCommon implements IChargeConductor {
    Charge charge = new Charge();
    int reflectors = 0;
    public int water_level = 0;

    public int fan_speed = 0;
    public float fan_rotation = 0;

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SOLARTURBINE;
    }

    @Override
    public Charge getCharge() {
        return charge;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        charge.writeToTag(tag, "charge");
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        charge = Charge.readFromTag(tag, "charge");
    }

    @Override
    public boolean activate(EntityPlayer entityplayer) {
        return false;
    }

    boolean hasSun() {
        Coord here = getCoord();
        if (!here.canSeeSky()) {
            return false;
        }
        return worldObj.isDaytime();
    }

    int getHeat() {
        return (hasSun() ? 1 : 0) + reflectors;
    }

    boolean saturated() {
        return charge.getValue() > getHeat() * 3;
    }

    void adjustFanSpeed() {
        int heat = getHeat();
        if (heat != fan_speed) {
            int delta = heat - fan_speed;
            int change = delta / 16;
            if (change == 0) {
                change = 1;
            }
            fan_speed += Math.copySign(change, delta);
        }
    }

    static Random rand = new Random();
    @Override
    public void updateEntity() {
        charge.update(this);
        if (water_level <= 0) {
            Coord below = getCoord().add(0, -1, 0);
            if (below.is(Block.waterMoving) || below.is(Block.waterStill)) {
                if (below.getMd() == 0) {
                    below.setId(0);
                    water_level = 256;
                    getCoord().dirty();
                }
            }
        }
        if (water_level <= 0) {
            return;
        }
        int heat = getHeat();
        if (heat <= 0) {
            adjustFanSpeed();
            return;
        }
        if (saturated()) {
            if (fan_speed == -1) {
                return;
            }
            if (fan_speed > -1 && heat > 0) {
                fan_speed--;
            } else {
                adjustFanSpeed();
            }
            return;
        }
        adjustFanSpeed();
        if (reflectors == 0 && worldObj.getWorldTime() % 20 == 0) {
            return;
        }
        int last_cut = water_level / 64;
        int d = heat > water_level ? water_level : heat;
        charge.addValue(d);
        water_level -= d;
        if (water_level / 64 != last_cut) {
            getCoord().dirty();
        }
    }
}
