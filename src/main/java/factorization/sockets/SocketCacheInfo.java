package factorization.sockets;

import com.google.common.base.Objects;
import factorization.util.RenderUtil;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.IModel;

class SocketCacheInfo {
    final IBakedModel[] parts;
    final EnumFacing facing;
    final boolean staticDraw;
    final boolean servoContext;

    SocketCacheInfo(TileEntitySocketBase socket, EnumFacing facing, boolean staticDraw, boolean servoContext) {
        this.facing = facing;
        parts = new IBakedModel[socket.parts.length + 1];
        ItemStack[] sp = socket.parts;
        parts[0] = SocketModel.base.model;
        for (int i = 0; i < sp.length; i++) {
            ItemStack is = sp[i];
            if (is == null) continue;
            if (staticDraw && !socket.isStaticPart(i, is)) continue;
            parts[i + 1] = RenderUtil.getBakedModel(is);
        }
        this.staticDraw = staticDraw;
        this.servoContext = servoContext;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SocketCacheInfo that = (SocketCacheInfo) o;
        return facing == that.facing && staticDraw == that.staticDraw && servoContext == that.servoContext && Objects.equal(parts, that.parts);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(parts, facing, staticDraw, servoContext);
    }

    public static SocketCacheInfo from(TileEntitySocketBase socket, EnumFacing facing, boolean staticDraw, boolean servoContext) {
        return new SocketCacheInfo(socket, facing, staticDraw, servoContext);
    }
}
