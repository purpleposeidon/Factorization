package factorization.fzds;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import factorization.api.Coord;
import factorization.api.Quaternion;
import factorization.fzds.api.IDeltaChunk;
import factorization.fzds.api.IFzdsShenanigans;
import factorization.shared.Core;
import factorization.shared.FzUtil;


public class HammerNet {
    public static HammerNet instance;
    public static final String channelName = "FZDS|Interact"; //NORELEASE: There's another network thingie around here for DSE velocity & stuff!?
    public static FMLEventChannel channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(channelName);
    
    public HammerNet() {
        instance = this;
        channel.register(this);
    }
    
    public static class HammerNetType {
        // Next time, make it an enum.
        public static final byte rotation = 0, rotationVelocity = 1, rotationBoth = 2, rotationCenterOffset = 10,
                rightClickEntity = 3, leftClickEntity = 4, rightClickBlock = 5, leftClickBlock = 6, digStart = 7, digProgress = 8, digFinish = 9;
    }
    
    @SubscribeEvent
    public void messageFromServer(ClientCustomPacketEvent event) {
        EntityPlayer player = Core.proxy.getClientPlayer();
        try {
            handleMessageFromServer(player, event.packet.payload());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void handleMessageFromServer(EntityPlayer player, ByteBuf dis) throws IOException {
        if (player == null || player.worldObj == null) {
            return; //Blah, what...
        }
        World world = player.worldObj;
        byte type = dis.readByte();
        int dse_id = dis.readInt();
        Entity ent = world.getEntityByID(dse_id);
        DimensionSliceEntity dse = null;
        if (ent instanceof DimensionSliceEntity) {
            dse = (DimensionSliceEntity) ent;
        } else {
            return;
        }
        switch (type) {
        case HammerNetType.rotation:
            setRotation(dis, dse);
            break;
        case HammerNetType.rotationVelocity:
            setRotationalVelocity(dis, dse);
            break;
        case HammerNetType.rotationBoth:
            setRotation(dis, dse);
            setRotationalVelocity(dis, dse);
            break;
        case HammerNetType.rotationCenterOffset:
            setCenterOffset(dis, dse);
            break;
        }
        
    }
    
    void setRotation(ByteBuf dis, DimensionSliceEntity dse) throws IOException {
        Quaternion q = Quaternion.read(dis);
        if (dse != null) {
            dse.setRotation(q);
        }
    }
    
    void setRotationalVelocity(ByteBuf dis, DimensionSliceEntity dse) throws IOException {
        Quaternion q = Quaternion.read(dis);
        if (dse != null) {
            dse.setRotationalVelocity(q);
        }
    }
    
    void setCenterOffset(ByteBuf dis, DimensionSliceEntity dse) throws IOException {
        double x = dis.readDouble();
        double y = dis.readDouble();
        double z = dis.readDouble();
        Vec3 vec = Vec3.createVectorHelper(x, y, z);
        dse.setRotationalCenterOffset(vec);
    }
    
    
    @SubscribeEvent
    public void messageFromClient(ServerCustomPacketEvent event) {
        EntityPlayerMP player = ((NetHandlerPlayServer) event.handler).playerEntity;
        try {
            handleMessageFromClient(player, event.packet.payload());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    void handleMessageFromClient(EntityPlayerMP player, ByteBuf dis) throws IOException {
        byte type = dis.readByte();
        int dse_id = dis.readInt();
        Entity ent = player.worldObj.getEntityByID(dse_id);
        if (!(ent instanceof IDeltaChunk)) {
            throw new IOException("Did not select a DimensionSliceEntity (id = " + dse_id + ", messageType = " + type + ")");
        }
        IDeltaChunk idc = (IDeltaChunk) ent;
        if (type == HammerNetType.digFinish || type == HammerNetType.digProgress || type == HammerNetType.digStart) {
            int x = dis.readInt();
            int y = dis.readInt();
            int z = dis.readInt();
            byte sideHit = dis.readByte();
            if (type == HammerNetType.digFinish) {
                breakBlock(idc, player, dis, x, y, z, sideHit);
            } else if (type == HammerNetType.digStart) {
                punchBlock(idc, player, dis, x, y, z, sideHit);
            }
        } else if (type == HammerNetType.rightClickBlock) {
            int x = dis.readInt();
            int y = dis.readInt();
            int z = dis.readInt();
            byte sideHit = dis.readByte();
            float vecX = dis.readFloat();
            float vecY = dis.readFloat();
            float vecZ = dis.readFloat();
            clickBlock(idc, player, x, y, z, sideHit, vecX, vecY, vecZ);
        }
    }
    
    boolean blockInReach(IDeltaChunk idc, EntityPlayerMP player, Coord at) {
        double reach_distance = player.theItemInWorldManager.getBlockReachDistance();
        Vec3 playerAt = player.getPosition(0);
        playerAt = idc.real2shadow(playerAt);
        double distance = at.createVector().distanceTo(playerAt);
        return distance <= reach_distance;
    }
    
    void breakBlock(IDeltaChunk idc, EntityPlayerMP player, ByteBuf dis, int x, int y, int z, byte sideHit) {
        Coord at = new Coord(DeltaChunk.getServerShadowWorld(), x, y, z);
        if (at.isAir()) return;
        if (!blockInReach(idc, player, at)) return;
        World origWorld = player.theItemInWorldManager.theWorld;
        player.theItemInWorldManager.theWorld = DeltaChunk.getServerShadowWorld();
        try {
            // NORELEASE: Not quite right; this will send packets to the player, not through the proxy
            player.theItemInWorldManager.tryHarvestBlock(at.x, at.y, at.z);
        } finally {
            player.theItemInWorldManager.theWorld = origWorld;
        }
    }
    
    void punchBlock(IDeltaChunk idc, EntityPlayerMP player, ByteBuf dis, int x, int y, int z, byte sideHit) {
        Coord at = new Coord(DeltaChunk.getServerShadowWorld(), x, y, z);
        if (at.isAir()) return;
        if (!blockInReach(idc, player, at)) return;
        Block block = at.getBlock();
        WorldServer shadow_world = (WorldServer) DeltaChunk.getServerShadowWorld();
        InteractionLiason liason = getLiason(shadow_world, player);
        block.onBlockClicked(shadow_world, x, y, z, liason);
        liason.finishUsingLiason();
    }
    
    InteractionLiason getLiason(WorldServer shadowWorld, EntityPlayer real_player) {
        InteractionLiason liason = new InteractionLiason(shadowWorld, new ItemInWorldManager(shadowWorld));
        liason.inventory = real_player.inventory;
        return liason;
    }
    
    static class InteractionLiason extends EntityPlayerMP implements IFzdsShenanigans {
        private static final GameProfile liasonGameProfile = new GameProfile(null /*UUID.fromString("69f64f91-665e-457d-ad32-f6082d0b8a71")*/ , "[FzdsInteractionLiason]");
        InventoryPlayer original_inventory;
        public InteractionLiason(WorldServer world, ItemInWorldManager itemManager) {
            super(MinecraftServer.getServer(), world, liasonGameProfile, itemManager);
            original_inventory = this.inventory;
        }
        
        void finishUsingLiason() {
            // Stuff? Drop our items? Die?
            this.inventory = original_inventory;
        }
    }
    
    private boolean do_click(WorldServer world, EntityPlayerMP player, int x, int y, int z, byte sideHit, float vecX, float vecY, float vecZ) {
        // Copy of PlayerControllerMP.onPlayerRightClick
        ItemStack is = player.getHeldItem();
        if (is != null && is.getItem().onItemUseFirst(is, player, world, x, y, z, sideHit, vecX, vecY, vecZ)) {
            return true;
        }
        boolean ret = false;

        if (!player.isSneaking() || player.getHeldItem() == null
                || player.getHeldItem().getItem().doesSneakBypassUse(world, x, y, z, player)) {
            ret = world.getBlock(x, y, z).onBlockActivated(world, x, y, z, player, sideHit, vecX, vecY, vecZ);
        }

        if (!ret && is != null && is.getItem() instanceof ItemBlock) {
            ItemBlock itemblock = (ItemBlock) is.getItem();

            if (!itemblock.func_150936_a(world, x, y, z, sideHit, player, is)) {
                return false;
            }
        }

        if (ret) {
            return true;
        } else if (is == null) {
            return false;
        } else if (FzUtil.isPlayerCreative(player)) {
            int j1 = is.getItemDamage();
            int i1 = is.stackSize;
            boolean flag1 = is.tryPlaceItemIntoWorld(player, world, x, y, z,
                    sideHit, vecX, vecY, vecZ);
            is.setItemDamage(j1);
            is.stackSize = i1;
            return flag1;
        } else {
            if (!is.tryPlaceItemIntoWorld(player, world, x, y, z, sideHit, vecX, vecY, vecZ)) {
                return false;
            }
            if (is.stackSize <= 0) {
                MinecraftForge.EVENT_BUS.post(new PlayerDestroyItemEvent(player, is));
            }
            return true;
        }
    }
    
    void clickBlock(IDeltaChunk idc, EntityPlayerMP real_player, int x, int y, int z, byte sideHit, float vecX, float vecY, float vecZ) throws IOException {
        WorldServer shadowWorld = (WorldServer) DeltaChunk.getServerShadowWorld();
        Coord at = new Coord(shadowWorld, x, y, z);
        if (at.isAir()) return;
        if (!blockInReach(idc, real_player, at)) return;
        
        InteractionLiason liason = getLiason(shadowWorld, real_player);
        try {
            do_click(shadowWorld, liason, x, y, z, sideHit, vecX, vecY, vecZ);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        liason.finishUsingLiason();
        
        /*
         * Create an InteractionLiason extends EntityPlayerMP to click the block.
         * If it recieves any packets to open a GUI, or possibly just any strange packets in general, then teleport the player into Hammer-space.
         * The player will be riding a mount.
         * When the player gets off the mount, teleport the player back.
         * Some GUIs might be whitelisted. Crafting tables shouldn't be too hard; chests shouldn't be any harder; furnaces might be achievable.
         */
    }
    
    static FMLProxyPacket makePacket(byte type, Object... items) {
        ByteArrayDataOutput dos = ByteStreams.newDataOutput();
        dos.writeByte(type);
        for (int i = 0; i < items.length; i++) {
            Object obj = items[i];
            if (obj instanceof Quaternion) {
                ((Quaternion) obj).write(dos);
            } else if (obj instanceof Integer) {
                dos.writeInt((Integer) obj);
            } else if (obj instanceof Byte) {
                dos.writeByte((Byte) obj);
            } else if (obj instanceof Float) {
                dos.writeFloat((Float) obj);
            } else if (obj instanceof MovingObjectPosition) { 
                MovingObjectPosition mop = (MovingObjectPosition) obj;
                dos.writeInt(mop.blockX);
                dos.writeInt(mop.blockY);
                dos.writeInt(mop.blockZ);
                dos.writeByte((byte) mop.sideHit);
            } else if (obj instanceof Vec3) {
                Vec3 vec = (Vec3) obj;
                dos.writeDouble(vec.xCoord);
                dos.writeDouble(vec.yCoord);
                dos.writeDouble(vec.zCoord);
            } else {
                throw new IllegalArgumentException("Can only do Quaternions/Integers/Bytes/Floats/MovingObjectPosition/Vec3!");
            }
        }
        return new FMLProxyPacket(Unpooled.wrappedBuffer(dos.toByteArray()), channelName);
    }
}
