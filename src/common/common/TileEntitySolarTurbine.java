package factorization.common;

import java.io.DataInput;
import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import factorization.api.Charge;
import factorization.api.Coord;
import factorization.api.IChargeConductor;
import factorization.api.IReflectionTarget;
import factorization.common.NetworkFactorization.MessageType;

public class TileEntitySolarTurbine extends TileEntityCommon implements IChargeConductor,
        IReflectionTarget {
    Charge charge = new Charge(this);
    int reflectors = 0;
    public int water_level = 0;
    public static int max_water = 256 * 4;
    int fan_speed = 0;

    public float fan_rotation = 0;

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SOLARTURBINE;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    @Override
    public Charge getCharge() {
        return charge;
    }

    @Override
    public String getInfo() {
        return "Power: " + getHeat();
    }

    @Override
    public void addReflector(int strength) {
        reflectors += strength;
        if (reflectors < 0) {
            System.err.println("reflector count went negative!");
            new Exception().printStackTrace();
            reflectors = 0;
        }
    }

    public int getReflectors() {
        return reflectors;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        charge.writeToNBT(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        charge.readFromNBT(tag);
        fan_rotation = rand.nextInt(90);
    }

    @Override
    public boolean activate(EntityPlayer entityplayer) {
        return false;
    }

    int getHeat() {
        return reflectors;
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

    int last_speed = -99, last_quart_water_level = -99;

    boolean distant(int original, int curent, float delta) {
        int d = (int) (original*delta);
        return (curent < original - d) || (original + d < curent);
    }
    
    void updateClients() {
        if (last_speed != fan_speed) {
        //if (distant(fan_speed, last_speed, 1F)) {
            broadcastMessage(null, MessageType.TurbineSpeed, fan_speed);
            last_speed = fan_speed;
        }
        int quart_water_level = water_level / 4;
        if (last_quart_water_level != quart_water_level) {
        //if (distant(quart_water_level, last_quart_water_level, 0.25F)) {
            broadcastMessage(null, MessageType.TurbineWater, water_level);
            last_quart_water_level = quart_water_level;
        }
    }

    @Override
    public boolean handleMessageFromServer(int messageType, DataInput input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        switch (messageType) {
        case MessageType.TurbineSpeed:
            fan_speed = input.readInt();
            return true;
        case MessageType.TurbineWater:
            water_level = input.readInt();
            getCoord().redraw();
            return true;
        }
        return false;
    }

    @Override
    public void updateEntity() {
        if (fan_speed == -1) {
            fan_rotation += rand.nextBoolean() ? -0.25 : 0.25;
        }
        else {
            if (fan_speed > 35) {
                fan_rotation += 35;
            } else {
                fan_rotation += fan_speed;
            }
        }
        if (worldObj.isRemote) {
            return;
        }
        updateClients();
        charge.update();
        if (water_level <= 0) {
            Coord below = getCoord().add(0, -1, 0);
            if (below.is(Block.waterMoving) || below.is(Block.waterStill)) {
                if (below.getMd() == 0) {
                    below.setId(0);
                    water_level = max_water;
                    getCoord().redraw();
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
            if (heat > 0) {
                if (fan_speed > heat / 2) {
                    fan_speed--;
                }
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
        int delta = Math.min(heat, water_level);
        charge.addValue(delta); //This is fine; heat depends on reflectors.
        water_level -= delta;
        if (water_level / 64 != last_cut) {
            getCoord().redraw();
        }
    }

    @Override
    public boolean isBlockSolidOnSide(int side) {
        return side != 1;
    }
}
