package factorization.shared;

import java.io.IOException;
import java.io.InputStream;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelFormatException;
import net.minecraftforge.client.model.obj.WavefrontObject;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class ObjectModel {
    private final ResourceLocation modelLocation;
    
    private int render_list = -1;
    
    public ObjectModel(ResourceLocation modelLocation) {
        this.modelLocation = modelLocation;
        Core.loadBus(this);
    }
    
    public void render(IIcon icon) {
        if (render_list == 0) {
            // Loading failed. So nothing can happen.
            return;
        }
        if (render_list == -1) {
            WavefrontObject model = readModel();
            if (model != null) {
                recordModel(model, icon);
            }
        }
        GL11.glCallList(render_list);
    }
    
    private WavefrontObject readModel() {
        WavefrontObject objectModel = null;
        try {
            InputStream input = null;
            try {
                input = Minecraft.getMinecraft().getResourceManager().getResource(modelLocation).getInputStream();
                if (input == null) {
                    Core.logWarning("Missing 3D model: " + modelLocation);
                    render_list = 0;
                    return null;
                }
                objectModel = new WavefrontObject(modelLocation.toString(), input);
                input.close();
                input = null;
            } finally {
                if (input != null) {
                    input.close();
                }
            }
        } catch (IOException e) {
            Core.logWarning("Failed to load model %s", modelLocation);
            e.printStackTrace();
            return null;
        } catch (ModelFormatException e) {
            Core.logWarning("Failed to load model %s", modelLocation);
            e.printStackTrace();
            return null;
        }
        return objectModel;
    }
    
    private void recordModel(WavefrontObject objectModel, final IIcon icon) {
        if (objectModel == null) return;
        Tessellator subsetTessellator = new Tessellator() {
            @Override
            public void setTextureUV(double u, double v) {
                super.setTextureUV(icon.getInterpolatedU(u*16), icon.getInterpolatedV(v*16));
            }
        };
        
        render_list = GLAllocation.generateDisplayLists(1);
        GL11.glNewList(render_list, GL11.GL_COMPILE);
        double modelScale = 1.0/16.0;
        GL11.glScaled(modelScale, modelScale, modelScale);
        subsetTessellator.startDrawingQuads();
        objectModel.tessellateAll(subsetTessellator);
        subsetTessellator.draw();
        modelScale = 1/modelScale;
        GL11.glScaled(modelScale, modelScale, modelScale);
        GL11.glEndList();
    }
    
    @SubscribeEvent
    public void resourcePackChanged(TextureStitchEvent.Post event) {
        if (render_list != -1 && render_list != 0) {
            GLAllocation.deleteDisplayLists(render_list);
        }
        render_list = -1;
    }
}
