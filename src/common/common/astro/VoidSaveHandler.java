package factorization.common.astro;

import java.io.File;

import net.minecraft.src.IChunkLoader;
import net.minecraft.src.IPlayerFileData;
import net.minecraft.src.ISaveHandler;
import net.minecraft.src.MinecraftException;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.WorldInfo;
import net.minecraft.src.WorldProvider;

class VoidSaveHandler implements ISaveHandler {

    @Override
    public WorldInfo loadWorldInfo() {
        return null;
    }

    @Override
    public void checkSessionLock() throws MinecraftException {}

    @Override
    public IChunkLoader getChunkLoader(WorldProvider var1) {
        return null;
    }

    @Override
    public void saveWorldInfoWithPlayer(WorldInfo var1, NBTTagCompound var2) {}

    @Override
    public void saveWorldInfo(WorldInfo var1) {}

    @Override
    public IPlayerFileData getSaveHandler() {
        return null;
    }

    @Override
    public void flush() {}

    @Override
    public File getMapFileFromName(String var1) {
        return null;
    }

    @Override
    public String getSaveDirectoryName() {
        return "none";
    }
}