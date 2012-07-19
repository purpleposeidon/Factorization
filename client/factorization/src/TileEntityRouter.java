package factorization.src;

/*
 * XXX TODO Mysterious Bugs:
 *   - infinite buckets. May be caused by heavy loads? May have fixed, but not sure
 */
import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
	// configs
	boolean moveFullStack = false;
	final int maxSearchPerTick = 16;

	// save these guys/share with client
	public int target_side;
	public int target_slot; // If negative, use target_side instead. The old value is saved as the inverse. For some reason.
	public boolean is_input;
	public boolean match_to_visit = false;
	public String match;
	ItemStack buffer;

	Coord lastSeenAt; // where we put an item in last

	// Runtime
	private HashMap<Coord, Coord> visited; // All of the cells that we've found
											// in the current run
	private ArrayList<Coord> frontier; // Cells that we haven't visited yet.
										// (subset of visited). NOTE: We really
										// do want this to be ordered

	int delayDistance; // wait some ticks when moving between far locations

	private static Coord[] adjacentOffsets = { new Coord(0, -1, 0),
			new Coord(0, +1, 0), new Coord(0, 0, -1), new Coord(0, 0, +1),
			new Coord(-1, 0, 0), new Coord(+1, 0, 0), };

	static private class Coord {
		public int x, y, z;

		Coord(int X, int Y, int Z) {
			x = X;
			y = Y;
			z = Z;
		}

		Coord add(Coord other) {
			return new Coord(x + other.x, y + other.y, z + other.z);
		}

		@Override
		public String toString() {
			return x + " " + z;
		}

		private IInventory openInventory(TileEntity ent, TileEntityRouter router) {
			if (ent instanceof TileEntityChest) {
				IInventory chest = FactorizationUtil.openDoubleChest((TileEntityChest) ent);
				// null means it's a lower chest, but would keep it from
				// continuing, so return a convenient Router.
				return (chest == null) ? router : chest;
			}
			return (IInventory) ent;
		}

		public boolean tryInsert(World w, TileEntityRouter router) {
			TileEntity ent = w.getBlockTileEntity(x, y, z);
			if (ent instanceof IInventory) {
				IInventory inv = openInventory(ent, router);
				if (router.actOn(inv)) {
					// router.drawActive(1);
					// Net Optimization: Have lastSeenAt increment draw_active
					router.draw_active += 1;
					router.delayDistance = distance(router.lastSeenAt);
					router.lastSeenAt = this;
					router.broadcastItem(MessageType.RouterLastSeen, null);
					return true;
				}
			}
			return false;
		}

		public int distance(Coord other) {
			if (other == null) {
				return 0;
			}
			int dx = x - other.x, dy = y - other.y, dz = z - other.z;
			return (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
		}

		public int distanceManhatten(Coord other) {
			if (other == null) {
				return 0;
			}
			int dx = x - other.x, dy = y - other.y, dz = z - other.z;
			return Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
		}

		@Override
		public int hashCode() {
			return (((x * 11) % 71) << 7) + ((z * 7) % 479) + y;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Coord) {
				Coord other = (Coord) obj;
				return x == other.x && y == other.y && z == other.z;
			}
			return false;
		}

		TileEntity getTileEntity(World w) {
			return w.getBlockTileEntity(x, y, z);
		}
	}

	public TileEntityRouter() {
		super();
		// default to targeting the top side; this is the typical input side.
		target_side = 1;
		target_slot = ~0;
		is_input = true;
		buffer = null;
		match = new String("");

		visited = new HashMap<Coord, Coord>();
		frontier = new ArrayList<Coord>();
	}

	void resetGraph() {
		visited.clear();
		Coord myCoord = new Coord(xCoord, yCoord, zCoord);
		visited.put(myCoord, myCoord);
		frontier.add(myCoord);
	}

	@Override
	public BlockClass getBlockClass() {
		return BlockClass.DarkIron;
	}

	static Random random = new Random();

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

	@Override
	void doLogic() {
		Profiler.startSection("router");
		needLogic();
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

	Coord popFrontier() {
		// TODO: Remove the nearest of 4 or so
		return frontier.remove(0);
	}

	void updateFrontier() {
		if (frontier.size() == 0) {
			return;
		}
		Coord here = popFrontier();

		if (!isCoordConnected(here)) {
			clearInvalidConnection(here);
			return;
		}

		here.tryInsert(worldObj, this);

		// Can't put anything else here. We move on
		for (Coord n : adjacentOffsets) {
			Coord neighbor = here.add(n);
			TileEntity ent = worldObj.getBlockTileEntity(neighbor.x,
					neighbor.y, neighbor.z);
			if (!(ent instanceof IInventory)) {
				continue;
			}
			if (!visited.containsKey(neighbor)) {
				frontier.add(neighbor);
				visited.put(neighbor, here);
			}
		}
	}

	boolean isCoordConnected(Coord here) {
		// make sure that the world hasn't changed under our trail
		// NOTE: Breaks under frames. Probably has to do with moving the router
		// under an inventory?
		while (true) {
			if (here == null) {
				return false;
			}
			if (!(worldObj.getBlockTileEntity(here.x, here.y, here.z) instanceof IInventory)) {
				return false;
			}
			if (here.x == xCoord && here.y == yCoord && here.z == zCoord) {
				return true;
			}
			here = visited.get(here);
		}
	}

	void clearInvalidConnection(Coord here) {
		// XXX NOTE O(n*log(n))
		// Shouldn't happen terribly often?
		while (true) {
			if (here == null) {
				return;
			}
			frontier.remove(here);
			here = visited.remove(here);
		}
	}

	/**
	 * If the router can possibly do anything
	 */
	boolean shouldUpdate() {
		if (worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord)
				|| worldObj.isBlockGettingPowered(xCoord, yCoord, zCoord)) {
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

	boolean moveStack(IInventory src, int src_slot, IInventory dest,
			int dest_slot) {
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
				// The two returns below shouldn't be a problem, as it'll be
				// equal, and there'll be room
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
			if (t instanceof ISidedInventory && target_side < 6
					&& target_side >= 0) {
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
			// XXX: Should onInventoryChanged() happen at moveStack for
			// both inventories?
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
		// hardly worth saving.
		if (lastSeenAt != null) {
			tag.setInteger("lastX", lastSeenAt.x);
			tag.setInteger("lastY", lastSeenAt.y);
			tag.setInteger("lastZ", lastSeenAt.z);
		}
		tag.setBoolean("move_full_stack", moveFullStack);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		target_side = tag.getInteger("target_side");
		target_slot = tag.getInteger("use_slot");
		is_input = tag.getBoolean("is_input");
		if (tag.hasKey("buffer")) {
			buffer = ItemStack.loadItemStackFromNBT(tag
					.getCompoundTag("buffer"));
		}
		match = tag.getString("match");
		match_to_visit = tag.getBoolean("match_to_visit");
		if (tag.hasKey("move_full_stack")) {
			moveFullStack = tag.getBoolean("move_full_stack");
		}
		if (tag.hasKey("lastX")) {
			lastSeenAt = new Coord(tag.getInteger("lastX"),
					tag.getInteger("lastY"), tag.getInteger("lastZ"));
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
		return "Router";
	}

	@Override
	public FactoryType getFactoryType() {
		return FactoryType.ROUTER;
	}

	public boolean handleMessageFromAny(int messageType, DataInput input)
			throws IOException {
		boolean cannon = mod_Factorization.instance.isCannonical(worldObj);
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

	public void broadcastMessage(EntityPlayer who, int messageType,
			Object... items) {
		// if server: send message to everyone within 100 blocks
		// if client: send message to server
		mod_Factorization.network.broadcastMessage(who, getCoord(), messageType,
				items);
	}

	@Override
	public boolean handleMessageFromClient(int messageType, DataInput input)
			throws IOException {
		if (super.handleMessageFromClient(messageType, input)) {
			return true;
		}
		if (handleMessageFromAny(messageType, input)) {
			return true;
		}
		return false;
	}

	@Override
	public boolean handleMessageFromServer(int messageType, DataInput input)
			throws IOException {
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
			lastSeenAt = new Coord(x, y, z);
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
		// XXX TODO: This is a terrible non-solution to work with frames.
		// Seems like we get infinite loops in isCoordConnected if we don't do
		// this. Maybe we should have our coordinates's origin be delta-based?
		resetGraph();
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
