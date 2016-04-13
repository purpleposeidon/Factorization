package factorization.ceramics;

import factorization.api.Coord;
import factorization.api.IFurnaceHeatable;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataOutNBT;
import factorization.api.datahelpers.Share;
import factorization.common.FactoryType;
import factorization.common.FzConfig;
import factorization.common.ResourceType;
import factorization.notify.Notice;
import factorization.shared.*;
import factorization.util.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static factorization.ceramics.TileEntityGreenware.GreenwareMessage.*;

public class TileEntityGreenware extends TileEntityCommon implements IFurnaceHeatable, ITickable {
    public static int MAX_PARTS = 32;
    EnumFacing front = EnumFacing.NORTH;
    byte rotation = 0;
    Quaternion rotation_quat = Quaternion.getRotationQuaternionRadians(0, EnumFacing.UP);
    
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.CERAMIC;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Ceramic;
    }

    @Override
    public boolean acceptsHeat() {
        ClayState state = getState();
        return state == ClayState.DRY || state == ClayState.UNFIRED_GLAZED || state == ClayState.WET;
    }

    @Override
    public void giveHeat() {
        ClayState state = getState();
        if (state == ClayState.DRY || state == ClayState.UNFIRED_GLAZED) {
            totalHeat += 1;
        }
        if (state == ClayState.WET) {
            lastTouched += 1;
        }
    }

    @Override
    public boolean hasLaggyStart() {
        return false;
    }

    @Override
    public boolean isStarted() {
        return false; // eh, not really necessary
    }

    public static class ClayLump {
        public byte minX, minY, minZ;
        public byte maxX, maxY, maxZ;

        public Block icon_id; // But only for blocks; no items
        public byte icon_md;
        public byte icon_side;

        public Quaternion quat;

        public int raw_color = -1;

        void write(ByteBuf out) {
            out.writeByte(minX);
            out.writeByte(minY);
            out.writeByte(minZ);
            out.writeByte(maxX);
            out.writeByte(maxY);
            out.writeByte(maxZ);
            out.writeShort(Block.getIdFromBlock(icon_id));
            out.writeByte(icon_md);
            out.writeByte(icon_side);
            quat.write(out);
        }

        void write(NBTTagCompound tag) {
            tag.setByte("lx", minX);
            tag.setByte("ly", minY);
            tag.setByte("lz", minZ);
            tag.setByte("hx", maxX);
            tag.setByte("hy", maxY);
            tag.setByte("hz", maxZ);
            //tag.setShort("icon_id", (short) FzUtil.getId(icon_id));
            String iname = DataUtil.getName(icon_id);
            if (iname != null) {
                tag.setString("icon_idC", iname);
            }
            tag.setByte("icon_md", icon_md);
            tag.setByte("icon_sd", icon_side);
            quat.writeToTag(tag, "r");
        }

        void write(ArrayList<Object> out) {
            out.add(minX);
            out.add(minY);
            out.add(minZ);
            out.add(maxX);
            out.add(maxY);
            out.add(maxZ);
            out.add((short) DataUtil.getId(icon_id));
            out.add(icon_md);
            out.add(icon_side);
            out.add(quat);
        }

        ClayLump read(ByteBuf in) throws IOException {
            minX = in.readByte();
            minY = in.readByte();
            minZ = in.readByte();
            maxX = in.readByte();
            maxY = in.readByte();
            maxZ = in.readByte();
            icon_id = DataUtil.getBlock(in.readShort());
            icon_md = in.readByte();
            icon_side = in.readByte();
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
            if (tag.hasKey("icon_id")) {
                icon_id = DataUtil.getBlock(tag.getShort("icon_id"));
            } else {
                icon_id = DataUtil.getBlockFromName(tag.getString("icon_idC"));
            }
            icon_md = tag.getByte("icon_md");
            if (tag.hasKey("icon_sd")) {
                icon_side = tag.getByte("icon_sd");
            } else {
                icon_side = -1;
            }
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
            offset(16, 16 + 1, 16);
            icon_id = Core.registry.resource_block;
            icon_md = (byte) ResourceType.BISQUE.md;
            icon_side = -1;
            quat = new Quaternion();
            return this;
        }

        public void toBlockBounds(Block b) {
            b.setBlockBounds((minX - 16) / 16F, (minY - 16) / 16F, (minZ - 16) / 16F, (maxX - 16) / 16F, (maxY - 16) / 16F, (maxZ - 16) / 16F);
        }

        public void toRotatedBlockBounds(TileEntityGreenware gw, Block b) {
            toBlockBounds(b);
            NORELEASE.fixme("Implement block bounds...");
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
    public int totalHeat = 0;
    boolean glazesApplied = false;
    private boolean partsValidated = false;

    public static int dryTime = 20 * 60 * 2; // 2 minutes
    public static int bisqueHeat = 1000, highfireHeat = bisqueHeat * 10;

    // Client-side only
    public boolean shouldRenderTesr = false;

    public static enum ClayState {
        WET("Wet Clay"), DRY("Bone-Dry Greenware"), BISQUED("Bisqued"), UNFIRED_GLAZED("Glazed Bisqueware"), HIGHFIRED("Highfire Glazed");
        public String english;

        ClayState(String en) {
            this.english = en;
        }
    };

    public TileEntityGreenware() {
    }

    public ClayState getState() {
        if (totalHeat > highfireHeat) {
            return ClayState.HIGHFIRED;
        }
        if (totalHeat > bisqueHeat) {
            if (glazesApplied) {
                return ClayState.UNFIRED_GLAZED;
            }
            return ClayState.BISQUED;
        }
        if (lastTouched > dryTime) {
            return ClayState.DRY;
        }
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
        return getState() == ClayState.WET;
    }

    void initialize() {
        parts = new ArrayList<ClayLump>();
        parts.add(new ClayLump().asDefault());
        touch();
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        lastTouched = data.as(Share.VISIBLE, "touch").putInt(lastTouched);
        totalHeat = data.as(Share.VISIBLE, "heat").putInt(totalHeat);
        glazesApplied = data.as(Share.PRIVATE, "glazed").putBoolean(glazesApplied);
        front = data.as(Share.VISIBLE, "front").putEnum(front);
        setRotation(data.as(Share.VISIBLE, "rot").putByte(rotation));
        if (data.isNBT()) {
            putParts(data, data.getTag());
        } else if (data.isReader()) {
            NBTTagCompound tag = data.as(Share.VISIBLE, "partList").putTag(new NBTTagCompound());
            putParts(data, tag);
        } else {
            NBTTagCompound tag = new NBTTagCompound();
            putParts(data, tag);
            tag = data.as(Share.VISIBLE, "partList").putTag(tag);
        }
        if (data.isReader()) {
            cache_dirty = true;
        }
    }

    private void putParts(DataHelper data, NBTTagCompound tag) {
        if (data.isReader()) {
            loadParts(tag);
        } else {
            writeParts(tag);
        }
    }

    private void writeParts(NBTTagCompound tag) {
        NBTTagList l = new NBTTagList();
        for (ClayLump lump : parts) {
            NBTTagCompound rc_tag = new NBTTagCompound();
            lump.write(rc_tag);
            l.appendTag(rc_tag);
        }
        tag.setTag("parts", l);
    }

    private void loadParts(NBTTagCompound tag) {
        if (tag == null) {
            initialize();
            return;
        }
        NBTTagList partList = tag.getTagList("parts", Constants.NBT.TAG_COMPOUND);
        if (partList == null) {
            initialize();
            return;
        }
        int tagCount = partList.tagCount();
        parts = new ArrayList<ClayLump>(tagCount);
        for (int i = 0; i < tagCount; i++) {
            NBTTagCompound rc_tag = partList.getCompoundTagAt(i);
            parts.add(new ClayLump().read(rc_tag));
        }
    }

    public void setRotation(byte newRotation) {
        rotation = newRotation;
        rotation_quat = Quaternion.getRotationQuaternionRadians(Math.PI*newRotation/2, EnumFacing.UP);
    }

    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, EnumFacing side, float hitX, float hitY, float hitZ) {
        super.onPlacedBy(player, is, side, hitX, hitY, hitZ);
        NBTTagCompound tag = null;
        if (is.hasTagCompound()) {
            tag = is.getTagCompound();
            try {
                putData(new DataInNBT(tag));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            addLump();
        }
        EnumFacing placement = SpaceUtil.determineFlatOrientation(player);
        if (tag == null || !tag.hasKey("front")) {
            front = placement;
            setRotation((byte) 0);
        } else if (placement.getDirectionVec().getY() == 0 && placement != null) {
            front = SpaceUtil.getOrientation(tag.getByte("front"));
            if (front == null || front.getDirectionVec().getY() != 0) {
                setRotation((byte) 0);
                front = placement;
            } else {
                EnumFacing f = placement;
                byte r = 0;
                for (byte i = 0; i < 4; i++) {
                    if (f == front) {
                        r = i;
                        break;
                    }
                    f = SpaceUtil.rotate(f, EnumFacing.UP);
                }
                setRotation(r);
            }
        }
    }

    public ItemStack getItem() {
        ItemStack ret = Core.registry.greenware_item.copy();
        NBTTagCompound tag = new NBTTagCompound();
        byte r = rotation;
        setRotation((byte) 0);
        try {
            putData(new DataOutNBT(tag));
        } catch (IOException e) {
            e.printStackTrace();
        }
        setRotation(r);
        tag.setByte("front", (byte)front.ordinal());
        ret.setTagCompound(tag);
        if (customName != null) {
            ret.setStackDisplayName(customName);
        }
        return ret;
    }

    private ClayState lastState = null;

    @Override
    public void update() {
        if (worldObj.isRemote) {
            return;
        }
        // TODO: Ugh, laaaame. Schedule a block update?
        if (!partsValidated) {
            partsValidated = true;
            Iterator<ClayLump> it = parts.iterator();
            while (it.hasNext()) {
                ClayLump lump = it.next();
                if (!isValidLump(lump)) {
                    if (parts.size() == 1) {
                        lump.asDefault();
                    } else {
                        it.remove();
                        InvUtil.spawnItemStack(getCoord(), new ItemStack(Items.clay_ball));
                    }
                }
            }
        }
        if (getState() == ClayState.WET) {
            if (!worldObj.isRaining()) {
                lastTouched++;
            }
            if (totalHeat > 0) {
                totalHeat--;
                lastTouched++;
            }
        }
        if (getState() != lastState) {
            lastState = getState();
            broadcastMessage(null, SculptState, lastState.ordinal());
        }
    }

    Item woolItem = Item.getItemFromBlock(Blocks.wool);
    
    @Override
    public boolean activate(EntityPlayer player, EnumFacing side) {
        ClayState state = getState();
        if (state == ClayState.WET) {
            touch();
        }
        ItemStack held = player.getCurrentEquippedItem();
        if (held == null) {
            return false;
        }
        Item heldId = held.getItem();
        boolean creative = player.capabilities.isCreativeMode;
        if (heldId == Items.blaze_powder && state != ClayState.HIGHFIRED) {
            PlayerUtil.cheatDecr(player, held);
            // TODO: Untested. Blaze powder should just finish the thing. Should also play a noise + emit particles.
            if (totalHeat < bisqueHeat) {
                totalHeat = bisqueHeat;
            } else if (totalHeat < highfireHeat) {
                totalHeat = highfireHeat;
            }
            return true;
        }
        if (heldId == Items.water_bucket && state == ClayState.DRY) {
            lastTouched = 0;
            if (creative) {
                return true;
            }
            int ci = player.inventory.currentItem;
            player.inventory.mainInventory[ci] = new ItemStack(Items.bucket);
            return true;
        }
        if (heldId == woolItem) {
            lastTouched = dryTime + 1;
            return true;
        }
        if (held.getItem() != Items.clay_ball || held.stackSize == 0) {
            return false;
        }
        if (!creative && state != ClayState.WET) {
            new Notice(this, "Not wet").send(player);
            return false;
        }
        if (!creative) {
            held.stackSize--;
        }
        if (player.worldObj.isRemote) {
            // Let the server tell us the results
            return true;
        }
        if (parts.size() >= MAX_PARTS) {
            new Notice(this, "Too complex").send(player);
            held.stackSize++;
            return false;
        }
        ClayLump toAdd = addLump();
        MovingObjectPosition hit = ItemSculptingTool.doRayTrace(player);
        if (hit == null || hit.subHit == -1) {
            return true;
        }
        ClayLump against = parts.get(hit.subHit);
        ClayLump extrusion = extrudeLump(against, hit.sideHit);
        if (isValidLump(extrusion)) {
            changeLump(parts.size() - 1, extrusion);
        } else {
            // TODO: Sometimes it fails when it shouldn't.
        }
        return true;
    }

    ClayLump addLump() {
        ClayLump ret = new ClayLump().asDefault();
        parts.add(ret);
        if (!worldObj.isRemote) {
            broadcastMessage(null, SculptNew);
            touch();
        }
        return ret;
    }

    void removeLump(int id) {
        if (id < 0 || id >= parts.size()) {
            return;
        }
        parts.remove(id);
        if (!worldObj.isRemote) {
            broadcastMessage(null, SculptRemove, id);
            touch();
        }
    }

    ClayLump extrudeLump(ClayLump against, EnumFacing dir) {
        ClayLump lump = against.copy();
        Block b = FzUtil.getTraceHelper();
        against.toBlockBounds(b);
        int wX = lump.maxX - lump.minX;
        int wY = lump.maxY - lump.minY;
        int wZ = lump.maxZ - lump.minZ;
        lump.maxX += wX * dir.getDirectionVec().getX();
        lump.maxY += wY * dir.getDirectionVec().getY();
        lump.maxZ += wZ * dir.getDirectionVec().getZ();
        lump.minX += wX * dir.getDirectionVec().getX();
        lump.minY += wY * dir.getDirectionVec().getY();
        lump.minZ += wZ * dir.getDirectionVec().getZ();
        return lump;
    }

    public boolean isValidLump(ClayLump lump) {
        // check volume
        if (!(Core.cheat)) {
            int wX = lump.maxX - lump.minX;
            int wY = lump.maxY - lump.minY;
            int wZ = lump.maxZ - lump.minZ;
            int area = wX * wY * wZ;
            int max_area = 16 * 16 * 16 /* / 4 */;
            if (!FzConfig.stretchy_clay) {
                max_area /= 4;
            }
            if (area <= 0 || area > max_area) {
                return false;
            }
        }

        // check bounds
        final int B = 16 * 3;
        if (lump.minX < 0)
            return false;
        if (lump.minY < 0)
            return false;
        if (lump.minZ < 0)
            return false;
        if (lump.maxX > B)
            return false;
        if (lump.maxY > B)
            return false;
        if (lump.maxZ > B)
            return false;

        // check for free space (needs to be last, as it can mutate the world)
        Block block = FzUtil.getTraceHelper();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    AxisAlignedBB ab = new AxisAlignedBB(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz, pos.getX() + dx + 1, pos.getY() + dy + 1, pos.getZ() + dz + 1);
                    Coord c = getCoord();
                    c.x += dx;
                    c.y += dy;
                    c.z += dz;
                    lump.toRotatedBlockBounds(this, block);
                    AxisAlignedBB in = block.getCollisionBoundingBox(worldObj, pos, c.getState());
                    if (in != null && ab.intersectsWith(in)) {
                        // This block needs to be an Extension, or this
                        if (c.isAir() || c.isReplacable()) {
                            c.setId(Core.registry.legacy_factory_block);
                            TileEntityExtension tex = new TileEntityExtension(this);
                            c.setTE(tex);
                            tex.getBlockClass().enforce(c);
                            continue;
                        }
                        TileEntity te = c.getTE();
                        if (te == this) {
                            continue;
                        }
                        if (te instanceof TileEntityExtension) {
                            TileEntityExtension tex = (TileEntityExtension) te;
                            if (tex.getParent() == this) {
                                continue;
                            }
                        }
                        // We used to not allow this. We just make a bit of noise instead.
                        // A notification will indicate that things will be a bit messed up here.
                        // FIXME: Let block collision boxes go outside the block (Notch hard-coded for fences)
                        new Notice(c, "!").sendToAll();
                    }
                }
            }
        }
        return true;
    }

    @Override
    protected void onRemove() {
        super.onRemove();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Coord c = getCoord().add(pos);
                    TileEntityExtension tex = c.getTE(TileEntityExtension.class);
                    if (tex != null && tex.getParent() == this) {
                        c.setAir();
                    }
                }
            }
        }
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
        broadcastMessage(null, SculptMove, toSend.toArray());
    }

    void changeLump(int id, ClayLump newValue) {
        updateLump(id, newValue);
        shareLump(id, newValue);
    }

    private float getFloat(DataInput input) throws IOException {
        int r = (int) (input.readFloat() * 2);
        // XXX TODO: clip to within the 3x3 cube!
        return r / 2F;
    }

    @Override
    public boolean handleMessageFromServer(Enum messageType, ByteBuf input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType.equals(factorization.net.StandardMessageType.Description)) {
            readStateChange(input);
            front = SpaceUtil.getOrientation(input.readByte());
            setRotation(input.readByte());
            parts.clear();
            ArrayList<Object> args = new ArrayList();
            while (true) {
                try {
                    parts.add(new ClayLump().read(input));
                } catch (IOException e) {
                    break;
                }
            }
            shouldRenderTesr = getState() == ClayState.WET;
        } else if (messageType.equals(GreenwareMessage.SculptMove)) {
            updateLump(input.readInt(), new ClayLump().read(input));
        } else if (messageType.equals(GreenwareMessage.SculptNew)) {
            addLump();
        } else if (messageType.equals(GreenwareMessage.SculptRemove)) {
            removeLump(input.readInt());
        } else if (messageType.equals(GreenwareMessage.SculptState)) {
            readStateChange(input);
        } else {
            return false;
        }
        cache_dirty = true;
        if (renderEfficient()) {
            getCoord().redraw();
        }
        return true;
    }

    private void readStateChange(ByteBuf input) throws IOException {
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
        case UNFIRED_GLAZED:
            totalHeat = bisqueHeat + 2;
            glazesApplied = true;
            break;
        case HIGHFIRED:
            totalHeat = highfireHeat + 1;
            break;
        }
        getCoord().redraw();
    }

    @Override
    protected boolean removedByPlayer(EntityPlayer player, boolean willHarvest) {
        if (player.worldObj.isRemote) {
            return false;
        }
        MovingObjectPosition hit = ItemSculptingTool.doRayTrace(player);
        if (hit == null || hit.subHit == -1 || parts.size() < 1) {
            return super.removedByPlayer(player, willHarvest);
        }
        Coord here = getCoord();
        ClayState state = getState();
        // If it's solid, break it.
        // If we're sneaking & creative, break it
        boolean shouldDestroy = player.isSneaking() || parts.size() == 1;
        if (player.capabilities.isCreativeMode) {
            if (shouldDestroy) {
                return super.removedByPlayer(player, willHarvest);
            } else {
                removeLump(hit.subHit);
                return true;
            }
        }
        shouldDestroy |= state != ClayState.WET;
        if (shouldDestroy) {
            InvUtil.spawnItemStack(here, getItem());
            here.setAir();
        } else {
            removeLump(hit.subHit);
            InvUtil.spawnItemStack(here, new ItemStack(Items.clay_ball));
        }
        return false;
    }

    @Override
    public MovingObjectPosition collisionRayTrace(Vec3 startVec, Vec3 endVec) {
        Block block = new Block(Material.rock);
        // It's possible for the startVec to be embedded in a lump (causing it
        // to hit the opposite side), so we must move it farther away
        Vec3 d = startVec.subtract(endVec);
        double scale = 5.2; // Diagonal of a 3³. (Was initially using incrScale = 2)
        // This isn't quite right; the dVector would properly be normalized here
        // & rescaled to the max diameter. But we can survive without it.
        // Unnormalized length of dVector is 6m in surviavl mode IIRC. This'll
        // be way longer than it needs to be.
        // Why is it + instead of -? Hmm.
        startVec = startVec.add(SpaceUtil.scale(d, scale));
        MovingObjectPosition shortest = null;
        for (int i = 0; i < parts.size(); i++) {
            ClayLump lump = parts.get(i);
            lump.toRotatedBlockBounds(this, block);
            MovingObjectPosition mop = block.collisionRayTrace(worldObj, pos, startVec, endVec);
            if (mop != null) {
                mop.subHit = i;
                if (shortest == null) {
                    shortest = mop;
                } else {
                    Vec3 s = shortest.hitVec;
                    Vec3 m = mop.hitVec;
                    s = new Vec3(s.xCoord, s.yCoord, s.zCoord);
                    m = new Vec3(m.xCoord, m.yCoord, m.zCoord);
                    startVec = startVec.subtract(s).subtract(m);
                    if (m.lengthVector() < s.lengthVector()) {
                        shortest = mop;
                    }
                }
            }
        }
        return shortest;
        // return super.collisionRayTrace(w, pos, startVec, endVec);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void renderCeramicsSelection(DrawBlockHighlightEvent event) {
        if (event.target.subHit == -1) {
            return;
        }
        Coord c = Coord.fromMop(event.player.worldObj, event.target);
        TileEntityGreenware clay = c.getTE(TileEntityGreenware.class);
        if (clay == null) {
            return;
        }
        if (event.target.subHit < 0 || event.target.subHit >= clay.parts.size()) {
            return;
        }
        event.setCanceled(true);
        EntityPlayer player = event.player;
        double partial = event.partialTicks;
        ClayLump lump = clay.parts.get(event.target.subHit);
        Block block = new Block(Material.rock);
        lump.toRotatedBlockBounds(clay, block);
        double widen = 0.002;
        double oX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partial;
        double oY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partial;
        double oZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partial;
        AxisAlignedBB bb = block.getSelectedBoundingBox(c.w, c.toBlockPos()).expand(widen, widen, widen).offset(-oX, -oY, -oZ);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDepthMask(false);
        float r = 0xFF;
        GL11.glLineWidth(2.0F);
        GL11.glColor4f(0, 0, 0, 0.4F);
        // GL11.glColor4f(0x4D/r, 0x34/r, 0x7C/r, 0.8F); //#4D347C
        RenderGlobal.drawSelectionBoundingBox(bb);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);

        // TODO: If the rotation tool is selected, may draw the axis?
        // Oooooh, we could also draw the offset position for *EVERY* tool...
    }


    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        AxisAlignedBB bb = new AxisAlignedBB(pos.getX() - 2, pos.getY() - 2, pos.getZ() - 2, pos.getX() + 2, pos.getY() + 2, pos.getZ() + 2);
        return bb;
    }

    @Override
    public void setBlockBounds(Block b) {
        super.setBlockBounds(b);
        // b.setBlockBounds(-1, -1, -1, 1, 1, 1);
    }

    @Override
    public boolean addCollisionBoxesToList(Block ignore, AxisAlignedBB aabb, List<AxisAlignedBB> list, Entity entity) {
        Block block = new Block(Material.rock);
        ClayState state = getState();
        if (state == ClayState.WET) {
            block.setBlockBounds(0, 0, 0, 1, 1F / 8F, 1);
            AxisAlignedBB a = block.getCollisionBoundingBox(worldObj, pos, null);
            if (aabb.intersectsWith(a)) {
                list.add(a);
            }
        }
        for (ClayLump lump : parts) {
            lump.toRotatedBlockBounds(this, block);
            AxisAlignedBB a = block.getCollisionBoundingBox(worldObj, pos, null);
            if (aabb.intersectsWith(a)) {
                list.add(a);
            }
        }
        return true;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox() {
        return null;
    }

    @Override
    public boolean isBlockSolidOnSide(EnumFacing side) {
        return false;
    }

    @Override
    public ItemStack getDroppedBlock() {
        return getItem();
    }

    @Override
    public void neighborChanged() {
        if (!worldObj.isRemote && parts.isEmpty()) {
            getCoord().setAir();
        }
    }

    boolean cache_dirty = true;
    @SideOnly(Side.CLIENT)
    IBakedModel modelCache;

    @SideOnly(Side.CLIENT)
    public IBakedModel buildModel() {
        if (!cache_dirty) return modelCache;
        cache_dirty = false;
        // NORELEASE: Implement. See: TRSRTransformation, FaceBakery
        return null;
    }

    public enum GreenwareMessage {
        SculptNew, SculptMove, SculptRemove, SculptState;
        public static final GreenwareMessage[] VALUES = values();
    }

    @Override
    public Enum[] getMessages() {
        return GreenwareMessage.VALUES;
    }
}
