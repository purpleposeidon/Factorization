package factorization.common;

/*
 * XXX TODO Mysterious Bugs:
 *   - infinite buckets. May be caused by heavy loads? May have fixed, but not sure
 */
import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.regex.Matcher;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IInventory;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntityChest;
import net.minecraft.src.World;
import net.minecraftforge.common.ISidedInventory;
import net.minecraftforge.common.Orientation;
import static net.minecraftforge.common.Orientation.*;
import factorization.api.Coord;
import factorization.common.NetworkFactorization.MessageType;

public class TileEntityRouter extends TileEntityFactorization {
    final public int maxSearchPerTick = 16;
    public int guiLastButtonSet = 0; //for the client

    // save these guys/share with client
    public boolean upgradeItemFilter = false, upgradeMachineFilter = false, upgradeSpeed = false,
            upgradeThorough = false, upgradeThroughput = false, upgradeEject = false;
    public int target_side;
    public int target_slot; // If negative, use target_side instead. The old value is saved as the inverse. For some reason.
    public boolean is_input;
    public boolean match_to_visit = false;
    public int eject_direction;
    public String match;
    ItemStack buffer;
    ItemStack filter[] = new ItemStack[9];

    Coord lastSeenAt; // where we put an item in last

    // Runtime
    private HashSet<TileEntity> visited; // All of the cells that we've found in the current run
    private ArrayList<TileEntity> frontier; // Cells that we haven't visited yet. (subset of visited). NOTE: We really do want this to be ordered
    private static Random random = new Random();
    int delayDistance; // wait some ticks when moving between far locations

    public TileEntityRouter() {
        super();
        // default to targeting the top side; this is the typical input side.
        target_side = 1;
        target_slot = ~0;
        is_input = true;
        buffer = null;
        match = new String("");

        visited = new HashSet();
        frontier = new ArrayList();
    }

    void resetGraph() {
        visited.clear();
        visited.add(this);
        frontier.add(this);
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.DarkIron;
    }

    void putParticles(World world) {
        if (lastSeenAt == null) {
            return;
        }

        if (draw_active == 0) {
            return;
        }

        int out_count = 1;
        int in_count = 1;
        if (is_input) {
            if (upgradeThroughput) {
                in_count += 5;
            } else {
                in_count += 3;
            }
        } else {
            if (upgradeThroughput) {
                out_count += 5;
            } else {
                in_count += 3;
            }
        }
        for (int i = out_count; i != 0; i--) {
            double px = xCoord + 0.5 + (random.nextFloat() / 2);
            double py = yCoord + 0.5 + (random.nextFloat() / 2);
            double pz = zCoord + 0.5 + (random.nextFloat() / 2);
            double vx = (lastSeenAt.x - px + random.nextFloat() / 2);
            double vy = (lastSeenAt.y - py + random.nextFloat() / 2);
            double vz = (lastSeenAt.z - pz + random.nextFloat() / 2);
            world.spawnParticle("portal", px, py, pz, vx, vy, vz);
        }
        for (int i = in_count; i != 0; i--) {
            double px = lastSeenAt.x + 0.5 + (random.nextFloat() / 2);
            double py = lastSeenAt.y + 0.5 + (random.nextFloat() / 2);
            double pz = lastSeenAt.z + 0.5 + (random.nextFloat() / 2);
            double vx = (xCoord - px + random.nextFloat() / 2);
            double vy = (yCoord - py + random.nextFloat() / 2);
            double vz = (zCoord - pz + random.nextFloat() / 2);
            world.spawnParticle("portal", px, py, pz, vx, vy, vz);
        }
    }

    private IInventory openInventory(TileEntity ent) {
        if (ent instanceof TileEntityChest) {
            IInventory chest = FactorizationUtil.openDoubleChest((TileEntityChest) ent);
            // null means it's a lower chest, but would keep it from
            // continuing, so return a convenient Router.
            return (chest == null) ? this : chest;
        }
        if (ent instanceof IInventory) {
            return (IInventory) ent;
        }
        return null;
    }

    boolean tryInsert(TileEntity ent) {
        if (ent.isInvalid()) {
            //Should help with frames/unloading chunks
            return false; //This is correct. upgradeThorough would have it hang here.
        }
        if (ent instanceof IInventory) {
            IInventory inv = openInventory(ent);
            if (actOn(inv)) {
                // Netcode Optimization: lastSeenAt increments draw_active
                // router.drawActive(1);
                draw_active += 1;
                Coord here = new Coord(ent);
                if (!upgradeSpeed) {
                    delayDistance = (int) here.distance(lastSeenAt);
                }
                else {
                    delayDistance = 1;
                }
                lastSeenAt = here;
                broadcastItem(MessageType.RouterLastSeen, null);
                return true;
            }
        }
        return false;
    }

    int last_eject = 0;
    void eject() {
        if (last_eject != 0) {
            last_eject--;
        }
        last_eject = 20;
        Coord target = getCoord().add(new CubeFace(eject_direction).toVector());
        IInventory inv = target.getTE(IInventory.class);
        if (inv == null) {
            return;
        }
        int start = 0, end = inv.getSizeInventory();
        if (inv instanceof ISidedInventory) {
            ISidedInventory isi = (ISidedInventory) inv;
            Orientation access_side = Orientation.getOrientation(CubeFace.oppositeSide(eject_direction));
            start = isi.getStartInventorySide(access_side);
            end = start + isi.getSizeInventorySide(access_side);
            if (start == end) {
                return;
            }
        }
        for (int slot = start; slot < end; slot++) {
            if (moveStack(this, 0, target.getTE(IInventory.class), slot)) {
                return;
            }
        }
    }

    @Override
    void doLogic() {
        Core.proxy.getProfiler().startSection("router");
        needLogic();
        if (lastSeenAt == null) {
            lastSeenAt = getCoord();
        }
        if (frontier.size() == 0) {
            resetGraph();
        }
        if (upgradeEject && !is_input && buffer != null && buffer.stackSize > 0) {
            eject();
        }

        if (delayDistance > 0) {
            delayDistance--;
            return;
        }

        int i = maxSearchPerTick;
        if (upgradeSpeed) {
            i *= 2;
        }
        while (i-- != 0 && shouldUpdate() && frontier.size() > 0) {
            updateFrontier();
            if (delayDistance != 0) {
                break;
            }
        }
        Core.proxy.getProfiler().endSection();
    }

    TileEntity popFrontier() {
        if (upgradeSpeed) {
            return frontier.remove(0);
        }
        int closestDistance = Integer.MAX_VALUE;
        int closestIndex = 0;
        final int end = Math.min(8, frontier.size());
        for (int i = 0; i < end; i++) {
            int d = lastSeenAt.distanceSq(new Coord(frontier.get(i)));
            if (d < closestDistance) {
                closestDistance = d;
                closestIndex = i;
            }
            if (closestDistance == 1) {
                return frontier.remove(i);
            }
        }
        return frontier.remove(closestIndex);
    }

    void updateFrontier() {
        if (frontier.size() == 0) {
            return;
        }
        TileEntity here = popFrontier();

        if (tryInsert(here) && upgradeThorough) {
            return;
        }

        // Can't put anything else here. We move on
        for (Coord neighbor : new Coord(here).getNeighborsAdjacent()) {
            TileEntity ent = neighbor.getTE();
            if (!(ent instanceof IInventory)) {
                continue; //can't just fetch an IInventory.
            }
            if (!visited.contains(ent)) {
                if (match_to_visit && !matchIInventory((IInventory) ent)) {
                    continue;
                }
                frontier.add(ent);
                visited.add(ent);
            }
        }
    }

    /**
     * If the router can possibly do anything
     */
    boolean shouldUpdate() {
        if (worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord) || worldObj.isBlockGettingPowered(xCoord, yCoord, zCoord)) {
            return false;
        }
        if (is_input) {
            return buffer != null;
        } else {
            if (buffer == null) {
                return true;
            }

            return buffer.stackSize < buffer.getMaxStackSize();
        }
    }

    boolean moveStack(IInventory src, int src_slot, IInventory dest, int dest_slot) {
        // move as much as possible from src's slot to dest's slot
        ItemStack srcStack = src.getStackInSlot(src_slot);
        ItemStack destStack = dest.getStackInSlot(dest_slot);
        if (srcStack == null || srcStack.stackSize <= 0) {
            return false;
        }
        if (destStack == null) {
            if (upgradeThroughput) {
                // Ha! Easy swap!
                src.setInventorySlotContents(src_slot, null);
                dest.setInventorySlotContents(dest_slot, srcStack);
                return true;
            } else {
                // leave it as 0 for us to fill up
                // The two returns below shouldn't be a problem, as it'll be equal, and there'll be room
                destStack = srcStack.copy();
                destStack.stackSize = 0;
            }
        }
        if (!srcStack.isItemEqual(destStack)) {
            return false;
        }
        if (destStack.stackSize >= destStack.getMaxStackSize()) {
            return false;
        }

        if (upgradeThroughput) {
            int movable = destStack.getMaxStackSize() - destStack.stackSize;
            if (movable > srcStack.stackSize) {
                // dest takes an incomplete stack
                destStack.stackSize += srcStack.stackSize;
                src.setInventorySlotContents(src_slot, null);
                dest.setInventorySlotContents(dest_slot, destStack);
                return true;
            }
            // partial move
            srcStack.splitStack(movable);
            destStack.stackSize += movable;
            if (srcStack.stackSize == 0) {
                srcStack = null;
            }
            src.setInventorySlotContents(src_slot, srcStack);
            dest.setInventorySlotContents(dest_slot, destStack);
        } else {
            srcStack.stackSize--;
            if (srcStack.stackSize <= 0) {
                srcStack = null;
            }
            destStack.stackSize++;
            src.setInventorySlotContents(src_slot, srcStack);
            dest.setInventorySlotContents(dest_slot, destStack);
        }

        return true;
    }

    boolean isIInventoryBanned(String name) {
        if (Core.routerBan == null) {
            //xu do mi tavla fo la .rAUterban.
            return false;
        }
        Matcher m = Core.routerBan.matcher(name);
        return m.matches();
    }

    public String getIInventoryName(IInventory t) {
        //NOTE: This seems to have troubles with IC TEs? Maybe just on SMP?
        String invName = t.getInvName();
        if (invName == null || invName.length() == 0) {
            String className = t.getClass().getSimpleName();
            if (className == null || className.length() == 0) {
                return null;
            }
            return className.toLowerCase();
        }
        return invName;
    }

    boolean matchIInventory(IInventory t) {
        if (t == null) {
            return false;
        }
        String invName = getIInventoryName(t);
        if (invName == null || isIInventoryBanned(invName)) {
            return false;
        }

        if (match == null || match.length() == 0) {
            return true;
        }

        invName = invName.toLowerCase();
        for (String comp : match.split("\\|")) {
            comp = comp.toLowerCase();
            if (comp.startsWith("!")) {
                comp = comp.replaceFirst("!", "");
                if (invName.startsWith(comp)) {
                    return false;
                }
            } else {
                if (invName.startsWith(comp)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean isLocked(IInventory t) {
        // If we can't access anything via ISidedInventory means, don't allow it
        if (t instanceof ISidedInventory) {
            int free_count = 0;
            ISidedInventory inv = (ISidedInventory) t;
            for (Orientation side : Orientation.values()) {
                // check each side
                if (inv.getSizeInventorySide(side) != 0) {
                    free_count++;
                }
            }
            return free_count == 0;
        }
        return false;
    }

    boolean legalSlot(IInventory t, int slot) {
        if (!(t instanceof ISidedInventory)) {
            return true;
        }
        ISidedInventory s = (ISidedInventory) t;
        for (Orientation side : Orientation.values()) {
            int low = s.getStartInventorySide(side);
            int high = low + s.getSizeInventorySide(side);
            if (low <= slot && slot < high) {
                return true;
            }
        }
        return false;
    }

    boolean itemPassesFilter(ItemStack is) {
        if (!upgradeItemFilter) {
            return true;
        }
        if (is == null) {
            return false;
        }
        int hits = 0;
        for (int i = 0; i < filter.length; i++) {
            if (filter[i] == null) {
                continue;
            }
            hits++;
            if (filter[i].isItemEqual(is)) {
                //NOTE: Ignores NBT data. I think this will be more useful.
                return true;
            } else if (filter[i].itemID == is.itemID) {
                //a slightly damaged sword will match all other damaged swords. A new sword should not.
                if (filter[i].isItemDamaged() && is.isItemDamaged()) {
                    return true;
                }
            }
        }
        return hits == 0;
    }

    boolean actOn(IInventory t) {
        if (t == null) {
            return false;
        }
        if (t instanceof TileEntityRouter) {
            // ignore ourselves!
            return false;
        }
        if (!matchIInventory(t)) {
            return false;
        }
        int start, end;
        if (target_slot < 0) {
            // Get/Put from one of the sides
            if (t instanceof ISidedInventory && target_side < 6 && target_side >= 0) {
                ISidedInventory inv = (ISidedInventory) t;
                start = inv.getStartInventorySide(Orientation.getOrientation(target_side));
                end = start + inv.getSizeInventorySide(Orientation.getOrientation(target_side));
            } else {
                start = 0;
                end = t.getSizeInventory();
            }

        } else {
            if (isLocked(t)) {
                return false;
            }
            start = target_slot;
            end = target_slot + 1;
            if (start >= t.getSizeInventory()) {
                return false;
            }
        }
        for (int slot = start; slot < end; slot++) {
            // XXX: Should onInventoryChanged() happen at moveStack for both inventories? Probably.
            if (!legalSlot(t, slot)) {
                continue;
            }
            //So, about that item filtering...
            if (is_input) {
                //Filtering the input doesn't sound useful.
                //XXX TODO: Maybe do nothing if buffer doesn't match the filter? That might be useful for a very complicated auto-processor
                if (moveStack(this, 0, t, slot)) {
                    onInventoryChanged();
                    t.onInventoryChanged();
                    return true;
                }
            } else {
                if (!itemPassesFilter(t.getStackInSlot(slot))) {
                    continue;
                }
                if (moveStack(t, slot, this, 0)) {
                    onInventoryChanged();
                    t.onInventoryChanged();
                    return true;
                }
            }
        }
        return false;
    }

    void verifyUpgrades() {
        if (!upgradeItemFilter) {
            for (int i = 0; i < filter.length; i++) {
                filter[i] = null;
            }
        }
        if (!upgradeMachineFilter) {
            match_to_visit = false;
            match = null;
        }

    }

    @Override
    public void dropContents() {
        super.dropContents(); //this takes care of buffer & filter
        ArrayList<ItemStack> toDrop = new ArrayList();
        if (upgradeItemFilter) {
            toDrop.add(new ItemStack(Core.registry.router_item_filter));
        }
        if (upgradeMachineFilter) {
            toDrop.add(new ItemStack(Core.registry.router_machine_filter));
        }
        if (upgradeSpeed) {
            toDrop.add(new ItemStack(Core.registry.router_speed));
        }
        if (upgradeThorough) {
            toDrop.add(new ItemStack(Core.registry.router_thorough));
        }
        if (upgradeThroughput) {
            toDrop.add(new ItemStack(Core.registry.router_throughput));
        }
        if (upgradeEject) {
            toDrop.add(new ItemStack(Core.registry.router_eject));
        }
        Coord here = getCoord();
        for (ItemStack is : toDrop) {
            FactorizationUtil.spawnItemStack(here, is);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        verifyUpgrades();
        tag.setInteger("target_side", target_side);
        tag.setInteger("use_slot", target_slot);
        tag.setBoolean("is_input", is_input);
        writeSlotsToNBT(tag);
        if (match != null) {
            tag.setString("match", match);
        }
        tag.setBoolean("match_to_visit", match_to_visit);
        tag.setInteger("eject_side", eject_direction);

        tag.setBoolean("upgrade_item_filter", upgradeItemFilter);
        tag.setBoolean("upgrade_machine_filter", upgradeMachineFilter);
        tag.setBoolean("upgrade_speed", upgradeSpeed);
        tag.setBoolean("upgrade_thorough", upgradeThorough);
        tag.setBoolean("upgrade_throughput", upgradeThroughput);
        tag.setBoolean("upgrade_eject", upgradeEject);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        target_side = tag.getInteger("target_side");
        target_slot = tag.getInteger("use_slot");
        is_input = tag.getBoolean("is_input");
        if (tag.hasKey("buffer")) {
            //This is old!
            buffer = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("buffer"));
        }
        else {
            readSlotsFromNBT(tag);
        }
        match = tag.getString("match");
        match_to_visit = tag.getBoolean("match_to_visit");
        upgradeItemFilter = tag.getBoolean("upgrade_item_filter");
        upgradeMachineFilter = tag.getBoolean("upgrade_machine_filter");
        upgradeSpeed = tag.getBoolean("upgrade_speed");
        upgradeThorough = tag.getBoolean("upgrade_thorough");
        upgradeThroughput = tag.getBoolean("upgrade_throughput");
        upgradeEject = tag.getBoolean("upgrade_eject");
    }

    @Override
    public boolean takeUpgrade(ItemStack is) {
        ItemMachineUpgrade upgrade = (ItemMachineUpgrade) is.getItem();
        if (upgrade == Core.registry.router_item_filter) {
            if (upgradeItemFilter)
                return false;
            upgradeItemFilter = true;
        }
        else if (upgrade == Core.registry.router_machine_filter) {
            if (upgradeMachineFilter)
                return false;
            upgradeMachineFilter = true;
        }
        else if (upgrade == Core.registry.router_speed) {
            if (upgradeSpeed)
                return false;
            upgradeSpeed = true;
        }
        else if (upgrade == Core.registry.router_thorough) {
            if (upgradeThorough)
                return false;
            upgradeThorough = true;
        }
        else if (upgrade == Core.registry.router_throughput) {
            if (upgradeThroughput)
                return false;
            upgradeThroughput = true;
        }
        else if (upgrade == Core.registry.router_eject) {
            if (upgradeEject)
                return false;
            upgradeEject = true;
        }
        else {
            return false;
        }
        return true;
    }

    @Override
    public int getSizeInventory() {
        return 10;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        if (i == 0) {
            return buffer;
        }
        int f = i - 1;
        if (f >= 0 && f < filter.length) {
            return filter[f];
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack) {
        if (i == 0) {
            buffer = itemstack;
            return;
        }
        int f = i - 1;
        if (f >= 0 && f < filter.length) {
            filter[f] = itemstack;
        }
    }

    @Override
    public String getInvName() {
        return "Item Router";
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.ROUTER;
    }

    public boolean handleMessageFromAny(int messageType, DataInput input) throws IOException {
        // target_side side
        // target_slot slot
        // is_input flag
        // match match

        int m;
        boolean b;
        boolean need_share;
        switch (messageType) {
        case MessageType.RouterTargetSide:
            m = input.readInt();
            need_share = (target_side != m);
            target_side = m;
            break;
        case MessageType.RouterSlot:
            m = input.readInt();
            need_share = (m != target_slot);
            target_slot = m;
            break;
        case MessageType.RouterIsInput:
            b = input.readBoolean();
            need_share = b != is_input;
            is_input = b;
            break;
        case MessageType.RouterMatch:
            String s = input.readUTF();
            need_share = match == null || !match.equals(s);
            match = s;
            break;
        case MessageType.RouterMatchToVisit:
            b = input.readBoolean();
            need_share = b != match_to_visit;
            match_to_visit = b;
            break;
        case MessageType.RouterEjectDirection:
            m = input.readInt();
            need_share = (m != eject_direction);
            eject_direction = m;
            break;
        default:
            return false;
        }
        if (need_share && !worldObj.isRemote) {
            broadcastItem(messageType, null);
        }
        return true;
    }

    @Override
    void sendFullDescription(EntityPlayer player) {
        super.sendFullDescription(player);
        broadcastItem(MessageType.ShareAll, player);
    }

    public void broadcastItem(int messageType, EntityPlayer who) {
        boolean all = (messageType == -1);
        if (all || messageType == MessageType.RouterTargetSide) {
            broadcastMessage(who, MessageType.RouterTargetSide, target_side);
        }
        if (all || messageType == MessageType.RouterSlot) {
            broadcastMessage(who, MessageType.RouterSlot, target_slot);
        }
        if (all || messageType == MessageType.RouterIsInput) {
            broadcastMessage(who, MessageType.RouterIsInput, is_input);
        }
        if (all || messageType == MessageType.RouterUpgradeState) {
            broadcastMessage(who, MessageType.RouterUpgradeState, upgradeItemFilter, upgradeMachineFilter, upgradeSpeed, upgradeThorough, upgradeThroughput, upgradeEject);
        }
        if (all || messageType == MessageType.RouterMatch) {
            if (match == null) {
                match = "";
            }
            broadcastMessage(who, MessageType.RouterMatch, match);
        }
        if (all || messageType == MessageType.RouterMatchToVisit) {
            broadcastMessage(who, MessageType.RouterMatchToVisit, match_to_visit);
        }
        if (all || messageType == MessageType.RouterEjectDirection) {
            broadcastMessage(who, MessageType.RouterEjectDirection, eject_direction);
        }
        //well, turns out 'all' doesn't actually mean we want to send them all.
        //Otherwise it'd spawn particles when the GUI's opened
        if (messageType == MessageType.RouterLastSeen) {
            if (lastSeenAt != null) {
                broadcastMessage(who, MessageType.RouterLastSeen, lastSeenAt.x, lastSeenAt.y, lastSeenAt.z);
            }
        }
    }

    public void broadcastMessage(EntityPlayer who, int messageType, Object... items) {
        Core.network.broadcastMessage(who, getCoord(), messageType, items);
    }

    public void removeUpgrade(int upgradeType, EntityPlayer player) {
        ItemStack drop = null;
        if (upgradeType == Core.registry.router_item_filter.upgradeId && upgradeItemFilter) {
            for (int i = 0; i < filter.length; i++) {
                ItemStack is = filter[i];
                if (is == null) {
                    continue;
                }
                player.inventory.addItemStackToInventory(is);
                filter[i] = null;
            }
            drop = new ItemStack(Core.registry.router_item_filter);
            upgradeItemFilter = false;
        }
        else if (upgradeType == Core.registry.router_machine_filter.upgradeId && upgradeMachineFilter) {
            drop = new ItemStack(Core.registry.router_machine_filter);
            upgradeMachineFilter = false;
        }
        else if (upgradeType == Core.registry.router_speed.upgradeId && upgradeSpeed) {
            drop = new ItemStack(Core.registry.router_speed);
            upgradeSpeed = false;
        }
        else if (upgradeType == Core.registry.router_thorough.upgradeId && upgradeThorough) {
            drop = new ItemStack(Core.registry.router_thorough);
            upgradeThorough = false;
        }
        else if (upgradeType == Core.registry.router_throughput.upgradeId && upgradeThroughput) {
            drop = new ItemStack(Core.registry.router_throughput);
            upgradeThroughput = false;
        }
        else if (upgradeType == Core.registry.router_eject.upgradeId && upgradeEject) {
            drop = new ItemStack(Core.registry.router_eject);
            upgradeEject = false;
        }
        verifyUpgrades();
        if (drop != null) {
            player.inventory.addItemStackToInventory(drop);
        }
        else {
            if (!worldObj.isRemote) {
                player.addChatMessage("No upgrade");
            }
        }
    }

    @Override
    public boolean handleMessageFromClient(int messageType, DataInput input) throws IOException {
        if (super.handleMessageFromClient(messageType, input)) {
            return true;
        }
        if (handleMessageFromAny(messageType, input)) {
            return true;
        }
        if (messageType == NetworkFactorization.MessageType.RouterDowngrade) {
            removeUpgrade(input.readInt(), Core.network.getCurrentPlayer());
            return true;
        }
        return false;
    }

    @Override
    public boolean handleMessageFromServer(int messageType, DataInput input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (handleMessageFromAny(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.RouterLastSeen) {
            int x = input.readInt();
            int y = input.readInt();
            int z = input.readInt();
            lastSeenAt = new Coord(worldObj, x, y, z);
            drawActive(1);
            return true;
        }
        if (messageType == MessageType.RouterUpgradeState) {
            upgradeItemFilter = input.readBoolean();
            upgradeMachineFilter = input.readBoolean();
            upgradeSpeed = input.readBoolean();
            upgradeThorough = input.readBoolean();
            upgradeThroughput = input.readBoolean();
            verifyUpgrades();
            return true;
        }
        return false;
    }

    @Override
    int getLogicSpeed() {
        return 2;
    }

    @Override
    void makeNoise() {
        putParticles(worldObj);
        if (rand.nextFloat() > 0.9898 && lastSeenAt != null) {
            Sound.routerCluck.playAt(this);
        }
    }

    @Override
    public void invalidate() {
        //There should no longer be a problem here.
        //		// X-XX T-ODO: This is a terrible non-solution to work with frames.
        //		// Seems like we get infinite loops in isCoordConnected if we don't do
        //		// this. Maybe we should have our coordinates's origin be delta-based?
        //		resetGraph();
    }

    @Override
    public int getStartInventorySide(Orientation side) {
        return 0;
    }

    @Override
    public int getSizeInventorySide(Orientation side) {
        return 1;
    }
}
