package factorization.sockets.fanturpeller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import net.minecraft.inventory.IInventory;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.IFluidHandler;
import factorization.api.Charge;
import factorization.api.Coord;
import factorization.api.IChargeConductor;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.FactoryType;
import factorization.sockets.ISocketHolder;
import factorization.sockets.TileEntitySocketBase;

public class SocketFanturpeller_BLEH extends TileEntitySocketBase implements IChargeConductor { //NORELEASE: Bleh.
    Charge charge = new Charge(this);
    boolean isSucking = true;
    byte target_speed = 3;
    float fanω; // because I can.
    FanAction action = null;

    transient float fanRotation, prevFanRotation;

    static Class<? extends FanAction>[] acters = new Class[] { null, PumpLiquids.class, BlowEntities.class,
            MixCrafting.class, GeneratePower.class };

    private byte getActorKey() {
        if (action == null)
            return 0;
        for (byte i = 1; i < acters.length; i++) {
            if (action.getClass() == acters[i]) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        charge = data.as(Share.PRIVATE, "charge").put(charge);
        isSucking = data.as(Share.MUTABLE_INDIRECT, "suck").put(isSucking);
        target_speed = data.as(Share.MUTABLE_INDIRECT, "target_speed").putByte(target_speed);
        fanω = data.as(Share.MUTABLE_INDIRECT, "fanw").putFloat(fanω);

        byte actIdx = data.as(Share.PRIVATE, "actid").put(getActorKey());
        if (data.isReader() && actIdx != 0 && actIdx < acters.length) {
            Class<? extends FanAction> actionClass = acters[actIdx];
            action = null;
            try {
                action = actionClass.getDeclaredConstructor(SocketFanturpeller_BLEH.class).newInstance(this);
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
        if (action != null) {
            action.serialize("", data);
        }
        return this;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SOCKET_FANTURPELLER;
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public String getInfo() {
        if (action == null)
            return null;
        return action.getInfo();
    }

    @Override
    public Charge getCharge() {
        return charge;
    }

    @Override
    public void updateEntity() {
        charge.update();
        super.updateEntity();
    }

    // And now the real work begins.

    private abstract class FanAction implements IDataSerializable {
        abstract void tick(ISocketHolder socket, Coord coord, boolean had_neighbor_change);

        void unset() {
        }

        String getInfo() {
            return null;
        }
    }
    
    void setAction(FanAction action) {
        if (this.action != null) {
            this.action.unset();
        }
        this.action = action;
    }

    void pickFanAction(Coord coord, boolean powered, boolean onServo) {
        /*
         * Possible actions: - move liquids (IFluidContainers/world). Source and
         * destination must be able to hold liquids (or have liquids spilled
         * into them, in the case of air) - suck in entities; or blow out items
         * and entities. Front must be clear. Back may have an inventory. - mix
         * shapeless recipes. Must have an inventory, front and back. - generate
         * power. Must have a redstone signal, and front must be clear, and back
         * must contain a gas as a block or as an inventory
         */
        final Coord front = coord.add(facing);
        final Coord back = coord.add(facing.getOpposite());

        if (!front.blockExists() || !back.blockExists()) {
            action = null;
            return;
        }

        final Coord source = isSucking ? front : back;
        final Coord destination = isSucking ? back : front;

        // BEGIN MACRO-GENERATED CODE
        // The real source for this is in fanmacro.py, which should be located in the same folder as this file.
        boolean need_PumpLiquids = false;
        boolean need_GeneratePower = false;
        boolean need_BlowEntities = false;
        boolean need_MixCrafting = false;
        if (!powered && !onServo && (isLiquid(source) || hasTank(source)) && (isLiquid(destination) || hasTank(destination) || isClear(destination))) {
            need_PumpLiquids = true;
        }
        if (powered && (isLiquid(source) || hasTank(source)) && isClear(destination)) {
            need_GeneratePower = true;
        }
        if (isClear(front) && (isClear(back) || hasInv(back))) {
            need_BlowEntities = true;
        }
        if (hasInv(front) && hasInv(back)) {
            need_MixCrafting = true;
        }
        if (need_PumpLiquids && action instanceof PumpLiquids) return;
        if (need_GeneratePower && action instanceof GeneratePower) return;
        if (need_BlowEntities && action instanceof BlowEntities) return;
        if (need_MixCrafting && action instanceof MixCrafting) return;
        if (need_PumpLiquids) {
            setAction(new PumpLiquids());
            return;
        }
        if (need_GeneratePower) {
            setAction(new GeneratePower());
            return;
        }
        if (need_BlowEntities) {
            setAction(new BlowEntities());
            return;
        }
        if (need_MixCrafting) {
            setAction(new MixCrafting());
            return;
        }
        // END MACRO-GENERATED CODE
    }

    boolean isLiquid(Coord at) {
        return at.getBlock() instanceof IFluidBlock;
    }

    boolean hasTank(Coord at) {
        return at.getTE(IFluidHandler.class) != null;
    }

    boolean hasInv(Coord at) {
        return at.getTE(IInventory.class) != null;
    }

    boolean isClear(Coord at) {
        return at.isReplacable();
    }

    transient boolean needActionCheck = true;
    @Override
    public void onNeighborTileChanged(int tilex, int tiley, int tilez) {
        needActionCheck = true;
    }
    
    float getTargetSpeed() {
        switch (target_speed) {
        default:
        case 0: return 0;
        case 1: return 30;
        case 2: return 20;
        case 3: return 10;
        }
    }
    
    boolean shouldDoWork() {
        if (target_speed == 0) return false;
        float ts = getTargetSpeed();
        if (fanω >= ts) return true;
        if (ts > fanω + 10) return false;
        return (ts - fanω)/10.0F > rand.nextFloat(); 
    }
    
    @Override
    public void genericUpdate(ISocketHolder socket, Coord coord, boolean powered) {
        prevFanRotation = fanRotation;
        boolean had_change = false;
        if (action == null || needActionCheck) {
            pickFanAction(coord, powered, socket != this);
            needActionCheck = false;
            had_change = true;
        }
        if (action != null) {
            action.tick(socket, coord, had_change);
        }
        if (action == null) {
            target_speed = 0;
        }
        float ts = getTargetSpeed();
        if (fanω > ts) {
            float dω = (fanω/5) - 1;
            if (fanω - dω < ts) {
                fanω = ts;
            } else {
                fanω -= dω;
            }
        } else if (fanω < ts) {
            if (charge.deplete(target_speed) >= target_speed) {
                fanω++;
            } else {
                fanω = Math.max(0, fanω - 1);
            }
        }
        fanRotation += isSucking ? fanω : -fanω;
    }

    // And now the extra-real work begins. o_o
    
    class PumpLiquids extends FanAction {
        class PumpCursor implements IDataSerializable {
            Coord at = new Coord(SocketFanturpeller_BLEH.this);
            ForgeDirection dir;
            byte speed = 8;
            boolean extracting;

            @Override
            public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
                at = data.as(Share.PRIVATE, prefix + "at").put(at);
                dir = data.as(Share.PRIVATE, prefix + "dir").putEnum(dir);
                speed = data.as(Share.PRIVATE, prefix + "speed").putByte(speed);
                extracting = data.as(Share.PRIVATE, prefix + "ext").putBoolean(extracting);
                return this;
            }
            
            boolean tick() {
                if (!at.blockExists()) return true;
                Fluid origFluid = at.getFluid();
                if (origFluid == null) return true;
                boolean boing = false;
                for (int i = 0; i < speed; i++) {
                    at.adjust(dir);
                    if (!at.blockExists()) {
                        at.adjust(dir.getOpposite());
                        boing = true;
                        break;
                    }
                    final Fluid hereFluid = at.getFluid();
                    if (hereFluid == origFluid) {
                        continue; //Just keep swimming
                    }
                    if (extracting) {
                        //here needs to be empty above
                    } else {
                        //here needs to be air
                    }
                    if ((hereFluid != origFluid && hereFluid != null) || !at.isReplacable()) {
                        //something we can't cross. Bounce off.
                        at.adjust(dir.getOpposite());
                        boing = true;
                        break;
                    }
                    break;
                }
                //pick a new direction
                if (boing) {
                    dir = ForgeDirection.getOrientation((dir.ordinal() + 1 + rand.nextInt(6)) % 6);
                } else {
                    dir = ForgeDirection.getOrientation(rand.nextInt(6));
                }
                return false;
            }
        }
        
        ArrayList<PumpCursor> cursors = new ArrayList();
        short sleep_ticks = 20;
        FluidTank buffer = new FluidTank(FluidContainerRegistry.BUCKET_VOLUME);
        byte transferMode = -1;
        
        @Override
        public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
            sleep_ticks = data.as(Share.PRIVATE, "opcount").putShort(sleep_ticks);
            int count = cursors.size();
            count = data.as(Share.PRIVATE, "count").putInt(count);
            transferMode = data.as(Share.PRIVATE, "transfermode").putByte(transferMode);
            if (data.isNBT()) { //... :| oh well. Probably won't happen again.
                if (data.isReader()) {
                    buffer.writeToNBT(data.getTag());
                } else {
                    buffer.readFromNBT(data.getTag());
                }
            }
            if (data.isWriter()) {
                int i = 0;
                for (PumpCursor pc : cursors) {
                    data.as(Share.PRIVATE, "c" + i).put(pc);
                    i++;
                }
            } else {
                cursors.clear();
                for (int i = 0; i < count; i++) {
                    cursors.add(data.as(Share.PRIVATE, "c" + count).put(new PumpCursor()));
                }
            }
            return this;
        }
        
        byte calculateTransferMode() {
            final Coord coord = new Coord(SocketFanturpeller_BLEH.this);
            final Coord front = coord.add(facing);
            final Coord back = coord.add(facing.getOpposite());
            final Coord source = isSucking ? front : back;
            final Coord destination = isSucking ? back : front;
            return (byte) ((isLiquid(source) ? 2 : 0) | (isLiquid(destination) ? 1 : 0));
        }
        
        void spawnCursor() {
            ForgeDirection go = facing;
            if (transferMode == 3) {
                if (rand.nextBoolean()) {
                    go = go.getOpposite();
                }
            } else if (transferMode == 2) {
                go = go.getOpposite();
            }
            PumpCursor pc = new PumpCursor();
            
            pc.dir = go;
            pc.at.adjust(pc.dir);
            
            cursors.add(pc);
        }

        @Override
        void tick(ISocketHolder socket, Coord coord, boolean had_neighbor_change) {
            if (!shouldDoWork()) {
                return;
            }
            if (transferMode == -1) {
                transferMode = calculateTransferMode();
            } else if (had_neighbor_change) {
                byte new_mode = calculateTransferMode();
                if (new_mode != transferMode) {
                    if (new_mode != 3) {
                        //The only allowable improvement is to go from having 1 to 2
                        action = null;
                        return;
                    }
                    transferMode = new_mode;
                }
            }
            if (sleep_ticks-- <= 0) {
                short ts = (short) getTargetSpeed();
                sleep_ticks = (short) (2*(30 - ts));
                if (ts < cursors.size()) {
                    spawnCursor();
                }
            }
            for (Iterator<PumpCursor> it = cursors.iterator(); it.hasNext();) {
                PumpCursor cur = it.next();
                if (cur.tick()) {
                    it.remove();
                }
            }
        }

    }

    class BlowEntities extends FanAction {
        @Override
        public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
            return this;
        }

        @Override
        void tick(ISocketHolder socket, Coord coord, boolean had_neighbor_change) {
            // TODO Auto-generated method stub

        }

    }

    class MixCrafting extends FanAction {
        @Override
        public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
            return this;
        }

        @Override
        void tick(ISocketHolder socket, Coord coord, boolean had_neighbor_change) {
            // TODO Auto-generated method stub

        }
    }

    class GeneratePower extends FanAction {
        @Override
        public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
            return this;
        }

        @Override
        void tick(ISocketHolder socket, Coord coord, boolean had_neighbor_change) {
            // TODO Auto-generated method stub

        }
    }

}
