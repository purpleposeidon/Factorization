package factorization.client.render;

import factorization.api.Coord;
import factorization.common.Core;
import factorization.common.FactoryType;
import factorization.common.TileEntityBattery;
import net.minecraft.client.Minecraft;
import net.minecraft.src.ModLoader;
import net.minecraft.src.RenderBlocks;
import net.minecraft.src.Tessellator;

public class BlockRenderBattery extends FactorizationBlockRender {

    @Override
    void render(RenderBlocks rb) {
        if (world_mode) {
            TileEntityBattery bat = new Coord(Minecraft.getMinecraft().theWorld, x, y, z).getTE(TileEntityBattery.class);
            if (bat == null) {
                return;
            }
            renderBatteryDisplay(rb, bat.getFullness());
        } else {
            renderNormalBlock(rb, FactoryType.BATTERY.md);
        }
    }

    
    void renderBatteryDisplay(RenderBlocks rb, float fullness) {
        fullness = Math.min(fullness, 1);
        int icon = (6) + 16 * 1;
        Tessellator tes = Tessellator.instance;
        double u = ((icon & 0xf) << 4) / 256.0;
        double v = (icon & 0xf0) / 256.0;

        double uw = u + 16F / 256F; // u + uvd;
        double vw = v + 16F / 256F; //v + uvd;

        float pixels = Math.round(fullness * 11);
        float h = 1F / 16F + pixels / 16F;
        v += (4 + 11 - pixels) / 256F;
        final double d = 1.0 / 128.0;
        int brightness = Core.registry.factory_block.getMixedBrightnessForBlock(ModLoader.getMinecraftInstance().theWorld, x, y, z);
        tes.setBrightness(brightness);
        float color = Math.min(1, fullness * .8F + 0.2F);
        tes.setColorOpaque_F(color, fullness, fullness);
        tes.addVertexWithUV(x, y, z - d, uw, vw);
        tes.addVertexWithUV(x, y + h, z - d, uw, v);
        tes.addVertexWithUV(x + 1, y + h, z - d, u, v);
        tes.addVertexWithUV(x + 1, y, z - d, u, vw);

        tes.addVertexWithUV(x + 1, y, z + 1 + d, uw, vw);
        tes.addVertexWithUV(x + 1, y + h, z + 1 + d, uw, v);
        tes.addVertexWithUV(x, y + h, z + 1 + d, u, v);
        tes.addVertexWithUV(x, y, z + 1 + d, u, vw);

        tes.addVertexWithUV(x - d, y, z + 1, u, vw);
        tes.addVertexWithUV(x - d, y + h, z + 1, u, v);
        tes.addVertexWithUV(x - d, y + h, z, uw, v);
        tes.addVertexWithUV(x - d, y, z, uw, vw);


        tes.addVertexWithUV(x + 1 + d, y, z, uw, vw);
        tes.addVertexWithUV(x + 1 + d, y + h, z, uw, v);
        tes.addVertexWithUV(x + 1 + d, y + h, z + 1, u, v);
        tes.addVertexWithUV(x + 1 + d, y, z + 1, u, vw);


        renderNormalBlock(rb, FactoryType.BATTERY.md);
    }


    @Override
    FactoryType getFactoryType() {
        return FactoryType.BATTERY;
    }
}
