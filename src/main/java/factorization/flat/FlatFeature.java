package factorization.flat;

import factorization.api.Coord;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataOutNBT;
import factorization.coremodhooks.IExtraChunkData;
import factorization.shared.Core;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.FMLControlledNamespacedRegistry;
import net.minecraftforge.fml.common.registry.PersistentRegistryManager;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nonnull;
import java.io.IOException;

public enum FlatFeature implements FMLControlledNamespacedRegistry.AddCallback<FlatFace> {
    INSTANCE;

    private boolean initialized = false;
    public void init() {
        if (initialized) return;
        initialized = true;
        Core.loadBus(this);
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            Core.loadBus(new FlatRender());
        }
    }

    static final ResourceLocation FLATS = new ResourceLocation("factorization:flats");
    static final int NO_FACE = 0;
    static final int DYNAMIC_SENTINEL = 1;
    static final char MAX_ID = Character.MAX_VALUE;
    static final char MIN_ID = DYNAMIC_SENTINEL + 1;
    static final FMLControlledNamespacedRegistry<FlatFace> registry = PersistentRegistryManager.createRegistry(FLATS, FlatFace.class, null /* default flat value */, MAX_ID, MIN_ID, false, INSTANCE);
    static final String NBT_KEY = "fz:flats";

    static byte side2byte(EnumFacing side) {
        return (byte) side.getAxis().ordinal();
    }

    static EnumFacing byte2side(int b) {
        return sideLookup[b & 0x3];
    }

    private static final EnumFacing sideLookup[] = new EnumFacing[] {
            EnumFacing.EAST,
            EnumFacing.UP,
            EnumFacing.SOUTH
    };

    @SubscribeEvent
    public void loadChunk(ChunkDataEvent.Load event) {
        Chunk chunk = event.getChunk();
        FlatChunkLayer layer = ((IExtraChunkData) chunk).getFlatLayer();
        NBTTagCompound chunkData = event.getData();
        if (!chunkData.hasKey(NBT_KEY)) return;
        NBTTagList list = chunkData.getTagList(NBT_KEY, Constants.NBT.TAG_COMPOUND);
        if (list == null || list.hasNoTags()) return;
        Coord at = new Coord(chunk.getWorld(), 0, 0, 0);
        int tagEnd = list.tagCount();
        for (int i = 0; i < tagEnd; i++) {
            NBTTagCompound tag = (NBTTagCompound) list.get(i);
            at.x = tag.getInteger("x");
            at.y = tag.getInteger("y");
            at.z = tag.getInteger("z");
            byte faceOrdinal = tag.getByte("side");
            EnumFacing face = EnumFacing.VALUES[faceOrdinal];
            FlatFace flat = construct(tag);
            if (flat != null) {
                layer.set(at, face, flat);
            }
        }
    }

    @SubscribeEvent
    public void saveChunk(ChunkDataEvent.Save event) {
        final Chunk chunk = event.getChunk();
        final FlatChunkLayer layer = ((IExtraChunkData) chunk).getFlatLayer();
        final NBTTagList out = new NBTTagList();
        layer.iterate(chunk, new IFlatVisitor() {
            @Override
            public void visit(Coord at, EnumFacing side, @Nonnull FlatFace face) {
                NBTTagCompound tag = new NBTTagCompound();
                if (face.isStatic()) {
                    tag.setInteger("static", face.staticId);
                } else {
                    try {
                        face.serialize("", new DataOutNBT(tag));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                tag.setInteger("x", at.x);
                tag.setInteger("y", at.y);
                tag.setInteger("z", at.z);
                tag.setByte("side", (byte) side.ordinal());
            }
        });
        event.getData().setTag(NBT_KEY, out);
    }

    private FlatFace construct(NBTTagCompound tag) {
        if (tag.hasKey("static")) {
            byte id = tag.getByte("static");
            return registry.getObjectById(id);
        }
        String name = tag.getString("dynamic");
        Class<? extends FlatFace> c = Flat.dynamicReg.get(new ResourceLocation(name));
        try {
            FlatFace ret = c.newInstance();
            DataInNBT dataInNBT = new DataInNBT(tag);
            ret.serialize("", dataInNBT);
            return ret;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    @Override
    public void onAdd(FlatFace face, int id) {
        if (id >= MAX_ID) {
            throw new IllegalArgumentException("ID is too high!");
        }
        if (id < MIN_ID) {
            throw new IllegalArgumentException("ID is too low!");
        }
        face.staticId = (char) id;
    }
}
