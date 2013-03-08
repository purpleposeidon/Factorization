package factorization.common;

import static java.lang.Math.abs;
import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.Icon;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.MatrixTransform;
import factorization.api.VectorUV;
import factorization.common.NetworkFactorization.MessageType;

public class TileEntityGreenware extends TileEntityCommon {
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.GREENWARE;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Ceramic;
    }
    
/*
    public ArrayList<RenderingCube> parts = new ArrayList();
    public int lastTouched = 0;
    int totalHeat = 0;
    
    //Client-side only
    public boolean renderedAsBlock = true; //keep rendering while waiting for the chunk to redraw
    
    public static int dryTime = 20*60*2; //2 minutes
    public static int bisqueHeat = 1000, glazeHeat = bisqueHeat*20;
    public static final int clayIconStart = 12*16;
    
    static class SelectionInfo {
        TileEntityGreenware gw;
        int id;
        SelectionInfo(TileEntityGreenware gw, int id) {
            this.gw = gw;
            this.id = id;
        }
    }
    
    //server-side
    static HashMap<String, SelectionInfo> selections = new HashMap();
    //client-side
    public static RenderingCube selected;
    
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
    
    public int getIcon(RenderingCube rc) {
        int icon = 0;
        switch (getState()) {
        case WET:
            icon = 0;
            if (isSelected(rc)) {
                icon = 1;
            }
            break;
        case DRY:
            icon = 3;
            break;
        case BISQUED:
            icon = 5;
            break;
        case GLAZED:
            return rc.icon;
        }
        return clayIconStart + icon;
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
        parts.add(new RenderingCube(clayIconStart, new VectorUV(3, 5, 3)));
        touch();
    }
    
    void writeParts(NBTTagCompound tag) {
        NBTTagList l = new NBTTagList();
        for (RenderingCube rc : parts) {
            NBTTagCompound rc_tag = new NBTTagCompound();
            rc.writeToNBT(rc_tag);
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
            parts.add(RenderingCube.loadFromNBT(rc_tag));
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
        ArrayList<Object> args = new ArrayList(2 + parts.size()*7);
        args.add(MessageType.SculptDescription);
        args.add(getState().ordinal());
        for (RenderingCube rc : parts) {
            rc.writeToArray(args);
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
    
    public boolean isSelected(RenderingCube rc) {
        return rc == selected;
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
        parts.add(new RenderingCube(clayIconStart, new VectorUV(4, 4, 4)));
        if (!worldObj.isRemote) {
            broadcastMessage(null, MessageType.SculptNew, creator);
            selections.put(creator, new SelectionInfo(this, parts.size() - 1));
            touch();
        } else if (creator.equals(Minecraft.getMinecraft().thePlayer.username)) {
            //I added it, so select it
            selected = parts.get(parts.size() - 1);
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
    
    public static boolean isValidLump(RenderingCube rc) {
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
        return true;
    }
    
    void updateLump(int id, RenderingCube newCube) {
        if (id < 0 || id >= parts.size()) {
            return;
        }
        RenderingCube old = parts.get(id);
        if (old.equals(newCube)) {
            return;
        }
        old.icon = newCube.icon;
        old.corner = newCube.corner;
        old.trans = newCube.trans;
        touch();
        if (worldObj.isRemote) {
            return;
        }
    }
    
    void shareLump(int id, RenderingCube selection) {
        ArrayList<Object> toSend = new ArrayList();
        toSend.add(id);
        selection.writeToArray(toSend);
        broadcastMessage(null, MessageType.SculptMove, toSend.toArray());
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
        if (messageType == MessageType.SculptSelect) {
            selections.put(Core.network.getCurrentPlayer().username, new SelectionInfo(this, input.readInt()));
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
                    parts.add(RenderingCube.readFromDataInput(input));
                } catch (IOException e) {
                    break;
                }
            }
            break;
        case MessageType.SculptMove:
            updateLump(input.readInt(), RenderingCube.readFromDataInput(input));
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
    */
}
