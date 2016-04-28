package factorization.fzds.network;

import java.io.IOException;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.world.BlockEvent.PlaceEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

import factorization.api.Coord;
import factorization.api.Quaternion;
import factorization.fzds.DeltaChunk;
import factorization.fzds.DimensionSliceEntity;
import factorization.fzds.interfaces.DeltaCapability;
import factorization.fzds.interfaces.IDimensionSlice;
import factorization.shared.Core;
import factorization.util.PlayerUtil;
import factorization.util.SpaceUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


public class HammerNet {
    public static HammerNet instance;
    public static final String channelName = "FZDS|Interact"; //NORELEASE: There's another network thingie around here for DSE velocity & stuff!?
    public static FMLEventChannel channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(channelName); // NORELEASE: Ack that for the dupe

    public HammerNet() {
        instance = this;
        channel.register(this);
        Core.loadBus(this);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void playerLoggedIn(FMLNetworkEvent.ServerConnectionFromClientEvent event) {
        PacketJunction.setup(event, Side.SERVER);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    @SideOnly(Side.CLIENT)
    public void clientLoggedIn(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        PacketJunction.setup(event, Side.CLIENT);
    }

    public static class HammerNetType {
        // Next time, make it an enum.
        public static final byte rightClickEntity = 3, leftClickEntity = 4, rightClickBlock = 5, leftClickBlock = 6, digStart = 7, digProgress = 8, digFinish = 9;
    }

    @SubscribeEvent
    public void messageFromClient(ServerCustomPacketEvent event) {
        MinecraftServer.getServer().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                EntityPlayerMP player = ((NetHandlerPlayServer) event.handler).playerEntity;
                try {
                    handleMessageFromClient(player, event.packet.payload());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    void handleMessageFromClient(EntityPlayerMP player, ByteBuf dis) throws IOException {
        byte type = dis.readByte();
        int dse_id = dis.readInt();
        Entity ent = player.worldObj.getEntityByID(dse_id);
        if (!(ent instanceof IDimensionSlice)) {
            throw new IOException("Did not select a DimensionSliceEntity (id = " + dse_id + ", messageType = " + type + ")");
        }
        DimensionSliceEntity idc = (DimensionSliceEntity) ent;
        
        if (!idc.can(DeltaCapability.INTERACT)) {
            if (type == HammerNetType.digFinish || type == HammerNetType.digProgress || type == HammerNetType.digStart || type == HammerNetType.rightClickBlock || type == HammerNetType.leftClickBlock) {
                Core.logWarning("%s tried to interact with IDC that doesn't permit that %s", player, idc);
                return;
            }
        }

        if (type == HammerNetType.digFinish || type == HammerNetType.digProgress || type == HammerNetType.digStart) {
            if (!idc.can(DeltaCapability.BLOCK_MINE)) {
                Core.logWarning("%s tried to mine IDC that doesn't permit that %s", player, idc);
                return;
            }
            int x = dis.readInt();
            int y = dis.readInt();
            int z = dis.readInt();
            BlockPos pos = new BlockPos(x, y, z);
            EnumFacing sideHit = EnumFacing.getFront(dis.readByte());
            if (type == HammerNetType.digFinish) {
                breakBlock(idc, player, dis, pos, sideHit);
            } else if (type == HammerNetType.digStart) {
                punchBlock(idc, player, dis, pos, sideHit);
            }
            idc.blocksChanged(x, y, z);
        } else if (type == HammerNetType.rightClickBlock) {
            /*if (!idc.can(DeltaCapability.BLOCK_PLACE)) {
                Core.logWarning("%s tried to use an item on IDC that doesn't permit that %s", player, idc);
                return;
            }*/
            int x = dis.readInt();
            int y = dis.readInt();
            int z = dis.readInt();
            BlockPos pos = new BlockPos(x, y, z);
            EnumFacing sideHit = EnumFacing.getFront(dis.readByte());
            float vecX = dis.readFloat();
            float vecY = dis.readFloat();
            float vecZ = dis.readFloat();
            try {
                dont_check_range = false;
                active_idc = idc;
                clickBlock(idc, player, pos, sideHit, vecX, vecY, vecZ);
            } finally {
                dont_check_range = true;
                active_idc = null;
            }
            idc.blocksChanged(x, y, z);
        } else if (type == HammerNetType.leftClickBlock) {
            int x = dis.readInt();
            int y = dis.readInt();
            int z = dis.readInt();
            EnumFacing sideHit = EnumFacing.getFront(dis.readByte());
            float vecX = dis.readFloat();
            float vecY = dis.readFloat();
            float vecZ = dis.readFloat();
            BlockPos pos = new BlockPos(x, y, z);
            leftClickBlock(idc, player, dis, pos, sideHit, vecX, vecY, vecZ);
        } else if (type == HammerNetType.rightClickEntity || type == HammerNetType.leftClickEntity) {
            int entId = dis.readInt();
            Entity hitEnt = idc.getMinCorner().w.getEntityByID(entId);
            if (hitEnt == null) {
                Core.logWarning("%s tried clicking a non-existing entity", player);
                return;
            }
            clickEntity(idc, player, hitEnt, type == HammerNetType.leftClickEntity);
        } else {
            Core.logWarning("%s tried to send an unknown packet %s to IDC %s", player, type, idc);
        }
    }
    
    private boolean dont_check_range = true;
    private IDimensionSlice active_idc = null;
    
    @SubscribeEvent
    public void handlePlace(PlaceEvent event) {
        if (dont_check_range) return;
        if (active_idc == null) return;
        if (!active_idc.can(DeltaCapability.BLOCK_PLACE)) {
            event.setCanceled(true);
            return;
        }
        cancelOutOfRangePlacements(event);
        if (!event.isCanceled()) {
            askController(event);
        }
    }

    void cancelOutOfRangePlacements(PlaceEvent event) {
        Coord min = active_idc.getMinCorner();
        if (event.world != min.w) return;
        Coord max = active_idc.getMaxCorner();
        BlockPos pos = event.blockSnapshot.pos;
        if (in(min.x, pos.getX(), max.x) && in(min.y, pos.getY(), max.y) && in(min.z, pos.getZ(), max.z)) return;
        event.setCanceled(true);
    }

    void askController(PlaceEvent event) {
        if (active_idc.getController().placeBlock(active_idc, event.player, new Coord(event.world, event.blockSnapshot.pos))) {
            event.setCanceled(true);
        }
    }
    
    boolean in(int low, int i, int high) {
        return low <= i && i <= high;
    }
    
    boolean blockInReach(IDimensionSlice idc, EntityPlayerMP player, Coord at) {
        double reach_distance = player.theItemInWorldManager.getBlockReachDistance();
        Vec3 playerAt = SpaceUtil.fromEntPos(player);
        playerAt = idc.real2shadow(playerAt);
        double distance = at.createVector().distanceTo(playerAt);
        return distance <= reach_distance;
    }
    
    void breakBlock(IDimensionSlice idc, EntityPlayerMP player, ByteBuf dis, BlockPos pos, EnumFacing sideHit) {
        Coord at = new Coord(DeltaChunk.getServerShadowWorld(), pos);
        if (at.isAir()) return;
        if (!blockInReach(idc, player, at)) return;
        if (idc.getController().breakBlock(idc, player, at, sideHit)) return;
        World origWorld = player.theItemInWorldManager.theWorld;
        player.theItemInWorldManager.theWorld = DeltaChunk.getServerShadowWorld();
        try {
            // NORELEASE: Not quite right; this will send packets to the player, not through the proxy
            player.theItemInWorldManager.tryHarvestBlock(at.toBlockPos());
        } finally {
            player.theItemInWorldManager.theWorld = origWorld;
        }
    }
    
    void punchBlock(IDimensionSlice idc, EntityPlayerMP player, ByteBuf dis, BlockPos pos, EnumFacing sideHit) {
        Coord at = new Coord(DeltaChunk.getServerShadowWorld(), pos);
        if (at.isAir()) return;
        if (!blockInReach(idc, player, at)) return;
        if (idc.getController().hitBlock(idc, player, at, sideHit)) return;
        Block block = at.getBlock();
        WorldServer shadow_world = (WorldServer) DeltaChunk.getServerShadowWorld();
        InteractionLiason liason = getLiason(shadow_world, player, idc);
        block.onBlockClicked(shadow_world, pos, liason);
        liason.finishUsingLiason();
    }

    void leftClickBlock(IDimensionSlice idc, EntityPlayerMP player, ByteBuf dis, BlockPos pos, EnumFacing sideHit, float vecX, float vecY, float vecZ) {
        Coord at = new Coord(DeltaChunk.getServerShadowWorld(), pos);
        if (at.isAir()) return;
        if (!blockInReach(idc, player, at)) return;
        if (idc.getController().hitBlock(idc, player, at, sideHit)) return;
        // TODO: Liason?
        Block block = at.getBlock();
        block.onBlockClicked(at.w, pos, player);
    }

    void clickEntity(IDimensionSlice idc, EntityPlayerMP player, Entity hitEnt, boolean leftClick) {
        InteractionLiason liason = getLiason((WorldServer) idc.getMinCorner().w, player, idc);
        if (leftClick) {
            liason.attackTargetEntityWithCurrentItem(hitEnt);
        } else {
            liason.interactWith(hitEnt);
        }
        liason.finishUsingLiason();
    }
    
    InteractionLiason getLiason(WorldServer shadowWorld, EntityPlayerMP real_player, IDimensionSlice idc) {
        // NORELEASE: Cache. Constructing fake players is muy expensivo
        InteractionLiason liason = new InteractionLiason(shadowWorld, new ItemInWorldManager(shadowWorld), real_player, idc);
        liason.initializeFor(idc);
        return liason;
    }

    private boolean do_click(IDimensionSlice idc, WorldServer world, EntityPlayerMP player, BlockPos pos, EnumFacing sideHit, float vecX, float vecY, float vecZ) {
        // Copy of PlayerControllerMP.onPlayerRightClick
        ItemStack is = player.getHeldItem();
        if (is != null && is.getItem().onItemUseFirst(is, player, world, pos, sideHit, vecX, vecY, vecZ)) {
            return true;
        }
        boolean ret = false;

        if (!player.isSneaking() || player.getHeldItem() == null
                || player.getHeldItem().getItem().doesSneakBypassUse(world, pos, player)) {
            IBlockState state = world.getBlockState(pos);
            ret = state.getBlock().onBlockActivated(world, pos, state, player, sideHit, vecX, vecY, vecZ);
        }

        if (ret) {
            return true;
        } else if (is == null || !idc.can(DeltaCapability.BLOCK_PLACE)) {
            return false;
        } else if (PlayerUtil.isPlayerCreative(player)) {
            int j1 = is.getItemDamage();
            int i1 = is.stackSize;
            boolean flag1 = is.onItemUse(player, world, pos,
                    sideHit, vecX, vecY, vecZ);
            is.setItemDamage(j1);
            is.stackSize = i1;
            return flag1;
        } else {
            if (!is.onItemUse(player, world, pos, sideHit, vecX, vecY, vecZ)) {
                return false;
            }
            if (is.stackSize <= 0) {
                MinecraftForge.EVENT_BUS.post(new PlayerDestroyItemEvent(player, is));
            }
            return true;
        }
    }
    
    void clickBlock(IDimensionSlice idc, EntityPlayerMP real_player, BlockPos pos, EnumFacing sideHit, float vecX, float vecY, float vecZ) throws IOException {
        WorldServer shadowWorld = (WorldServer) DeltaChunk.getServerShadowWorld();
        Coord at = new Coord(shadowWorld, pos);
        if (at.isAir()) return;
        if (!blockInReach(idc, real_player, at)) return;
        if (idc.getController().useBlock(idc, real_player, at, sideHit)) return;
        
        InteractionLiason liason = getLiason(shadowWorld, real_player, idc);
        try {
            do_click(idc, shadowWorld, liason, pos, sideHit, vecX, vecY, vecZ);
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

    public static void writePos(ByteArrayDataOutput out, BlockPos pos) {
        out.writeInt(pos.getX());
        out.writeInt(pos.getY());
        out.writeInt(pos.getZ());
    }
    
    public static FMLProxyPacket makePacket(byte type, Object... items) {
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
            } else if (obj instanceof Double) {
                dos.writeDouble((Double) obj);
            } else if (obj instanceof MovingObjectPosition) { 
                MovingObjectPosition mop = (MovingObjectPosition) obj;
                writePos(dos, mop.getBlockPos());
                dos.writeByte((byte) mop.sideHit.ordinal());
            } else if (obj instanceof Vec3) {
                Vec3 vec = (Vec3) obj;
                dos.writeDouble(vec.xCoord);
                dos.writeDouble(vec.yCoord);
                dos.writeDouble(vec.zCoord);
            } else {
                throw new IllegalArgumentException("Can only do Quaternions/Integers/Bytes/Floats/Doubles/MovingObjectPosition/Vec3! Not " + obj);
            }
        }
        return new FMLProxyPacket(new PacketBuffer(Unpooled.wrappedBuffer(dos.toByteArray())), channelName);
    }

    @SubscribeEvent
    public void tickLiasons(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        InteractionLiason.updateActiveLiasons();
    }
}
