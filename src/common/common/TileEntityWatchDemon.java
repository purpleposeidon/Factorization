package factorization.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import net.minecraft.src.Chunk;
import net.minecraft.src.ChunkCoordIntPair;
import net.minecraft.src.Entity;
import net.minecraft.src.FactorizationHack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.World;
import net.minecraft.src.forge.IChunkLoadHandler;
import net.minecraft.src.forge.ISaveEventHandler;

public class TileEntityWatchDemon extends TileEntityCommon {
    public static LoadHandler loadHandler = new LoadHandler();
    static Map<World, HashSet<ChunkCoordIntPair>> world2hash = new HashMap();
    static Chunk mychunk = null;
    int lastXcoord, lastZcoord;

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SENTRYDEMON;
    }

    public static class LoadHandler implements IChunkLoadHandler, ISaveEventHandler {
        static Charset charset = Charset.forName("UTF-8");

        @Override
        public void addActiveChunks(World world, Set<ChunkCoordIntPair> chunkList) {
            //lets the chunks tick
            HashSet<ChunkCoordIntPair> l = world2hash.get(world);
            if (l == null) {
                return;
            }
            chunkList.addAll(l);
        }

        @Override
        public boolean canUnloadChunk(Chunk chunk) {
            HashSet<ChunkCoordIntPair> l = world2hash.get(chunk.worldObj);
            if (l == null) {
                return true;
            }
            boolean canUnload = !l.contains(new ChunkCoordIntPair(chunk.xPosition, chunk.zPosition));
            //			if (canUnload) {
            //				if (TileEntityWatchDemon.chunkContainsDemon(chunk)) {
            //					System.out.println("Tried to unload chunk with demon: " + chunk.xPosition + " " + chunk.zPosition + ": " + canUnload);
            //					return false;
            //				}
            //			}
            return canUnload;
        }

        @Override
        public boolean canUpdateEntity(Entity entity) {
            //....?
            //Uh. I see "Additional Buildcraft Objects" returns true here.
            //But that seems to force all entities to update!
            //I think this is the natural way... but maybe it won't update entities in chunks that would normally be unloaded?
            //I guess that's fine.
            return false;
            //			Chunk c = entity.worldObj.getChunkFromBlockCoords((int) entity.posX, (int) entity.posZ);
            //			return toLoad.contains(c);
        }

        File getSetSave(World world) {
            File world_dir = Core.instance.getWorldSaveDir(world);
            File ret = new File(world_dir.getAbsolutePath(), "factorizationChunkLoader");
            return ret;
        }

        private void doLoadChunks() {
            for (World world : world2hash.keySet()) {
                HashSet<ChunkCoordIntPair> toLoad = world2hash.get(world);
                if (toLoad == null) {
                    continue;
                }
                for (ChunkCoordIntPair coord : toLoad) {
                    FactorizationHack.getChunkProvider(world).loadChunk(coord.chunkXPos, coord.chunkZPosition);
                }
            }
        }

        @Override
        public void onWorldLoad(World world) {
            if (!Core.instance.isCannonical(world)) {
                return;
            }
            HashSet<ChunkCoordIntPair> toLoad = new HashSet<ChunkCoordIntPair>();
            world2hash.put(world, toLoad);
            File f = getSetSave(world);
            if (!f.exists() || !f.canRead()) {
                return;
            }
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(f);
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis, charset));
                while (reader.ready()) {
                    String line = reader.readLine();
                    String bits[] = line.split(Pattern.quote("#"));
                    if (bits == null || bits.length == 0) {
                        continue;
                    }
                    line = bits[0];
                    if (line.length() == 0) {
                        continue;
                    }
                    ArrayList<Integer> ints = new ArrayList(2);
                    try {
                        for (String s : line.split(" ")) {
                            if (s == null || s.length() == 0) {
                                continue;
                            }
                            ints.add(Integer.parseInt(s));
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        continue;
                    }

                    if (ints.size() != 2) {
                        continue;
                    }
                    toLoad.add(new ChunkCoordIntPair(ints.get(0), ints.get(1)));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //addActiveChunks(world, world.activeChunkSet);
            System.out.println(f + " preloaded " + toLoad.size() + " extra chunks");
        }

        @Override
        public void onWorldSave(World world) {
            if (!Core.instance.isCannonical(world)) {
                return;
            }
            FileOutputStream fos = null;
            try {
                File f = getSetSave(world);
                f.createNewFile();
                fos = new FileOutputStream(f);
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(fos, charset));
                out.write("# This is a list of chunks that are kept loaded by Sentry Demons");
                out.newLine();
                out.write("# They will be loaded when the world starts. The adjacent chunks will be loaded after.");
                out.newLine();
                out.newLine();
                HashSet<ChunkCoordIntPair> toLoad = world2hash.get(world);
                if (toLoad == null) {
                    toLoad = new HashSet();
                }
                for (ChunkCoordIntPair coord : toLoad) {
                    Chunk chunk = world.getChunkFromChunkCoords(coord.chunkXPos, coord.chunkZPosition);
                    String coords = coord.chunkXPos + " " + coord.chunkZPosition;
                    out.write(coords);
                    for (int i = 10 - coords.length(); i >= 0; i--) {
                        out.write(" ");
                    }
                    out.write("# At " + (coord.chunkXPos << 4) + " 0 " + (coord.chunkZPosition << 4)
                            + ". " + chunk.chunkTileEntityMap.size() + " TileEntities and "
                            + chunk.entityLists.length + " Entities");
                    out.newLine();
                }
                if (toLoad.size() == 0) {
                    out.write("# There are no chunks to load");
                    out.newLine();
                }
                out.flush();
                fos.flush();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fos != null) {
                        fos.close();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        @Override
        public void onChunkLoad(World world, Chunk chunk) {
            for (Object o : chunk.chunkTileEntityMap.values()) {
                if (o instanceof TileEntityWatchDemon) {
                    ((TileEntityWatchDemon) o).updateLoadInfo();
                }
            }
        }

        @Override
        public void onChunkUnload(World world, Chunk chunk) {
        }

        @Override
        public void onChunkSaveData(World world, Chunk chunk, NBTTagCompound data) {
        }

        @Override
        public void onChunkLoadData(World world, Chunk chunk, NBTTagCompound data) {
        }
    }

    void updateLoadInfo() {
        if (worldObj.isRemote) {
            return;
        }
        HashSet<ChunkCoordIntPair> toLoad = world2hash.get(worldObj);
        if (toLoad == null) {
            toLoad = new HashSet();
            world2hash.put(worldObj, toLoad);
        }
        int chunk_range = Core.watch_demon_chunk_range;
        for (int dx = -chunk_range; dx <= chunk_range; dx++) {
            for (int dz = -chunk_range; dz <= chunk_range; dz++) {
                int cx = (xCoord >> 4) + dx;
                int cz = (zCoord >> 4) + dz;
                ChunkCoordIntPair coord = new ChunkCoordIntPair(cx, cz);
                toLoad.add(coord);
            }
        }
    }

    static boolean chunkContainsDemon(Chunk chunk) {
        for (Object o : chunk.chunkTileEntityMap.values()) {
            if (o instanceof TileEntityWatchDemon) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (worldObj.getWorldTime() % 60 == 0) {
            updateLoadInfo();
        }
        lastXcoord = xCoord;
        lastZcoord = zCoord;
    }

    @Override
    public String toString() {
        return xCoord + " " + yCoord + " " + zCoord;
    }

    void onRemove() {
        if (worldObj.isRemote) {
            return;
        }
        HashSet<ChunkCoordIntPair> toLoad = world2hash.get(worldObj);
        if (toLoad == null) {
            //This wouldn't really make sense, would it?
            return;
        }
        ArrayList<TileEntityWatchDemon> demons = new ArrayList<TileEntityWatchDemon>();
        int chunk_range = Core.watch_demon_chunk_range;
        for (int dx = -chunk_range; dx <= chunk_range; dx++) {
            for (int dz = -chunk_range; dz <= chunk_range; dz++) {
                Chunk hereChunk = worldObj.getChunkFromBlockCoords(xCoord + dx * 16, zCoord + dz * 16);
                ChunkCoordIntPair hereCoord = new ChunkCoordIntPair((xCoord >> 4) + dx, (zCoord >> 4) + dz);
                toLoad.remove(hereCoord);
                for (Object o : hereChunk.chunkTileEntityMap.values()) {
                    if (!(o instanceof TileEntityWatchDemon)) {
                        continue;
                    }
                    TileEntityWatchDemon demon = (TileEntityWatchDemon) o;
                    if (demon == this) {
                        continue;
                    }
                    demons.add(demon);
                }
            }
        }
        for (TileEntityWatchDemon demon : demons) {
            demon.updateLoadInfo();
        }
        //		System.out.println("Demon " + this + " removed. Chunks keeping loaded: ");
        //		for (ChunkCoordIntPair c : toLoad) {
        //			System.out.println(c.chunkXPos + " " + c.chunkZPosition);
        //		}
    }

    static HashMap<World, Integer> corruption = new HashMap(16);

    static void corrupt(World world, int amount) {
        Integer g = corruption.get(world);
        if (g == null) {
            g = new Integer(0);
        }
        g += amount;
        corruption.put(world, g);
    }

    static int getCorruption(World world) {
        Integer g = corruption.get(world);
        if (g == null) {
            return 9999;
        }
        return g;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if ((xCoord >> 4) != (lastXcoord >> 4) || (zCoord >> 4) != (lastZcoord >> 4)) {
            int x = xCoord, z = zCoord;
            xCoord = lastXcoord;
            zCoord = lastZcoord;
            onRemove();
            xCoord = x;
            zCoord = z;
            updateLoadInfo();
            corrupt(worldObj, 128);
        }
    }

    public static void worldTick(World world) {
        if (world.isRemote) {
            return;
        }
        if (world.getWorldTime() % 2024 == 0) {
            corrupt(world, 1);
        }
        if (getCorruption(world) > 1024) {
            corrupt(world, -1024);
            world2hash.get(world).clear();
            for (Object o : world.loadedTileEntityList) {
                if (o instanceof TileEntityWatchDemon) {
                    ((TileEntityWatchDemon) o).updateLoadInfo();
                }
            }
        }
    }
}
