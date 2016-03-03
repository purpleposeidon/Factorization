package factorization.flat;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import factorization.api.Coord;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataOutNBT;
import factorization.coremodhooks.IExtraChunkData;
import factorization.flat.api.*;
import factorization.flat.render.ClientRenderInfo;
import factorization.flat.render.EntityHack;
import factorization.flat.render.EntityHackRender;
import factorization.flat.render.FlatModel;
import factorization.shared.Core;
import factorization.util.NORELEASE;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.FMLControlledNamespacedRegistry;
import net.minecraftforge.fml.common.registry.PersistentRegistryManager;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;

@Mod(
        modid = FlatMod.modId,
        name = FlatMod.name,
        version = Core.version
)
public class FlatMod {
    public static final String modId = Core.modId + ".flat";
    public static final String name = "Factorization Miscellaneous Nonsense";

    public static FlatMod INSTANCE;

    public static Logger log;

    public FlatMod() {
        INSTANCE = this; // @Mod.Instance doesn't work.
        Core.loadBus(this);
        Core.loadBus(proxy);
        Flat.registerDynamic(new ResourceLocation("flat:air"), FlatFaceAir.class);
        proxy.initClient();
        FlatNet.instance.init();
    }

    @Mod.EventHandler
    public void init(FMLPreInitializationEvent event) {
        log = event.getModLog();
        //FzUtil.setCoreParent(event);
    }

    @SidedProxy
    public static ServerProxy proxy;

    public static class ServerProxy {
        void initClient() { }
        IFlatRenderInfo constructRenderInfo() {
            return IFlatRenderInfo.NULL;
        }
    }

    public static class ClientProxy extends ServerProxy {
        final Minecraft mc = Minecraft.getMinecraft();

        @SubscribeEvent
        public void loadModels(TextureStitchEvent.Pre event) {
            for (FlatFace face : Flat.getAll()) {
                face.loadModels(new IModelMaker() {
                    @Nullable
                    @Override
                    public IFlatModel getModel(ResourceLocation url) {
                        return new FlatModel(url);
                    }
                });
            }
        }

        @Override
        void initClient() {
            setupEntityHack();
            redrawWhenSettingsChange();
            FlatRayTracer.INSTANCE.init();
        }

        private void redrawWhenSettingsChange() {
            IResourceManagerReloadListener redrawFlats = new IResourceManagerReloadListener() {
                @Override
                public void onResourceManagerReload(IResourceManager resourceManager) {
                    if (mc.theWorld == null) return;
                    IChunkProvider cp = mc.theWorld.getChunkProvider();
                    if (cp instanceof ChunkProviderClient) {
                        ChunkProviderClient cpc = (ChunkProviderClient) cp;
                        for (Chunk chunk : cpc.chunkListing) {
                            FlatChunkLayer fcl = ((IExtraChunkData) chunk).getFlatLayer();
                            fcl.renderInfo.markDirty(null);
                        }
                    }
                }
            };
            ((SimpleReloadableResourceManager) mc.getResourceManager()).registerReloadListener(redrawFlats);
        }

        private void setupEntityHack() {
            RenderingRegistry.registerEntityRenderingHandler(EntityHack.class, new IRenderFactory<EntityHack>() {
                @Override
                public Render<? super EntityHack> createRenderFor(RenderManager manager) {
                    return new EntityHackRender(manager);
                }
            });
        }

        @Override
        IFlatRenderInfo constructRenderInfo() {
            if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) return IFlatRenderInfo.NULL;
            return new ClientRenderInfo();
        }
    }

    static final ResourceLocation FLATS = new ResourceLocation("factorization:flats");
    static final int NO_FACE = 0;
    public static final char DYNAMIC_SENTINEL = 1;
    static final char MAX_ID = Character.MAX_VALUE;
    static final char MIN_ID = DYNAMIC_SENTINEL + 1;
    public static final FMLControlledNamespacedRegistry<FlatFace> staticReg = PersistentRegistryManager.createRegistry(FLATS, FlatFace.class, null /* default flat value */, MAX_ID, MIN_ID, false, new FMLControlledNamespacedRegistry.AddCallback<FlatFace>() {
        @Override
        public void onAdd(FlatFace face, int id) {
            if (id == DYNAMIC_SENTINEL) {
                throw new IllegalArgumentException("Tried to register FlatFace over DYNAMIC_SENTINEL id");
            }
            if (id == 0) {
                throw new IllegalStateException("Tried to register FlatFace over null id");
            }
            if (id >= MAX_ID) {
                throw new IllegalArgumentException("ID is too high!");
            }
            face.staticId = (char) id;
        }
    });
    public static final BiMap<ResourceLocation, Class<? extends FlatFace>> dynamicReg = HashBiMap.create();
    public static final HashMap<ResourceLocation, FlatFace> dynamicSamples = Maps.newHashMap();
    static final String NBT_KEY = "fz:flats";

    public static byte side2byte(EnumFacing side) {
        return (byte) side.getAxis().ordinal();
    }

    public static EnumFacing byte2side(int b) {
        return sideLookup[b % 3];
    }

    static final EnumFacing sideLookup[] = new EnumFacing[] {
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
        NORELEASE.println(chunk + " has " + tagEnd);
    }

    @SubscribeEvent
    public void saveChunk(ChunkDataEvent.Save event) {
        final Chunk chunk = event.getChunk();
        final FlatChunkLayer layer = ((IExtraChunkData) chunk).getFlatLayer();
        final NBTTagList out = new NBTTagList();
        layer.iterate(new IFlatVisitor() {
            @Override
            public void visit(Coord at, EnumFacing side, @Nonnull FlatFace face) {
                if (face.isNull()) return;
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
                out.appendTag(tag);
            }
        });
        event.getData().setTag(NBT_KEY, out);
    }

    private FlatFace construct(NBTTagCompound tag) {
        if (tag.hasKey("static")) {
            int id = tag.getInteger("static");
            return staticReg.getObjectById(id);
        }
        String name = tag.getString("dynamic");
        Class<? extends FlatFace> c = dynamicReg.get(new ResourceLocation(name));
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

    @SubscribeEvent
    public void discardChunkRenderInfo(ChunkEvent.Unload event) {
        IExtraChunkData cd = (IExtraChunkData) event.getChunk();
        cd.getFlatLayer().discard();
    }

    @SubscribeEvent
    public void addWorldEventListener(WorldEvent.Load event) {
        event.world.addWorldAccess(new FlatWorldEventListener(event.world));
    }
}
