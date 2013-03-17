package factorization.common;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.Icon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;

import factorization.api.Quaternion;
import factorization.common.NetworkFactorization.MessageType;
import factorization.common.TileEntityGreenware.ClayLump;

public class TileEntityGreenware extends TileEntityCommon {
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.CERAMIC;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Ceramic;
    }
    
    public static class ClayLump {
        public byte minX, minY, minZ;
        public byte maxX, maxY, maxZ;
        
        public short icon_id; //Only blocks
        public byte icon_md;
        
        public Quaternion quat;
        
        void write(ByteArrayDataOutput out) {
            out.writeByte(minX);
            out.writeByte(minY);
            out.writeByte(minZ);
            out.writeByte(maxX);
            out.writeByte(maxY);
            out.writeByte(maxZ);
            out.writeShort(icon_id);
            out.writeByte(icon_md);
            quat.write(out);
        }
        
        void write(NBTTagCompound tag) {
            tag.setByte("lx", minX);
            tag.setByte("ly", minY);
            tag.setByte("lz", minZ);
            tag.setByte("hx", maxX);
            tag.setByte("hy", maxY);
            tag.setByte("hz", maxZ);
            tag.setShort("icon_id", icon_id);
            tag.setByte("icon_md", icon_md);
            quat.writeToTag(tag, "r");
        }
        
        void write(ArrayList<Object> out) {
            out.add(minX);
            out.add(minY);
            out.add(minZ);
            out.add(maxX);
            out.add(maxY);
            out.add(maxZ);
            out.add(icon_id);
            out.add(icon_md);
            out.add(quat);
        }
        
        ClayLump read(DataInput in) throws IOException {
            minX = in.readByte();
            minY = in.readByte();
            minZ = in.readByte();
            maxX = in.readByte();
            maxY = in.readByte();
            maxZ = in.readByte();
            icon_id = in.readShort();
            icon_md = in.readByte();
            quat = Quaternion.read(in);
            return this;
        }
        
        ClayLump read(NBTTagCompound tag) {
            minX = tag.getByte("lx");
            minY = tag.getByte("ly");
            minZ = tag.getByte("lz");
            maxX = tag.getByte("hx");
            maxY = tag.getByte("hy");
            maxZ = tag.getByte("hz");
            icon_id = tag.getShort("icon_id");
            icon_md = tag.getByte("icon_md");
            quat = Quaternion.loadFromTag(tag, "r");
            return this;
        }
        
        void offset(int dx, int dy, int dz) {
            minX += dx;
            maxX += dx;
            minY += dy;
            maxY += dy;
            minZ += dz;
            maxZ += dz;
        }
        
        ClayLump asDefault() {
            minX = minZ = 4;
            minY = 0;
            maxX = maxZ = 16 - 4;
            maxY = 10;
            offset(16, 16+1, 16);
            icon_id = (short) Core.resource_id;
            icon_md = (byte) ResourceType.BISQUE.md;
            quat = new Quaternion();
            return this;
        }
        
        public void toBlockBounds(Block b) {
            b.setBlockBounds((minX - 16)/16F, (minY - 16)/16F, (minZ - 16)/16F, (maxX - 16)/16F, (maxY - 16)/16F, (maxZ - 16)/16F);
        }

        public ClayLump copy() {
            ClayLump ret = new ClayLump();
            ret.minX = minX;
            ret.minY = minY;
            ret.minZ = minZ;
            ret.maxX = maxX;
            ret.maxY = maxY;
            ret.maxZ = maxZ;
            ret.icon_id = icon_id;
            ret.icon_md = icon_md;
            ret.quat = new Quaternion(quat);
            return ret;
        }
    }
    
    public ArrayList<ClayLump> parts = new ArrayList();
    public int lastTouched = 0;
    int totalHeat = 0;
    
    //Client-side only
    public boolean renderedAsBlock = true; //keep rendering while waiting for the chunk to redraw
    
    public static int dryTime = 20*60*2; //2 minutes
    public static int bisqueHeat = 1000, glazeHeat = bisqueHeat*20;
    public static final int clayIconStart = 12*16;
    
    public static enum ClayState {
        WET("Wet Greenware"), DRY("Bone-Dry Greenware"), BISQUED("Bisqued"), GLAZED("High-Fire Glazed");
        public String english;
        ClayState(String en) {
            this.english = en;
        }
    };
    
    public TileEntityGreenware() {
    }
    
    public static ClayState getStateFromInfo(int touch, int heat) {
        if (heat > glazeHeat) {
            return ClayState.GLAZED;
        }
        if (heat > bisqueHeat) {
            return ClayState.BISQUED;
        }
        if (touch > dryTime) {
            return ClayState.DRY;
        }
        return ClayState.WET;
    }
    
    public ClayState getState() {
        return getStateFromInfo(lastTouched, totalHeat);
    }
    
    public Icon getIcon(ClayLump lump) {
        switch (getState()) {
        case WET: return BlockIcons.ceramics$wet;
        case DRY: return BlockIcons.ceramics$dry;
        case BISQUED: return BlockIcons.ceramics$bisque;
        case GLAZED:
            Item it = Item.itemsList[lump.icon_id];
            if (it == null) {
                return BlockIcons.error;
            }
            return it.getIconFromDamage(lump.icon_md);
        default: return BlockIcons.error;
        }
    }
    
    public void touch() {
        if (getState() == ClayState.WET) {
            lastTouched = 0;
        }
    }
    
    public boolean renderEfficient() {
        return getState() != ClayState.WET;
    }
    
    public boolean canEdit() {
        return getState() == ClayState.WET;
    }
    
    void initialize() {
        parts.clear();
        parts.add(new ClayLump().asDefault());
        touch();
    }
    
    void writeParts(NBTTagCompound tag) {
        NBTTagList l = new NBTTagList();
        for (ClayLump lump : parts) {
            NBTTagCompound rc_tag = new NBTTagCompound();
            lump.write(rc_tag);
            l.appendTag(rc_tag);
        }
        tag.setTag("parts", l);
        tag.setInteger("touch", lastTouched);
    }
    
    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        writeParts(tag);
    }
    
    void loadParts(NBTTagCompound tag) {
        if (tag == null) {
            initialize();
            return;
        }
        NBTTagList partList = tag.getTagList("parts");
        if (partList == null) {
            initialize();
            return;
        }
        parts.clear();
        for (int i = 0; i < partList.tagCount(); i++) {
            NBTTagCompound rc_tag = (NBTTagCompound) partList.tagAt(i);
            parts.add(new ClayLump().read(rc_tag));
        }
        lastTouched = tag.getInteger("touch");
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        loadParts(tag);
    }
    
    @Override
    public Packet getAuxillaryInfoPacket() {
        ArrayList<Object> args = new ArrayList(2 + parts.size()*9);
        args.add(MessageType.SculptDescription);
        args.add(getState().ordinal());
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        for (ClayLump lump : parts) {
            lump.write(args);
        }
        return getDescriptionPacketWith(args.toArray());
    }
    
    @Override
    void onPlacedBy(EntityPlayer player, ItemStack is, int side) {
        super.onPlacedBy(player, is, side);
        loadParts(is.getTagCompound());
    }

    ItemStack getItem() {
        ItemStack ret = Core.registry.greenware_item.copy();
        NBTTagCompound tag = new NBTTagCompound();
        writeParts(tag);
        ret.setTagCompound(tag);
        return ret;
    }
    
    private ClayState lastState = null;
    @Override
    public void updateEntity() {
        super.updateEntity();
        if (worldObj.isRemote) {
            return;
        }
        switch (getState()) {
        case WET:
            if (!worldObj.isRaining()) {
                lastTouched++;
            }
            if (totalHeat > 0) {
                totalHeat--;
                lastTouched++;
            }
            break;
        }
        if (getState() != lastState) {
            lastState = getState();
            broadcastMessage(null, MessageType.SculptState, lastState.ordinal());
        }
    }
    
    @Override
    public boolean activate(EntityPlayer player) {
        if (getState() == ClayState.WET) {
            touch();
        }
        ItemStack held = player.getCurrentEquippedItem();
        if (held == null) {
            return false;
        }
        int heldId = held.getItem().itemID;
        if (heldId == Item.bucketWater.itemID && getState() == ClayState.DRY) {
            lastTouched = 0;
            if (player.capabilities.isCreativeMode) {
                return true;
            }
            int ci = player.inventory.currentItem;
            player.inventory.mainInventory[ci] = new ItemStack(Item.bucketEmpty);
            return true;
        }
        if (heldId == Block.cloth.blockID) {
            lastTouched = dryTime + 1;
            return true;
        }
        if (held.getItem() != Item.clay || held.stackSize == 0) {
            return false;
        }
        held.stackSize--;
        if (player.worldObj.isRemote) {
            //Let the server tell us the results
            return true;
        }
        if (parts.size() >= 32) {
            Core.notify(player, getCoord(), "Too complex");
            held.stackSize++;
            return false;
        }
        addLump(player.username);
        return true;
    }
    
    void addLump(String creator) {
        parts.add(new ClayLump().asDefault());
        if (!worldObj.isRemote) {
            broadcastMessage(null, MessageType.SculptNew, creator);
            touch();
        }
    }
    
    void removeLump(int id) {
        if (id < 0 || id >= parts.size()) {
            return;
        }
        parts.remove(id);
        if (!worldObj.isRemote) {
            broadcastMessage(null, MessageType.SculptRemove, id);
            touch();
        }
    }
    
    public static boolean isValidLump(ClayLump lump) {
        return true; //TODO: Implement
        /*
        float edge = 8*3; //a cube and a half
        for (int i = 0; i < 6; i++) {
            for (VectorUV vertex : rc.faceVerts(i)) {
                if (abs(vertex.x) > edge) {
                    return false;
                }
                if (abs(vertex.y) > edge) {
                    return false;
                }
                if (abs(vertex.z) > edge) {
                    return false;
                }
            }
        }
        //do not be skiny
        if (rc.corner.x <= 0 || rc.corner.y <= 0 || rc.corner.z <= 0) {
            return false;
        }
        //do not be huge
        float max = 8;
        if (rc.corner.x > max || rc.corner.y > max || rc.corner.z > max) {
            return false;
        }
        return true;*/
    }
    
    private void updateLump(int id, ClayLump lump) {
        if (id < 0 || id >= parts.size()) {
            return;
        }
        ClayLump old = parts.get(id);
        if (old.equals(lump)) {
            return;
        }
        parts.set(id, lump);
        touch();
        if (worldObj.isRemote) {
            return;
        }
    }
    
    private void shareLump(int id, ClayLump selection) {
        ArrayList<Object> toSend = new ArrayList();
        toSend.add(id);
        selection.write(toSend);
        broadcastMessage(null, MessageType.SculptMove, toSend.toArray());
    }
    
    void changeLump(int id, ClayLump newValue) {
        updateLump(id, newValue);
        shareLump(id, newValue);
    }
    
    private float getFloat(DataInput input) throws IOException {
        int r = (int) (input.readFloat() * 2);
        //XXX TODO: clip to within the 3x3 cube!
        return r/2F;
    }
    
    @Override
    public boolean handleMessageFromClient(int messageType, DataInput input)
            throws IOException {
        if (super.handleMessageFromClient(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.SculptWater) {
            InventoryPlayer inv = Core.network.getCurrentPlayer().inventory;
            ItemStack is = inv.mainInventory[inv.currentItem];
            if (is == null) {
                return true;
            }
            Item item = is.getItem();
            if (item == null) {
                return true;
            }
            int id = item.itemID;
            if (is.getItem() == Item.bucketWater && getState() == ClayState.DRY) {
                is.itemID = Item.bucketWater.itemID;
                lastTouched = 0;
            }
            if (id == Block.cloth.blockID) {
                lastTouched = dryTime;
            }
            return true;
        }
        return false;
    }
    
    @Override
    public boolean handleMessageFromServer(int messageType, DataInput input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        switch (messageType) {
        case MessageType.SculptDescription:
            readStateChange(input);
            parts.clear();
            ArrayList<Object> args = new ArrayList();
            while (true) {
                try {
                    parts.add(new ClayLump().read(input));
                } catch (IOException e) {
                    break;
                }
            }
            break;
        case MessageType.SculptMove:
            updateLump(input.readInt(), new ClayLump().read(input));
            break;
        case MessageType.SculptNew:
            addLump(input.readUTF());
            break;
        case MessageType.SculptRemove:
            removeLump(input.readInt());
            break;
        case MessageType.SculptState:
            readStateChange(input);
            break;
        default: return false;
        }
        return true;
    }
    
    private void readStateChange(DataInput input) throws IOException {
        switch (ClayState.values()[input.readInt()]) {
        case WET:
            lastTouched = 0;
            break;
        case DRY:
            lastTouched = dryTime + 10;
            break;
        case BISQUED:
            totalHeat = bisqueHeat + 1;
            break;
        case GLAZED:
            totalHeat = glazeHeat + 1;
            break;
        }
        getCoord().redraw();
    }
    
    @Override
    public MovingObjectPosition collisionRayTrace(World w, int x, int y, int z, Vec3 startVec, Vec3 endVec) {
        BlockRenderHelper block;
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            block = Core.registry.clientTraceHelper;
        } else {
            block = Core.registry.serverTraceHelper;
        }
        for (int i = 0; i < parts.size(); i++) {
            ClayLump lump = parts.get(i);
            lump.toBlockBounds(block);
            block.beginNoIcons();
            block.rotate(lump.quat);
            block.setBlockBoundsBasedOnRotation();
            MovingObjectPosition mop = block.collisionRayTrace(w, x, y, z, startVec, endVec);
            if (mop != null) {
                mop.subHit = i;
                return mop;
            }
        }
        return null;
        //return super.collisionRayTrace(w, x, y, z, startVec, endVec);
    }
}
