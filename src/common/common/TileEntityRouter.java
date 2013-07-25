package factorization.common;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.regex.Matcher;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.common.FactorizationUtil.FzInv;
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
    private HashSet<TileEntity> visited = new HashSet(); // All of the cells that we've found in the current run
    private LinkedList<TileEntity> frontier = new LinkedList<TileEntity>(); // Cells that we haven't visited yet. (subset of visited). NOTE: We really do want this to be ordered
    private static Random random = new Random();
    int delayDistance; // wait some ticks when moving between far locations
    private int ticksSinceLastSpam = 9999;

    public TileEntityRouter() {
        super();
        // default to targeting the top side; this is the typical input side.
        target_side = 1;
        target_slot = ~0;
        is_input = true;
        buffer = null;
        match = new String("");
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
    
    @Override
    public Icon getIcon(ForgeDirection dir) {
        switch (dir) {
        case UP: return BlockIcons.router$top;
        case DOWN: return BlockIcons.router$bottom;
        case NORTH: return BlockIcons.router$north.get(this);
        case SOUTH: return BlockIcons.router$south.get(this);
        case EAST: return BlockIcons.router$east.get(this);
        case WEST: return BlockIcons.router$west.get(this);
        default: return super.getIcon(dir);
        }
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

    boolean tryInsert(TileEntity ent) {
        if (ent.isInvalid()) {
            //Should help with frames/unloading chunks
            return false; //This is correct. upgradeThorough would have it hang here.
        }
        if (ent instanceof IInventory) {
            if (actOn((IInventory) ent)) {
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
    
    Coord getEjectCoord() {
        if (!upgradeEject) {
            return null;
        }
        return getCoord().add(ForgeDirection.getOrientation(eject_direction).getOpposite());
    }

    void eject() {
        if (last_eject != 0) {
            last_eject--;
        }
        last_eject = 20;
        Coord target = getEjectCoord();
        IInventory target_inv = target.getTE(IInventory.class);
        if (target_inv == null) {
            return;
        }
        buffer = FactorizationUtil.openInventory(target_inv, eject_direction).push(buffer);
    }

    @Override
    void doLogic() {
        Core.profileStart("router");
        needLogic();
        ticksSinceLastSpam++;
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
        Core.profileEnd();
    }

    TileEntity popFrontier() {
        if (upgradeSpeed) {
            return frontier.remove();
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

        if (here != this && tryInsert(here) && upgradeThorough) {
            resetGraph();
            frontier.add(here);
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
        if (worldObj.getBlockPowerInput(xCoord, yCoord, zCoord) > 0) {
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

    boolean isIInventoryBanned(String name) {
        if (FzConfig.routerBan == null) {
            //xu do mi tavla fo la .rAUterban.
            return false;
        }
        Matcher m = FzConfig.routerBan.matcher(name);
        return m.matches();
    }

    public String getIInventoryName(IInventory t) {
        String invName = t.getInvName();
        if (invName == null || invName.length() == 0) {
            String className = t.getClass().getSimpleName();
            if (className == null || className.length() == 0) {
                return t.toString();
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
        if (upgradeEject && t == getEjectCoord().getTE(IInventory.class)) {
            return false;
        }

        if (match == null || match.length() == 0 || upgradeMachineFilter == false) {
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

    boolean itemPassesInsertFilter(FzInv inv, ItemStack is) {
        if (!upgradeItemFilter) {
            return true;
        }
        int matching_count = 0;
        boolean empty_filter = true;
        for (int i = 0; i < filter.length; i++) {
            ItemStack here = filter[i];
            if (here == null) {
                continue;
            }
            empty_filter = false;
            if (FactorizationUtil.couldMerge(here, is)) {
                matching_count += here.stackSize;
            }
        }
        if (empty_filter) {
            return true;
        }
        for (int i = 0; i < inv.size(); i++) {
            ItemStack here = inv.get(i);
            if (here == null) {
                continue;
            }
            if (FactorizationUtil.couldMerge(here, is)) {
                matching_count -= here.stackSize;
            }
        }
        return matching_count > 0;
    }

    boolean itemPassesExtractFilter(ItemStack is) {
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
            if (FactorizationUtil.couldMerge(filter[i], is)) {
                return true;
            } else if (filter[i].itemID == is.itemID && is.getItem().isDamageable() && filter[i].isItemDamaged() && is.isItemDamaged()) {
                //a slightly damaged sword will match all other damaged swords. A new sword should not.
                return true;
            }
        }
        return hits == 0;
    }

    boolean actOn(IInventory t) {
        if (t == null) {
            return false;
        }
        if (t instanceof TileEntityRouter && !(upgradeMachineFilter && match != null && !match.isEmpty())) {
            // ignore ourselves!
            return false;
        }
        if (!matchIInventory(t)) {
            return false;
        }
        int start, end;
        FzInv inv;
        if (target_slot >= 0) {
            if (!FactorizationUtil.canAccessSlot(t, target_slot)) {
                return false;
            }
            if (target_slot >= t.getSizeInventory()) {
                return false;
            }
            start = target_slot;
            end = target_slot + 1;
            inv = new FactorizationUtil.PlainInvWrapper(t);
        } else {
            inv = FactorizationUtil.openInventory(t, target_side, false);
            if (inv == null) {
                return false;
            }
            start = 0;
            end = inv.size();
        }
        FzInv me = FactorizationUtil.openInventory(this, 0, false);
        int toMove = upgradeThroughput ? 64 : 1;
        for (int slot = start; slot < end; slot++) {
            if (is_input) {
                if (!itemPassesInsertFilter(inv, buffer)) {
                    return false;
                }
                if (me.transfer(0, inv, slot, toMove)) {
                    return true;
                }
            } else {
                if (!itemPassesExtractFilter(inv.get(slot))) {
                    continue;
                }
                if (inv.transfer(slot, me, 0, toMove)) {
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
        //Compat: We don't allow "extract from anywhere" anymore
        if (target_side == 6) {
            target_side = 1;
        }
        if (target_side == ~6) {
            target_side = ~1;
        }
        target_slot = tag.getInteger("use_slot");
        is_input = tag.getBoolean("is_input");
        if (tag.hasKey("buffer")) {
            //This is old & here for compat.
            buffer = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("buffer"));
        }
        else {
            readSlotsFromNBT(tag);
        }
        match = tag.getString("match");
        match_to_visit = tag.getBoolean("match_to_visit");
        eject_direction = tag.getInteger("eject_side");
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
        onInventoryChanged();
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
                if (ticksSinceLastSpam > 10) {
                    broadcastMessage(who, MessageType.RouterLastSeen, lastSeenAt.x, lastSeenAt.y, lastSeenAt.z);
                    ticksSinceLastSpam = 0;
                }
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
                //player.addChatMessage("No upgrade");
            }
        }
    }

    @Override
    public boolean handleMessageFromClient(int messageType, DataInputStream input) throws IOException {
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
    public boolean handleMessageFromServer(int messageType, DataInputStream input) throws IOException {
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
            upgradeEject = input.readBoolean();
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

    private static final int[] side_info = {0};

    @Override
    public int[] getAccessibleSlotsFromSide(int s) {
        return side_info;
    }
    
    @Override
    public boolean isItemValidForSlot(int s, ItemStack itemstack) {
        if (upgradeItemFilter && is_input) {
            return itemPassesExtractFilter(itemstack);
        }
        return s == 0;
    }
}
