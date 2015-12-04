package factorization.shared;

import java.io.IOException;
import java.io.InputStream;

import factorization.api.Quaternion;
import factorization.util.SpaceUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelFormatException;
import net.minecraftforge.client.model.obj.WavefrontObject;

import org.lwjgl.opengl.GL11;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

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

    private WavefrontObject isbrh_model = null;

    public boolean renderISBRH(RenderBlocks rb, IIcon icon, Block block, int x, int y, int z) {
        if (isbrh_model == null) {
            isbrh_model = readModel();
        }
        if (isbrh_model == null) {
            return false;
        }
        if (rb.overrideBlockTexture != null) {
            icon = rb.overrideBlockTexture;
        }
        Tessellator.instance.setColorOpaque(0xFF, 0xFF, 0xFF);
        int brightness = block.getMixedBrightnessForBlock(rb.blockAccess, x, y, z);
        Tessellator.instance.setBrightness(brightness);
        ModelTessellator tess = new ModelTessellator(icon);
        tess.setTranslation(x + 0.5, y, z + 0.5);
        isbrh_model.tessellateAll(tess);
        return true;
    }

    public boolean renderBrightISBRH(RenderBlocks rb, IIcon icon, Block block, int x, int y, int z) {
        if (isbrh_model == null) {
            isbrh_model = readModel();
        }
        if (isbrh_model == null) {
            return false;
        }
        if (rb.overrideBlockTexture != null) {
            icon = rb.overrideBlockTexture;
        }
        Tessellator.instance.setColorOpaque(0xFF, 0xFF, 0xFF);
        Tessellator.instance.setBrightness(0xF000F0);
        ModelTessellator tess = new ModelTessellator(icon);
        tess.setTranslation(x + 0.5, y, z + 0.5);
        isbrh_model.tessellateAll(tess);
        return true;
    }

    public boolean renderRotatedISBRH(RenderBlocks rb, IIcon icon, Block block, int x, int y, int z, Quaternion quat) {
        if (isbrh_model == null) {
            isbrh_model = readModel();
        }
        if (isbrh_model == null) {
            return false;
        }
        if (rb.overrideBlockTexture != null) {
            icon = rb.overrideBlockTexture;
        }
        Tessellator.instance.setColorOpaque(0xFF, 0xFF, 0xFF);
        int brightness = block.getMixedBrightnessForBlock(rb.blockAccess, x, y, z);
        Tessellator.instance.setBrightness(brightness);
        RotatedModelTessellator tess = new RotatedModelTessellator(icon, quat);
        tess.setTranslation(x + 0.5, y, z + 0.5);
        isbrh_model.tessellateAll(tess);
        return true;
    }

    public void render() {
        if (render_list == 0) {
            return;
        }
        if (render_list == -1) {
            WavefrontObject model = readModel();
            if (model != null) {
                recordModel(model);
            }
        }
        GL11.glCallList(render_list);
    }

    public static final double modelScale = 1.0 / 16.0;
    private static class ModelTessellator extends Tessellator {
        final IIcon icon;

        private ModelTessellator(IIcon icon) {
            this.icon = icon;
        }

        @Override
        public void setTextureUV(double u, double v) {
            Tessellator.instance.setTextureUV(icon.getInterpolatedU(u * 16), icon.getInterpolatedV(v * 16));
        }

        @Override
        public void addVertex(double x, double y, double z) {
            Tessellator.instance.addVertex(x * modelScale + xOffset, y * modelScale + yOffset, z * modelScale + zOffset);
        }

        @Override
        public void setNormal(float x, float y, float z) {
            Tessellator.instance.setNormal(x, y, z);
        }
    }

    private static class RotatedModelTessellator extends Tessellator {
        final IIcon icon;
        final Quaternion quat;
        final Vec3 vec = SpaceUtil.newVec();

        private RotatedModelTessellator(IIcon icon, Quaternion quat) {
            this.icon = icon;
            this.quat = quat;
        }

        @Override
        public void setTextureUV(double u, double v) {
            Tessellator.instance.setTextureUV(icon.getInterpolatedU(u * 16), icon.getInterpolatedV(v * 16));
        }

        @Override
        public void addVertex(double x, double y, double z) {
            vec.xCoord = x;
            vec.yCoord = y;
            vec.zCoord = z;
            quat.applyRotation(vec);
            Tessellator.instance.addVertex(vec.xCoord * modelScale + xOffset, vec.yCoord * modelScale + yOffset, vec.zCoord * modelScale + zOffset);
        }

        @Override
        public void setNormal(float x, float y, float z) {
            vec.xCoord = x;
            vec.yCoord = y;
            vec.zCoord = z;
            quat.applyRotation(vec);
            Tessellator.instance.setNormal((float)vec.xCoord, (float)vec.yCoord, (float)vec.zCoord);
        }
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
        GL11.glScaled(modelScale, modelScale, modelScale);
        subsetTessellator.startDrawingQuads();
        objectModel.tessellateAll(subsetTessellator);
        subsetTessellator.draw();
        double s = 1/modelScale;
        GL11.glScaled(s, s, s);
        GL11.glEndList();
    }

    private void recordModel(WavefrontObject objectModel) {
        render_list = GLAllocation.generateDisplayLists(1);
        GL11.glNewList(render_list, GL11.GL_COMPILE);
        double modelScale = 1.0/16.0;
        GL11.glScaled(modelScale, modelScale, modelScale);
        Tessellator.instance.startDrawingQuads();
        objectModel.tessellateAll(Tessellator.instance);
        Tessellator.instance.draw();
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
        isbrh_model = null;
    }
}
