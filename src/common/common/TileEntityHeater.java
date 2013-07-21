package factorization.common;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Field;

import net.minecraft.block.BlockFurnace;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Charge;
import factorization.api.Coord;
import factorization.api.IChargeConductor;
import factorization.common.NetworkFactorization.MessageType;
import factorization.common.TileEntityGreenware.ClayState;

public class TileEntityHeater extends TileEntityCommon implements IChargeConductor {
    Charge charge = new Charge(this);
    public byte heat = 0;
    public static final byte maxHeat = 32;

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.HEATER;
    }
    
    @Override
    public Icon getIcon(ForgeDirection dir) {
        return BlockIcons.heater_spiral;
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
    public boolean handleMessageFromServer(int messageType, DataInputStream input) throws IOException {
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
        if (here.isPowered()) {
            charge.update();
            return;
        }
        long now = worldObj.getTotalWorldTime() + here.seed();
        int rate = 4;
        long i = now % rate;
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
        int recurs = 0, action = 0;
        for (Coord c : here.getRandomNeighborsAdjacent()) {
            TileEntity te = c.getTE();
            if (te == null) {
                continue;
            }
            if (te instanceof TileEntityHeater) {
                recurs++;
                continue;
            }
            if (sendHeat(te, false)) {
                action++;
                if (heat <= 0) {
                    return;
                }
            }
        }
        if (recurs > 0 && action == 0) {
            for (Coord c : here.getRandomNeighborsAdjacent()) {
                TileEntity te = c.getTE();
                if (te == null) {
                    continue;
                }
                if (te instanceof TileEntityHeater) {
                    sendHeat(te, true);
                    continue;
                }
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
    
    boolean sendHeat(TileEntity te, boolean canRecurse) {
        if (te instanceof TileEntityExtension) {
            te = ((TileEntityExtension) te).getParent();
            if (te == null) {
                return false;
            }
        }
        if (te instanceof TileEntityFurnace) {
            TileEntityFurnace furnace = (TileEntityFurnace) te;
            if (!TEF_canSmelt(furnace)) {
                return false;
            }
            ProxiedHeatingResult pf = new ProxiedHeatingResult(new Coord(te), furnace.furnaceBurnTime, furnace.furnaceCookTime);
            furnace.furnaceBurnTime = pf.burnTime;
            furnace.furnaceCookTime = Math.min(pf.cookTime, 200 - 1);
            BlockFurnace.updateFurnaceBlockState(furnace.furnaceCookTime > 0, worldObj, te.xCoord, te.yCoord, te.zCoord);
            return true;
        }
        if (te instanceof TileEntitySlagFurnace) {
            TileEntitySlagFurnace furnace = (TileEntitySlagFurnace) te;
            if (!furnace.canSmelt()) {
                return false;
            }
            ProxiedHeatingResult pf = new ProxiedHeatingResult(new Coord(te), furnace.furnaceBurnTime, furnace.furnaceCookTime);
            furnace.furnaceBurnTime = pf.burnTime;
            furnace.furnaceCookTime = pf.cookTime;
            return true;
        } 
        if (te instanceof TileEntityCrystallizer) {
            TileEntityCrystallizer crys = (TileEntityCrystallizer) te;
            if (!crys.needHeat()) {
                return false;
            }
            crys.heat++;
            heat--;
            return true;
        } 
        if (rpAlloyFurnace != null && rpAlloyFurnace.isInstance(te)) {
            Exception err = null;
            try {
                Field burntimeField = rpAlloyFurnace.getField("burntime");
                //Field totalburnField = rpAlloyFurnace.getField("totalburn");
                ProxiedHeatingResult pf = new ProxiedHeatingResult(new Coord(te), burntimeField.getInt(te), 0 /* Elo doesn't want speedy. :( */);
                burntimeField.setInt(te, pf.burnTime);
                return true;
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
            return false;
        }
        if (te instanceof TileEntityGreenware) {
            TileEntityGreenware teg = (TileEntityGreenware) te;
            ClayState state = teg.getState();
            if (state == ClayState.DRY || state == ClayState.UNFIRED_GLAZED) {
                teg.totalHeat += 1;
                heat -= 3;
                return true;
            }
            if (state == ClayState.WET) {
                teg.lastTouched += 1;
                heat -= 6;
                return true;
            }
            return false;
        }
        if (canRecurse && te instanceof TileEntityHeater) {
            int to_take = 2;
            if (heat < to_take) {
                return false;
            }
            TileEntityHeater heater = ((TileEntityHeater) te);
            for (Coord c : heater.getCoord().getRandomNeighborsAdjacent()) {
                TileEntity it = c.getTE();
                if (it == this || it == null || it instanceof TileEntityHeater) {
                    continue;
                }
                if (heater.sendHeat(it, false)) {
                    heat -= to_take;
                    return true;
                }
            }
            return false;
        }
        return false;
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
