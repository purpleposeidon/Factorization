package factorization.src;

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
import net.minecraft.src.Profiler;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntityChest;
import net.minecraft.src.World;
import net.minecraft.src.mod_Factorization;
import net.minecraft.src.forge.ISidedInventory;
import factorization.src.NetworkFactorization.MessageType;

public class TileEntityRouter extends TileEntityFactorization {
	// TODO: Save these in NBT!
	public int maxSearchPerTick = 16;

	// save these guys/share with client
	public boolean moveFullStack = false;
	public int target_side;
	public int target_slot; // If negative, use target_side instead. The old value is saved as the inverse. For some reason.
	public boolean is_input;
	public boolean match_to_visit = false;
	public String match;
	ItemStack buffer;

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
			if (moveFullStack) {
				in_count += 5;
			} else {
				in_count += 3;
			}
		} else {
			if (moveFullStack) {
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
			return true;
		}
		if (ent instanceof IInventory) {
			IInventory inv = openInventory(ent);
			if (actOn(inv)) {
				// router.drawActive(1);
				// Net Optimization: Have lastSeenAt increment draw_active
				draw_active += 1;
				Coord here = new Coord(ent);
				delayDistance = (int) here.distance(lastSeenAt);
				lastSeenAt = here;
				broadcastItem(MessageType.RouterLastSeen, null);
				return true;
			}
		}
		return false;
	}

	@Override
	void doLogic() {
		Profiler.startSection("router");
		needLogic();
		if (lastSeenAt == null) {
			lastSeenAt = getCoord();
		}
		if (frontier.size() == 0) {
			resetGraph();
		}

		if (delayDistance > 0) {
			delayDistance--;
			return;
		}

		int i = maxSearchPerTick;
		while (i-- != 0 && shouldUpdate() && frontier.size() > 0) {
			updateFrontier();
			if (delayDistance != 0) {
				break;
			}
		}
		Profiler.endSection();
	}

	TileEntity popFrontier() {
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

		tryInsert(here);

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
			if (moveFullStack) {
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

		if (moveFullStack) {
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
		if (Core.instance.routerBan == null) {
			//xu do mi tavla fo la .rAUterban.
			return false;
		}
		Matcher m = Core.instance.routerBan.matcher(name);
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
			for (int side = 0; side < 6; side++) {
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
		for (int side = 0; side < 6; side++) {
			int low = s.getStartInventorySide(side);
			int high = low + s.getSizeInventorySide(side);
			if (low <= slot && slot < high) {
				return true;
			}
		}
		return false;
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
				start = inv.getStartInventorySide(target_side);
				end = start + inv.getSizeInventorySide(target_side);
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
			// XXX: Should onInventoryChanged() happen at moveStack for both inventories?
			if (!legalSlot(t, slot)) {
				continue;
			}
			if (is_input) {
				if (moveStack(this, 0, t, slot)) {
					onInventoryChanged();
					t.onInventoryChanged();
					return true;
				}
			} else {
				if (moveStack(t, slot, this, 0)) {
					onInventoryChanged();
					t.onInventoryChanged();
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setInteger("target_side", target_side);
		tag.setInteger("use_slot", target_slot);
		tag.setBoolean("is_input", is_input);
		if (buffer != null) {
			tag.setTag("buffer", buffer.writeToNBT(new NBTTagCompound()));
		}
		if (match != null) {
			tag.setString("match", match);
		}
		tag.setBoolean("match_to_visit", match_to_visit);
		tag.setBoolean("move_full_stack", moveFullStack);
		tag.setInteger("max_search_per_tick", maxSearchPerTick);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		target_side = tag.getInteger("target_side");
		target_slot = tag.getInteger("use_slot");
		is_input = tag.getBoolean("is_input");
		if (tag.hasKey("buffer")) {
			buffer = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("buffer"));
		}
		match = tag.getString("match");
		match_to_visit = tag.getBoolean("match_to_visit");
		moveFullStack = tag.getBoolean("move_full_stack");
		if (tag.hasKey("max_search_per_tick")) {
			maxSearchPerTick = tag.getInteger("max_search_per_tick");
		}
	}

	@Override
	public int getSizeInventory() {
		return 1;
	}

	@Override
	public ItemStack getStackInSlot(int i) {
		if (i != 0) {
			return null;
		}
		return buffer;
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack itemstack) {
		if (i != 0) {
			return;
		}
		buffer = itemstack;
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
		default:
			return false;
		}
		if (need_share && mod_Factorization.instance.isCannonical(worldObj)) {
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
		if (all || messageType == MessageType.RouterMatch) {
			if (match == null) {
				match = "";
			}
			broadcastMessage(who, MessageType.RouterMatch, match);
		}
		if (all | messageType == MessageType.RouterMatchToVisit) {
			broadcastMessage(who, MessageType.RouterMatchToVisit, match_to_visit);
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
		mod_Factorization.network.broadcastMessage(who, getCoord(), messageType, items);
	}

	@Override
	public boolean handleMessageFromClient(int messageType, DataInput input) throws IOException {
		if (super.handleMessageFromClient(messageType, input)) {
			return true;
		}
		if (handleMessageFromAny(messageType, input)) {
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
		return false;
	}

	@Override
	int getLogicSpeed() {
		return 1;
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
		//		// XXX TODO: This is a terrible non-solution to work with frames.
		//		// Seems like we get infinite loops in isCoordConnected if we don't do
		//		// this. Maybe we should have our coordinates's origin be delta-based?
		//		resetGraph();
	}

	@Override
	public int getStartInventorySide(int side) {
		return 0;
	}

	@Override
	public int getSizeInventorySide(int side) {
		return 1;
	}
}
