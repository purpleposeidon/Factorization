package factorization.weird.barrel;

import factorization.api.FzOrientation;
import factorization.common.FactoryType;
import factorization.shared.Core;
import factorization.util.DataUtil;
import factorization.util.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.property.IExtendedBlockState;

public class CacheInfo {
    final TextureAtlasSprite log, plank;
    final TileEntityDayBarrel.Type type;
    final FzOrientation orientation;
    final boolean isMetal;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CacheInfo cacheInfo = (CacheInfo) o;

        if (isMetal != cacheInfo.isMetal) return false;
        if (!log.equals(cacheInfo.log)) return false;
        if (!plank.equals(cacheInfo.plank)) return false;
        if (type != cacheInfo.type) return false;
        return orientation == cacheInfo.orientation;

    }

    @Override
    public int hashCode() {
        int result = log.hashCode();
        result = 31 * result + plank.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + orientation.hashCode();
        result = 31 * result + (isMetal ? 1 : 0);
        return result;
    }

    private CacheInfo(TextureAtlasSprite log, TextureAtlasSprite plank, TileEntityDayBarrel.Type type, FzOrientation orientation, boolean isMetal) {
        this.log = log;
        this.plank = plank;
        this.type = type;
        this.orientation = orientation;
        this.isMetal = isMetal;

    }

    public static CacheInfo from(TileEntityDayBarrel barrel) {
        TextureAtlasSprite log = RenderUtil.getSprite(barrel.woodLog);
        TextureAtlasSprite slab = RenderUtil.getSprite(barrel.woodSlab);
        FzOrientation fzo = barrel.orientation;
        TileEntityDayBarrel.Type type = barrel.type;
        return new CacheInfo(log, slab, type, fzo, isMetal(barrel.woodLog));
    }


    public static CacheInfo from(ItemStack is) {
        TileEntityDayBarrel barrel = (TileEntityDayBarrel) FactoryType.DAYBARREL.getRepresentative();
        assert barrel != null;
        barrel.loadFromStack(is);
        barrel.orientation = FzOrientation.FACE_WEST_POINT_UP;
        return CacheInfo.from(barrel);
    }

    static boolean isMetal(ItemStack it) {
        if (it == null) return true;
        Block block = DataUtil.getBlock(it);
        return block == null || !block.getMaterial().getCanBurn();
    }
}
