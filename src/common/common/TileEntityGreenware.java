package factorization.common;

import static java.lang.Math.abs;
import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.src.EntityItem;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagList;
import net.minecraft.src.Packet;
import factorization.common.NetworkFactorization.MessageType;
import factorization.common.RenderingCube.Vector;

public class TileEntityGreenware extends TileEntityCommon {

    public ArrayList<RenderingCube> parts = new ArrayList();
    int lastTouched = 0;
    
    public static int dryTime = 20*60*10;
    public static int clayIcon_src = 8 + 4*16;
    public static int clayIcon = 0, selectedIcon = 1;
    
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
    
    enum ClayState {
        WET, DRY, BISQUED, FIRED;
    };
    
    public TileEntityGreenware() {
        int a = 4;
    }
    
    public ClayState getState() {
        return ClayState.WET;
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
        return lastTouched < dryTime;
    }
    
    void initialize() {
        parts.clear();
        parts.add(new RenderingCube(clayIcon, new Vector(3, 5, 3), null));
        touch();
    }
    
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.GREENWARE;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Ceramic;
    }
    
    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
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
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        NBTTagList l = (NBTTagList) tag.getTag("parts");
        if (l == null) {
            initialize();
            return;
        }
        parts.clear();
        for (int i = 0; i < l.tagCount(); i++) {
            NBTTagCompound rc_tag = (NBTTagCompound) l.tagAt(i);
            parts.add(RenderingCube.loadFromNBT(rc_tag));
        }
        lastTouched = tag.getInteger("touch");
    }
    
    @Override
    public Packet getAuxillaryInfoPacket() {
        ArrayList<Object> args = new ArrayList(1 + parts.size()*7);
        args.add(MessageType.SculptDescription);
        for (RenderingCube rc : parts) {
            rc.writeToArray(args);
        }
        return getDescriptionPacketWith(args.toArray());
    }
    
    @Override
    void onPlacedBy(EntityPlayer player, ItemStack is, int side) {
        super.onPlacedBy(player, is, side);
        initialize();
    }
    
    @Override
    void onRemove() {
        super.onRemove();
        int i = parts.size() - 1;
        if (i > 0) {
            EntityItem drop = new EntityItem(worldObj, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, new ItemStack(Item.clay, i));
            worldObj.spawnEntityInWorld(drop);
        }
    }
    
    @Override
    public void updateEntity() {
        super.updateEntity();
        if (!worldObj.isRemote && !worldObj.isRaining() && canEdit()) {
            lastTouched++;
        }
    }

    @Override
    public boolean activate(EntityPlayer player) {
        if (!player.worldObj.isRemote) {
            touch();
        }
        ItemStack held = player.getCurrentEquippedItem();
        if (held == null) {
            return false;
        }
        if (held.getItem() != Item.clay || held.stackSize == 0) {
            return false;
        }
        held.stackSize--;
        if (player.worldObj.isRemote) {
            //Let the server tell us the results
            return true;
        }
        if (parts.size() >= 64) {
            player.addChatMessage("This piece is too complex");
            held.stackSize++;
            return false;
        }
        addLump(player.username);
        return true;
    }
    
    void addLump(String creator) {
        parts.add(new RenderingCube(clayIcon, new Vector(4, 4, 4), null));
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
            for (Vector vertex : rc.faceVerts(i)) {
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
    
    void updateLump(int id, Vector newCorner, Vector newOrigin, Vector newAxis, float theta) {
        if (id < 0 || id >= parts.size()) {
            return;
        }
        RenderingCube rc = parts.get(id);
        if (rc.corner.equals(newCorner) && rc.origin.equals(newOrigin) && rc.axis.equals(newAxis) && rc.theta == theta) {
            return;
        }
        rc.corner = newCorner;
        rc.origin = newOrigin;
        rc.axis = newAxis;
        rc.theta = theta;
        if (worldObj.isRemote) {
            return;
        }
        touch();
    }
    
    void shareLump(int id, RenderingCube selection) {
        broadcastMessage(null, MessageType.SculptMove, id,
                selection.corner,
                selection.origin,
                selection.axis,
                selection.theta);
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
        return false;
    }
    
    @Override
    public boolean handleMessageFromServer(int messageType, DataInput input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        switch (messageType) {
        case MessageType.SculptDescription:
            parts.clear();
            ArrayList<Object> args = new ArrayList();
            while (true) {
                try {
                    args.add(input.readInt());
                } catch (IOException e) {
                    break;
                }
                for (int i = 0; i < 10; i++) {
                    args.add(input.readFloat());
                }
                parts.add(RenderingCube.readFromArray(args));
            }
            break;
        case MessageType.SculptMove:
            updateLump(input.readInt(), //id
                    Vector.readFromDataInput(input),
                    Vector.readFromDataInput(input),
                    Vector.readFromDataInput(input),
                    getFloat(input) //theta
                    );
            break;
        case MessageType.SculptNew:
            addLump(input.readUTF());
            break;
        case MessageType.SculptRemove:
            removeLump(input.readInt());
            break;
        default: return false;
        }
        return true;
    }
}
