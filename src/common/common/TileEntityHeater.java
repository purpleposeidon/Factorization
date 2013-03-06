package factorization.common;

import java.io.DataInput;
import java.io.IOException;
import java.lang.reflect.Field;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import net.minecraft.block.BlockFurnace;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Charge;
import factorization.api.Coord;
import factorization.api.IChargeConductor;
import factorization.common.NetworkFactorization.MessageType;

public class TileEntityHeater extends TileEntityCommon implements IChargeConductor {
    Charge charge = new Charge(this);
    public byte heat = 0;
    public static final byte maxHeat = 32;

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.HEATER;
    }
    
    @SideOnly(Side.CLIENT)
    public static FzIcon heater_spiral = tex("machine/heater_spiral"), heater_heat = tex("machine/heater_heat");
    
    @Override
    Icon getIcon(ForgeDirection dir) {
        return heater_spiral;
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
        return null;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        charge.writeToNBT(tag);
        tag.setByte("heat", heat);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        charge.readFromNBT(tag);
        heat = tag.getByte("heat");
    }
    
    int charge2heat(int i) {
        return (int) (i / 1.5);
    }

    byte last_heat = -99;

    void updateClient() {
        int delta = Math.abs(heat - last_heat);
        if (delta > 2) {
            broadcastMessage(null, MessageType.HeaterHeat, heat);
            last_heat = heat;
        }
    }

    @Override
    byte getExtraInfo() {
        return heat;
    }

    @Override
    void useExtraInfo(byte b) {
        heat = b;
    }

    @Override
    public boolean handleMessageFromServer(int messageType, DataInput input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.HeaterHeat) {
            heat = input.readByte();
            return true;
        }
        return false;
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) {
            return;
        }
        updateClient();
        Coord here = getCoord();
        long now = worldObj.getWorldTime() + here.seed();
        int rate = 4;
        if (now % rate == 0) {
            int heatToRemove = maxHeat - heat;
            int avail = Math.min(heatToRemove, charge.getValue());
            if (avail > 0 && charge2heat(heatToRemove) > 0) {
                heat += charge2heat(charge.deplete(heatToRemove));
            }
        }
        charge.update();

        if (heat <= 0) {
            return;
        }
        for (Coord c : here.getRandomNeighborsAdjacent()) {
            sendHeat(c.getTE());
            if (heat <= 0) {
                return;
            }
        }
    }

    boolean shouldHeat(int cookTime) {
        if (heat >= maxHeat * 0.5) {
            return true;
        }
        return cookTime > 0;
    }

    int addGraceHeat(int burnTime) {
        return Math.max(4, burnTime);
    }

    private class ProxiedHeatingResult {
        int burnTime, cookTime, topBurnTime;

        public ProxiedHeatingResult(Coord furnace, int burnTime, int cookTime) {
            this.burnTime = burnTime;
            this.cookTime = cookTime;
            this.topBurnTime = 200;
            calculate(furnace);
        }

        private void calculate(Coord furnace) {
            for (int i = 0; i < 2; i++) {
                if (heat <= maxHeat / 2) {
                    return;
                }
                if (burnTime < topBurnTime) {
                    burnTime += 1;
                } else {
                    cookTime += 1;
                }
                heat--;
            }
        }
    }

    static Class<? extends TileEntity> rpAlloyFurnace = null;
    static {
        //Waiting for Elo to set up something on her end so that we can check if it's heatable
//		try {
//			rpAlloyFurnace = (Class<? extends TileEntity>) Class.forName("com.eloraam.redpower.base.TileAlloyFurnace");
//			Core.logInfo("Heaters will work on RedPower Alloy Furnaces");
//		} catch (ClassNotFoundException e) {
//			Core.logInfo("Couldn't find AlloyFurnace");
//			e.printStackTrace();
//		}
    }
    
    void sendHeat(TileEntity te) {
        if (te instanceof TileEntityFurnace) {
            TileEntityFurnace furnace = (TileEntityFurnace) te;
            if (!TEF_canSmelt(furnace)) {
                return;
            }
            ProxiedHeatingResult pf = new ProxiedHeatingResult(new Coord(te), furnace.furnaceBurnTime, furnace.furnaceCookTime);
            furnace.furnaceBurnTime = pf.burnTime;
            furnace.furnaceCookTime = Math.min(pf.cookTime, 200 - 1);
            BlockFurnace.updateFurnaceBlockState(furnace.furnaceCookTime > 0, worldObj, te.xCoord, te.yCoord, te.zCoord);
        }
        if (te instanceof TileEntitySlagFurnace) {
            TileEntitySlagFurnace furnace = (TileEntitySlagFurnace) te;
            if (!furnace.canSmelt()) {
                return;
            }
            ProxiedHeatingResult pf = new ProxiedHeatingResult(new Coord(te), furnace.furnaceBurnTime, furnace.furnaceCookTime);
            furnace.furnaceBurnTime = pf.burnTime;
            furnace.furnaceCookTime = pf.cookTime;
        } 
        if (te instanceof TileEntityCrystallizer) {
            TileEntityCrystallizer crys = (TileEntityCrystallizer) te;
            if (!crys.needHeat()) {
                return;
            }
            crys.heat++;
            heat--;
        } 
        if (rpAlloyFurnace != null && rpAlloyFurnace.isInstance(te)) {
            Exception err = null;
            try {
                Field burntimeField = rpAlloyFurnace.getField("burntime");
                //Field totalburnField = rpAlloyFurnace.getField("totalburn");
                ProxiedHeatingResult pf = new ProxiedHeatingResult(new Coord(te), burntimeField.getInt(te), 0 /* Elo doesn't want speedy. :( */);
                burntimeField.setInt(te, pf.burnTime);
            } catch (SecurityException e) {
                err = e;
            } catch (NoSuchFieldException e) {
                err = e;
            } catch (IllegalArgumentException e) {
                err = e;
            } catch (IllegalAccessException e) {
                err = e;
            } finally {
                if (err != null) {
                    rpAlloyFurnace = null;
                    Core.logWarning("Failed to reflect into RedPower AlloyFurnace; heating disabled.");
                    err.printStackTrace();
                }
            }
        }
    }

    boolean TEF_canSmelt(TileEntityFurnace diss) {
        //private function for TileEntityFurnace.canSmelt, boooo
        if (diss.getStackInSlot(0) == null) {
            return false;
        } else {
            ItemStack var1 = FurnaceRecipes.smelting().getSmeltingResult(diss.getStackInSlot(0));
            if (var1 == null)
                return false;
            if (diss.getStackInSlot(2) == null)
                return true;
            if (!diss.getStackInSlot(2).isItemEqual(var1) /* no NBT okay (vanilla source) */ )
                return false;
            int result = diss.getStackInSlot(2).stackSize + var1.stackSize;
            return (result <= diss.getInventoryStackLimit() && result <= var1.getMaxStackSize());
        }
    }
}
