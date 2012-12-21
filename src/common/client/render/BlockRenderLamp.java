package factorization.client.render;

import factorization.common.BlockFactorization;
import factorization.common.Core;
import factorization.common.FactoryType;
import factorization.common.Texture;
import net.minecraft.client.renderer.RenderBlocks;

public class BlockRenderLamp extends FactorizationBlockRender {

    @Override
    void render(RenderBlocks rb) {
        float s = 1F / 16F;
        float p = 1F / 64F;
        float trim_out = BlockFactorization.lamp_pad;
        float trim_in = trim_out + s * 2;
        float glass_mid = (trim_in + trim_out) / 2;
        float glass_ver = trim_in; //trim_in + 1F / 128F;
        float panel = trim_out + s; //trim_in + s * 0;
        BlockFactorization block = Core.registry.factory_block;
        int metal = Texture.lamp_iron;
        int glass = Texture.lamp_iron + 2;
        //glass
        renderPart(rb, glass, glass_mid, glass_ver, glass_mid, 1 - glass_mid, 1 - glass_ver, 1 - glass_mid);
        //corners
        renderPart(rb, metal, trim_in, trim_in, trim_in, trim_out, 1 - trim_in, trim_out); //lower left
        renderPart(rb, metal, 1 - trim_out, trim_in, 1 - trim_out, 1 - trim_in, 1 - trim_in, 1 - trim_in); //upper right
        renderPart(rb, metal, trim_in, 1 - trim_in, 1 - trim_in, trim_out, trim_in, 1 - trim_out); //upper left
        renderPart(rb, metal, 1 - trim_in, 1 - trim_in, trim_in, 1 - trim_out, trim_in, trim_out); //lower right
        //covers
        renderPart(rb, metal, trim_out, 1 - trim_in, trim_out, 1 - trim_out, 1 - trim_out, 1 - trim_out); //top
        renderPart(rb, metal, 1 - trim_out, trim_out, 1 - trim_out, trim_out, trim_in, trim_out); //bottom
        //knob
        renderPart(rb, metal, panel, 1 - trim_out, panel, 1 - panel, 1 - trim_out + s * 1, 1 - panel);
        renderPart(rb, metal, panel, trim_out - s * 1, panel, 1 - panel, trim_out, 1 - panel);

        //TODO: Handle. From the top, a side, or the ground.
    }
    
    @Override
    FactoryType getFactoryType() {
        return FactoryType.LAMP;
    }

}
