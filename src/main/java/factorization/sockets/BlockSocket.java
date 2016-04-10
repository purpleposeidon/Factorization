package factorization.sockets;

import factorization.common.FactoryType;
import factorization.idiocy.DumbExtendedProperty;
import factorization.shared.SimpleFzBlockCutout;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

public class BlockSocket extends SimpleFzBlockCutout {
    public static final IUnlistedProperty<SocketCacheInfo> SOCKET_INFO = new DumbExtendedProperty<SocketCacheInfo>("info", SocketCacheInfo.class);

    public BlockSocket() {
        super(Material.iron, FactoryType.SOCKET_EMPTY);
    }

    @Override
    public boolean canRenderInLayer(EnumWorldBlockLayer layer) {
        return layer == EnumWorldBlockLayer.CUTOUT;
    }

    @Override
    protected BlockState createBlockState() {
        return new ExtendedBlockState(this, new IProperty[] {}, new IUnlistedProperty[] {SOCKET_INFO});
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntitySocketBase socket = (TileEntitySocketBase) world.getTileEntity(pos);
        IExtendedBlockState extendedBS = (IExtendedBlockState) super.getExtendedState(state, world, pos);
        if (socket == null) {
            socket = (TileEntitySocketBase) FactoryType.SOCKET_EMPTY.getRepresentative();
            assert socket != null;
        }
        EnumFacing facing = socket.facing;
        return extendedBS.withProperty(SOCKET_INFO, SocketCacheInfo.from(socket, facing, true, false));
    }
}
