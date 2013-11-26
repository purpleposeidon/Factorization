package factorization.common.servo;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeDirection;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

import cpw.mods.fml.common.network.FMLNetworkHandler;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.FzOrientation;
import factorization.api.IChargeConductor;
import factorization.api.IEntityMessage;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataInPacket;
import factorization.api.datahelpers.DataInPacketClientEdited;
import factorization.api.datahelpers.DataOutNBT;
import factorization.api.datahelpers.DataOutPacket;
import factorization.api.datahelpers.Share;
import factorization.common.Core;
import factorization.common.FactorizationUtil;
import factorization.common.FactorizationUtil.FzInv;
import factorization.common.FactoryType;
import factorization.common.ISocketHolder;
import factorization.common.NetworkFactorization.MessageType;
import factorization.common.TileEntitySocketBase;
import factorization.common.sockets.GuiDataConfig;
import factorization.common.sockets.SocketEmpty;
import factorization.notify.Notify;

public class ServoMotor extends Entity implements IEntityAdditionalSpawnData, IEntityMessage, IInventory, ISocketHolder {
    //NORELEASE: Have the servorail TileEntity store a reference to this?
    public static final int STACKS = 16;
    public static final int STACK_EQUIPMENT = 0, STACK_ARGUMENT = 4, STACK_IO = 2, STACK_CONFIG = 3, STACK_ERRNO = 15;
    private ServoStack[] stacks = new ServoStack[STACKS];
    {
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = new ServoStack();
        }
    }
    private ItemStack[] inv = new ItemStack[4], inv_last_sent = new ItemStack[inv.length];
    public boolean skipNextInstruction = false;

    Coord pos_prev, pos_next;
    float pos_progress;
    
    public TileEntitySocketBase socket = new SocketEmpty();
    public boolean isSocketActive = false, isSocketPulsed = false;
    
    public FzOrientation prevOrientation = FzOrientation.UNKNOWN, orientation = FzOrientation.UNKNOWN;
    public ForgeDirection nextDirection = ForgeDirection.UNKNOWN, lastDirection = ForgeDirection.UNKNOWN;
    private byte speed_b;
    public byte target_speed = 2;
    private static final double max_speed_b = 127;
    double accumulated_motion;
    public boolean stopped = false, prevStopped = false;
    
    boolean new_motor = true;
    
    //For client-side rendering
    double sprocket_rotation = 0, prev_sprocket_rotation = 0;
    double servo_reorient = 0, prev_servo_reorient = 0;

    private static final double normal_speed = 0.0875;
    private static final double[] targetSpeeds = {normal_speed / 3, normal_speed / 2, normal_speed, normal_speed*2, normal_speed*4};
    private static final double speed_limit = targetSpeeds[targetSpeeds.length - 1];
    
    short actions_since_last_sync = 0;

    public ServoMotor(World world) {
        super(world);
        setSize(1, 1);
        double d = 0.5;
        pos_prev = new Coord(world, 0, 0, 0);
        pos_next = pos_prev.copy();
        isImmuneToFire = true;
    }
    
    /**
     * You <b>must</b> call this method instead of using worldObj.spawnEntityInWorld!
     */
    public void spawnServoMotor() {
        pos_prev = new Coord(this);
        pos_next = pos_prev.copy();
        pickNextOrientation();
        pickNextOrientation();
        interpolatePosition(0);
        worldObj.spawnEntityInWorld(this);
    }

    @Override
    protected void entityInit() {
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
        try {
            putData(new DataInNBT(nbttagcompound));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
        try {
            putData(new DataOutNBT(nbttagcompound));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void readSpawnData(ByteArrayDataInput data) {
        try {
            putData(new DataInPacket(data, Side.CLIENT));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace(); //Hrm! Why? (I mean, besides the obvious.)
        }
    }

    @Override
    public void writeSpawnData(ByteArrayDataOutput data) {
        try {
            putData(new DataOutPacket(data, Side.SERVER));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace(); //Hrm! Why? (I mean, besides the obvious.)
        }
    }

    void putData(DataHelper data) throws IOException {
        data.as(Share.VISIBLE, "controller");
        orientation = data.as(Share.VISIBLE, "Orient").putFzOrientation(orientation);
        nextDirection = data.as(Share.VISIBLE, "nextDir").putEnum(nextDirection);
        lastDirection = data.as(Share.VISIBLE, "lastDir").putEnum(lastDirection);
        speed_b = data.as(Share.VISIBLE, "speedb").putByte(speed_b);
        target_speed = data.as(Share.VISIBLE, "speedt").putByte(target_speed);
        accumulated_motion = data.as(Share.PRIVATE, "accumulated_motion").putDouble(accumulated_motion);
        pos_next = data.as(Share.VISIBLE, "pos_next").put(pos_next);
        pos_prev = data.as(Share.VISIBLE, "pos_prev").put(pos_prev);
        pos_progress = data.as(Share.VISIBLE, "pos_progress").putFloat(pos_progress);
        skipNextInstruction = data.as(Share.VISIBLE, "skip").putBoolean(skipNextInstruction);
        new_motor = data.as(Share.PRIVATE, "new").putBoolean(new_motor); //NORELEASE: This isn't used?
        for (int i = 0; i < STACKS; i++) {
            String name = "stack" + i;
            stacks[i] = data.as(Share.VISIBLE, name).put(stacks[i]);
        }
        for (int i = 0; i < inv.length; i++) {
            ItemStack is = inv[i] == null ? EMPTY_ITEM : inv[i];
            is = data.as(Share.VISIBLE, "inv" + i).putItemStack(is);
            if (is == null) {
                inv[i] = is;
            } else {
                inv[i] = is.itemID == 0 ? null : is;
            }
        }
        data.as(Share.VISIBLE, "sock");
        if (data.isReader()) {
            NBTTagCompound tag = data.putTag(new NBTTagCompound());
            TileEntity te = TileEntity.createAndLoadEntity(tag);
            if (te instanceof TileEntitySocketBase) {
                socket = (TileEntitySocketBase) te;
            } else {
                socket = new SocketEmpty();
            }
        } else {
            NBTTagCompound output = new NBTTagCompound();
            socket.writeToNBT(output);
            data.putTag(output);
        }
        isSocketActive = data.as(Share.VISIBLE, "sockon").putBoolean(isSocketActive);
        isSocketPulsed = data.as(Share.VISIBLE, "sockpl").putBoolean(isSocketPulsed);
        stopped = data.as(Share.VISIBLE, "stop").putBoolean(stopped);
    }

    public double getTargetSpeed() {
        try {
            return targetSpeeds[target_speed];
        } catch (ArrayIndexOutOfBoundsException e) {
            target_speed = 1;
            return targetSpeeds[target_speed];
        }
    }
    
    boolean validPosition(Coord c, boolean desperate) {
        TileEntityServoRail sr = c.getTE(TileEntityServoRail.class);
        if (sr == null) {
            return false;
        }
        return sr.priority >= 0 || desperate;
    }

    boolean validDirection(ForgeDirection dir, boolean desperate) {
        return validPosition(getCurrentPos().add(dir), desperate);
    }

    private boolean need_description_packet = false;
    private static Quaternion target_orientation = new Quaternion();
    @Override
    public void onEntityUpdate() {
        if (new_motor) {
            if (prevOrientation == FzOrientation.UNKNOWN ) {
                prevOrientation = orientation;
            }
        }
        super.onEntityUpdate();
        if (worldObj.isRemote) {
            prev_sprocket_rotation = sprocket_rotation;
            prev_servo_reorient = servo_reorient;
            doLogic();
            if (stopped) {
                speed_b = 0;
            }
        } else {
            byte orig_speed = speed_b;
            doLogic();
            if (stopped) {
                speed_b = 0;
            }
            need_description_packet |= orig_speed != speed_b;
        }
        interpolatePosition(pos_progress);
        if (stopped && getCurrentPos().isWeaklyPowered()) {
            stopped = false;
        }
        if (stopped != prevStopped) {
            desync(true);
        }
        if (need_description_packet) {
            need_description_packet = false;
            describe();
        }
        prevStopped = stopped;
    }
    
    public void interpolatePosition(float interp) {
        setPosition(
                ip(pos_prev.x, pos_next.x, interp),
                ip(pos_prev.y, pos_next.y, interp),
                ip(pos_prev.z, pos_next.z, interp));
    }
    
    static double ip(int a, int b, float interp) {
        return a + (b - a)*interp;
    }

    void updateSpeed() {
        final double speed = getProperSpeed();
        final double maxSpeed = getTargetSpeed();
        boolean should_accelerate = speed < maxSpeed && orientation != FzOrientation.UNKNOWN;
        if (speed > maxSpeed) {
            speed_b = (byte) Math.max(maxSpeed, (speed_b/2) - 1);
            return;
        }
        if (Core.cheat_servo_energy) {
            if (should_accelerate) {
                accelerate();
            }
        } else {
            long now = worldObj.getTotalWorldTime();
            IChargeConductor conductor = getCurrentPos().getTE(IChargeConductor.class);
            boolean failure = true;
            if (conductor != null) {
                int to_drain = 0;
                if (should_accelerate) {
                    if (now % 3 == 0) {
                        to_drain = 2;
                    } else {
                        should_accelerate = false;
                    }
                } else if (now % 60 == 0) {
                    to_drain = 1;
                }
                if (to_drain > 0 && should_accelerate && conductor.getCharge().tryTake(to_drain) >= to_drain) {
                    accelerate();
                    failure = false;
                } else if (to_drain == 0) {
                    failure = false;
                }
            } else {
                despawn(false);
            }
            if (failure) {
                speed_b = (byte) Math.max(speed_b - 1, 0);
            }
        }
    }
    
    public void penalizeSpeed() {
        if (speed_b > 4) {
            speed_b--;
        }
    }
    
    public void desync(boolean totally) {
        if (totally) {
            actions_since_last_sync = Short.MAX_VALUE;
        } else {
            need_description_packet = true;
        }
    }
    
    void doLogic() {
        if (isDead) {
            return;
        }
        if (stopped) {
            updateSocket();
            return;
        }
        if (orientation == FzOrientation.UNKNOWN) {
            pickNextOrientation();
        }
        if (!worldObj.isRemote) {
            if (actions_since_last_sync > 200) {
                actions_since_last_sync = 0;
                Packet toSend = FMLNetworkHandler.getEntitySpawningPacket(this);
                Core.network.broadcastPacket(worldObj, (int) posX, (int) posY, (int) posZ, toSend);
            }
            updateSpeed();
        }
        final double speed = getProperSpeed() ;
        if (speed <= 0 || orientation == FzOrientation.UNKNOWN) {
            updateSocket();
            return;
        }
        accumulated_motion += speed;
        moveMotor();
        if (pos_progress >= 1) {
            pos_progress -= 1F;
            accumulated_motion = Math.min(pos_progress, speed);
            Chunk oldChunk = pos_prev.getChunk(), newChunk = pos_next.getChunk();
            pos_prev = pos_next;
            updateSocket();
            onEnterNewBlock();
            pickNextOrientation();
            if (oldChunk != newChunk) {
                oldChunk.setChunkModified();
                newChunk.setChunkModified();
            }
        } else {
            updateSocket();
        }
    }
    
    void updateSocket() {
        Coord here = getCurrentPos();
        here.setAsTileEntityLocation(socket);
        socket.facing = orientation.top;
        socket.genericUpdate(this, here, isSocketActive ^ isSocketPulsed);
        isSocketPulsed = false;
    }
    
    public Random getRandom() {
        //Synchronizing RNG state isn't worthwhile
        //It's possible things could end up in loops like this.
        //Could probably think of something else to throw in.
        Random rand = FactorizationUtil.dirtyRandomCache();
        long seed = entityId + getCurrentPos().seed() << 5 + orientation.ordinal() << 2 + nextDirection.ordinal();
        rand.setSeed(seed);
        return rand;
    }
    
    public boolean testDirection(ForgeDirection d, boolean desperate) {
        if (d == ForgeDirection.UNKNOWN) {
            return false;
        }
        return validDirection(d, desperate);
    }
    
    static int similarity(FzOrientation base, FzOrientation novel) {
        int score = 0;
        //if pointing in plane, we want them to face the same direction
        
        return score;
    }
    
    
    boolean pickNextOrientation() {
        actions_since_last_sync++;
        boolean ret = pickNextOrientation_impl();
        pos_next = pos_prev.add(orientation.facing);
        return ret;
    }
    
    public void changeOrientation(ForgeDirection dir) {
        ForgeDirection orig_direction = orientation.facing;
        ForgeDirection orig_top = orientation.top;
        FzOrientation start = FzOrientation.fromDirection(dir);
        FzOrientation perfect = start.pointTopTo(orig_top);
        if (perfect == FzOrientation.UNKNOWN) {
            if (dir == orig_top) {
                //convex turn
                perfect = start.pointTopTo(orig_direction.getOpposite());
            } else if (dir == orig_top.getOpposite()) {
                //concave turn
                perfect = start.pointTopTo(orig_direction);
            }
            if (perfect == FzOrientation.UNKNOWN) {
                perfect = start; //Might be impossible?
            }
        }
        orientation = perfect;
        lastDirection = orig_direction;
        if (orientation.facing == nextDirection) {
            nextDirection = ForgeDirection.UNKNOWN;
        }
    }

    boolean pickNextOrientation_impl() {
        ArrayList<ForgeDirection> dirs = FactorizationUtil.getRandomDirections(worldObj.rand);
        int available_nonbackwards_directions = 0;
        Coord look = pos_next.copy();
        int all_count = 0;
        for (int i = 0; i < dirs.size(); i++) {
            ForgeDirection fd = dirs.get(i);
            look.set(pos_next);
            look.adjust(fd);
            TileEntityServoRail sr = look.getTE(TileEntityServoRail.class);
            if (sr == null) {
                continue;
            }
            all_count++;
            if (fd == orientation.facing.getOpposite()) {
                continue;
            }
            if (sr.priority > 0) {
                changeOrientation(fd);
                need_description_packet = true; //NORELEASE: Check this out: Does the client simulate the same as the server? (Probably not.) Use a particle marker.
                return true;
            }
            if (sr.priority == 0) {
                available_nonbackwards_directions++;
            }
        }
        
        if (all_count == 0) {
            //Sadness
            speed_b = 0;
            return false;
        }
        
        final boolean desperate = available_nonbackwards_directions < 1;
        final ForgeDirection direction = orientation.facing;
        final ForgeDirection opposite = direction.getOpposite();
        
        if (nextDirection != opposite && testDirection(nextDirection, desperate)) {
            // We can go the way we were told to go next
            changeOrientation(nextDirection);
            return true;
        }
        if (testDirection(direction, desperate)) {
            // Our course is fine.
            return true;
        }
        if (lastDirection != opposite && testDirection(lastDirection, desperate)) {
            // Try the direction we were going before (this makes us go in zig-zags)
            changeOrientation(lastDirection);
            return true;
        }
        final ForgeDirection top = orientation.top;
        if (testDirection(top, desperate) /* top being opposite won't be an issue because of Geometry */ ) {
            // We'll turn upwards.
            changeOrientation(top);
            return true;
        }
        
        // We'll pick a random direction; we're re-using the list from before, should be fine.
        // Going backwards is our last resort.
        for (int i = 0; i < 6; i++) {
            ForgeDirection d = dirs.get(i);
            if (d == nextDirection || d == direction || d == opposite || d == lastDirection || d == top) {
                continue;
            }
            if (validDirection(d, desperate)) {
                changeOrientation(d);
                need_description_packet = true; //NORELEASE: Check this out: Does the client simulate the same as the server? (Probably not.) Use a particle marker.
                return true;
            }
        }
        if (validDirection(opposite, true)) {
            orientation = FzOrientation.fromDirection(opposite).pointTopTo(top);
            if (orientation != FzOrientation.UNKNOWN) {
                return true;
            }
        }
        orientation = FzOrientation.UNKNOWN;
        return false;
    }

    void accelerate() {
        speed_b += 1;
        speed_b = (byte) Math.min(speed_b, max_speed_b);
    }

    void broadcast(int message_type, Object... msg) {
        Packet p = Core.network.entityPacket(this, message_type, msg);
        Core.network.broadcastPacket(worldObj, (int) posX, (int) posY, (int) posZ, p);
    }

    void moveMotor() {
        if (accumulated_motion == 0) {
            return;
        }
        double move = Math.min(accumulated_motion, 1 - pos_progress);
        accumulated_motion -= move;
        pos_progress += move;
        if (worldObj.isRemote) {
            sprocket_rotation += move;
            
            if (orientation != prevOrientation) {
                servo_reorient = Math.min(1, servo_reorient + move);
                if (servo_reorient >= 1) {
                    prev_servo_reorient = servo_reorient = 0;
                    prevOrientation = orientation;
                }
            } else {
                servo_reorient = 0;
            }
        }
    }
    
    public Coord getCurrentPos() {
        return pos_prev;
    }
    
    public Coord getNextPos() {
        return pos_next;
    }

    void onEnterNewBlock() {
        TileEntityServoRail rail = getCurrentPos().getTE(TileEntityServoRail.class);
        if (rail == null /* :| */ || rail.decoration == null) {
            return;
        }
        if (getCurrentPos().isWeaklyPowered()) {
            return;
        }
        if (skipNextInstruction) {
            skipNextInstruction = false;
            return;
        }
        rail.decoration.motorHit(this);
        actions_since_last_sync++;
    }

    @Override
    public boolean interactFirst(EntityPlayer player) {
        desync(true);
        ItemStack is = FactorizationUtil.normalize(player.getHeldItem());
        if (is == null) {
            return false;
        }
        if (socket instanceof SocketEmpty) {
            int md = is.getItemDamage();
            if (md > 0 && md < FactoryType.MAX_ID) {
                try {
                    socket = (TileEntitySocketBase) FactoryType.fromMd(md).getFactoryTypeClass().newInstance();
                } catch (Throwable e) {
                    e.printStackTrace();
                    Notify.send(null, this, "Exception; see console log\n" + e.getMessage());
                }
            }
            if (!player.capabilities.isCreativeMode) {
                is.stackSize--;
            }
        } else if (socket != null) {
            return socket.activateOnServo(player, this);
        }
        return false;
    }
    
    @Override
    public boolean attackEntityFrom(DamageSource damageSource, float damage) {
        if (damageSource instanceof EntityDamageSourceIndirect) {
            return false;
        }
        Entity src = damageSource.getSourceOfDamage();
        if (!(src instanceof EntityPlayer)) {
            return false;
        }
        EntityPlayer player = (EntityPlayer) src;
        despawn(player.capabilities.isCreativeMode);
        return true;
    }
    
    void despawn(boolean creativeModeHit) {
        if (worldObj.isRemote) {
            return;
        }
        setDead();
        if (creativeModeHit) {
            return;
        }
        ArrayList<ItemStack> toDrop = new ArrayList();
        toDrop.add(new ItemStack(Core.registry.servo_motor_placer));
        for (ItemStack is : inv) {
            toDrop.add(is);
        }
        if (socket != null && !(socket instanceof SocketEmpty)) {
            socket.uninstall();
            toDrop.add(new ItemStack(Core.registry.socket_part, 1, socket.getFactoryType().md));
        }
        dropItemStacks(toDrop);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public String toString() {
        return super.toString() + (worldObj.isRemote ? " client" : " server") + " " + getProperSpeed();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int three) {
        // The servers sends crappy packets. TODO: Sure would be nice if it didn't send those packets...
    }

    @Override
    public void setPosition(double x, double y, double z) {
        // super.setPosition(x, y, z); //Super does some stupid shit to the bounding box.
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        double neg_size = -0.25;
        double pos_size = 0.75;
        double height = 2F/16F;
        double dy = 0.5;
        this.boundingBox.setBounds(x - neg_size, dy + y - height, z - neg_size, x + pos_size, dy + y + height, z + pos_size);
    }

    @Override
    public void setPositionAndRotation(double x, double y, double z, float yaw, float pitch) {
        super.setPositionAndRotation(x, y, z, yaw, pitch);
    }

    void describe() {
        broadcast(MessageType.motor_description,
                speed_b,
                pos_progress,
                pos_prev.asDeltaCoord(),
                pos_next.asDeltaCoord(),
                (byte) orientation.ordinal(),
                (byte) nextDirection.ordinal()
                //, (boolean) stopped
                );
        for (int i = 0; i < inv_last_sent.length; i++) {
            inv_last_sent[i] = EMPTY_ITEM; //makes sure everything gets updated properly.
        }
        onInventoryChanged();
    }
    
    @Override
    public boolean handleMessageFromClient(short messageType, DataInputStream input) throws IOException {
        if (messageType == MessageType.DataHelperEdit) {
            DataInPacketClientEdited di = new DataInPacketClientEdited(input);
            socket.serialize("", di);
            onInventoryChanged();
            return true;
        }
        return false;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public boolean handleMessageFromServer(short messageType, DataInputStream input)
            throws IOException {
        switch (messageType) {
        case MessageType.OpenDataHelperGui:
            if (!worldObj.isRemote) {
                return false;
            } else {
                DataInPacket dip = new DataInPacket(input, Side.CLIENT);
                socket.serialize("", dip);
                Minecraft.getMinecraft().displayGuiScreen(new GuiDataConfig(socket, this));
            }
            break;
        case MessageType.motor_inventory:
            for (int i = 0; i < inv.length; i++) {
                inv[i] = null;
            }
            while (true) {
                byte index = input.readByte();
                if (index < 0) {
                    break;
                }
                inv[index] = FactorizationUtil.readStack(input);
            }
            return true;
        case MessageType.motor_description:
            speed_b = input.readByte();
            pos_progress = input.readFloat();
            pos_prev.set(DeltaCoord.read(input));
            pos_next.set(DeltaCoord.read(input));
            interpolatePosition(pos_progress);
            //$FALL-THROUGH$~~~~~!
        case MessageType.motor_direction:
            orientation = FzOrientation.getOrientation(input.readByte());
            nextDirection = ForgeDirection.getOrientation(input.readByte());
            return true;
        }
        return socket.handleMessageFromServer(messageType, input);
    }

    public double getProperSpeed() {
        double perc = speed_b/(max_speed_b);
        return speed_limit*perc;
    }
    
    public void dropItemStacks(Iterable<ItemStack> toDrop) {
        for (ItemStack is : toDrop) {
            FactorizationUtil.spawnItemStack(this, is);
        }
    }
    
    public ServoStack getServoStack(int stackId) {
        stackId = Math.max(0, stackId);
        stackId = Math.min(stackId, STACKS);
        return stacks[stackId];
    }
    
    public void putError(Object error) {
        if (!worldObj.isRemote) {
            Notify.send(getCurrentPos(), "%s", error.toString());
        }
        ServoStack ss = getServoStack(STACK_ERRNO);
        if (ss.getFreeSpace() <= 0) {
            ss.popEnd();
        }
        ss.push(error);
    }

    @Override
    public int getSizeInventory() {
        return inv.length;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        return inv[i];
    }

    @Override
    public ItemStack decrStackSize(int i, int j) {
        ItemStack ret = inv[i].splitStack(j);
        inv[i] = FactorizationUtil.normalize(inv[i]);
        return ret;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int i) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack) {
        inv[i] = itemstack;
    }

    @Override
    public String getInvName() {
        return "Servo Motor Inventory";
    }

    @Override
    public boolean isInvNameLocalized() {
        return true;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    private static final ItemStack EMPTY_ITEM = new ItemStack(0, 0, 0);
    @Override
    public void onInventoryChanged() {
        ArrayList<Object> toSend = new ArrayList(18);
        for (byte i = 0; i < inv.length; i++) {
            if (FactorizationUtil.identical(inv[i], inv_last_sent[i])) {
                continue;
            }
            if (inv[i] != null) {
                toSend.add(i);
                toSend.add(inv[i]);
            }
            inv_last_sent[i] = inv[i];
        }
        if (toSend.isEmpty()) {
            return;
        }
        toSend.add(-1);
        broadcast(MessageType.motor_inventory, toSend.toArray());
        getCurrentPos().getChunk().setChunkModified();
        getNextPos().getChunk().setChunkModified();
    }
    
    @Override
    public boolean isUseableByPlayer(EntityPlayer entityplayer) {
        return false;
    }

    @Override
    public void openChest() { }

    @Override
    public void closeChest() { }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        return true;
    }
    
    private FzInv my_fz_inv = FactorizationUtil.openInventory(this, 0);
    public FzInv getInv() {
        return my_fz_inv;
    }
    
    public ItemStack getHeldItem() {
        for (int i = 0; i < inv.length; i++) {
            ItemStack is = inv[i];
            if (is != null) {
                return is;
            }
        }
        return null;
    }

    private final ArrayList<MovingObjectPosition> ret = new ArrayList<MovingObjectPosition>();
    ArrayList<MovingObjectPosition> rayTrace() {
        ret.clear();
        final Coord c = getCurrentPos();
        final ForgeDirection top = orientation.top;
        final ForgeDirection face = orientation.facing;
        final ForgeDirection right = face.getRotation(top);
        
        AxisAlignedBB ab = AxisAlignedBB.getAABBPool().getAABB(
                c.x + top.offsetX, c.y + top.offsetY, c.z + top.offsetZ,  
                c.x + 1 + top.offsetX, c.y + 1 + top.offsetY, c.z + 1 + top.offsetZ);
        for (Entity entity : (Iterable<Entity>)worldObj.getEntitiesWithinAABBExcludingEntity(this, ab)) {
            if (!entity.canBeCollidedWith()) {
                continue;
            }
            ret.add(new MovingObjectPosition(entity));
        }
        
        nullVec.xCoord = nullVec.yCoord = nullVec.zCoord = 0;
        Coord targetBlock = c.add(top);
        mopBlock(ret, targetBlock, top.getOpposite()); //nose-to-nose with the servo
        mopBlock(ret, targetBlock.add(top), top.getOpposite()); //a block away
        mopBlock(ret, targetBlock.add(top.getOpposite()), top);
        if (ret.size() == 0) {
            mopBlock(ret, targetBlock.add(face), face.getOpposite()); //running forward
            mopBlock(ret, targetBlock.add(face.getOpposite()), face); //running backward
            if (ret.size() == 0) {
                mopBlock(ret, targetBlock.add(right), right.getOpposite()); //to the servo's right
                mopBlock(ret, targetBlock.add(right.getOpposite()), right); //to the servo's left
            }
        }
        return ret;
    }
    
    private static final Vec3 nullVec = Vec3.createVectorHelper(0, 0, 0);
    void mopBlock(ArrayList<MovingObjectPosition> list, Coord target, ForgeDirection side) {
        if (target.isAir()) {
            return;
        }
        /*AxisAlignedBB aabb = target.getCollisionBoundingBoxFromPool();
        if (aabb == null) {
            return;
        }*/
        list.add(target.createMop(side, nullVec));
    }
    
    @Override
    public boolean dumpBuffer(List<ItemStack> buffer) {
        if (buffer.isEmpty()) {
            return false;
        }
        FzInv me = FactorizationUtil.openInventory(this, false);
        ItemStack got = buffer.get(0);
        if (got == null) {
            buffer.remove(0);
            return true;
        }
        ItemStack res = me.push(buffer.get(0));
        if (res == null) {
            buffer.remove(0);
        } else {
            buffer.set(0, res);
        }
        return true;
    }
    
    @Override
    public boolean extractCharge(int amount) {
        IChargeConductor wire = getCurrentPos().getTE(IChargeConductor.class);
        if (wire == null) {
            return false;
        }
        return wire.getCharge().tryTake(amount) >= amount;
    }
    
    @Override
    public void sendMessage(int msgType, Object... msg) {
        Packet toSend = Core.network.entityPacket(this, msgType, msg);
        Core.network.broadcastPacket(worldObj, (int) posX, (int) posY, (int) posZ, toSend); 
    }
}
