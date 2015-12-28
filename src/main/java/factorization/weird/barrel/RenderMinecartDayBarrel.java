package factorization.weird.barrel;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderMinecart;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;

public class RenderMinecartDayBarrel extends RenderMinecart<EntityMinecartDayBarrel> {
    private static final TileEntityDayBarrelRenderer tesr = new TileEntityDayBarrelRenderer();

    static {
        tesr.setRendererDispatcher(TileEntityRendererDispatcher.instance);
    }

    public RenderMinecartDayBarrel(RenderManager renderManagerIn) {
        super(renderManagerIn);
    }

    protected void func_180560_a(EntityMinecartDayBarrel minecart, float partialTicks, IBlockState state) {
        super.func_180560_a(minecart, partialTicks, state);
        TileEntityRendererDispatcher.instance.renderTileEntity(minecart.barrel, partialTicks, 0);
    }
}
