package factorization.weird;

import factorization.shared.Core;
import factorization.shared.FzModel;
import factorization.shared.PatreonRewards;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class NeptuneCape {
    {
        Core.loadBus(this);
    }
    
    static boolean hideMask(EntityPlayer player) {
        final ItemStack helmet = player.getCurrentArmor(3);
        return helmet == null || helmet.getItem() != Core.registry.logicMatrixProgrammer;
    }
    
    static boolean should_render_mask = false;
    private static int total_hash = 0;
    private static final int[] mask_wearers = build_list(PatreonRewards.getMasqueraders());
    private static int[] build_list(List<String> names) {
        int[] ret = new int[names.size()];
        int hash_or = 0;
        for (int i = 0; i < ret.length; i++) {
            hash_or |= ret[i] = names.get(i).hashCode();
        }
        total_hash = ~hash_or;
        return ret;
    }

    private static boolean isNameWhitelisted(String name) {
        int name_hash = name.hashCode();
        if ((name_hash & total_hash) != 0) return false;
        for (int hash : mask_wearers) {
            if (hash == name_hash) return true;
        }
        return false;
    }
    
    @SubscribeEvent
    public void renderLmp(RenderPlayerEvent.Pre event) {
        if (invalid) return;
        EntityPlayer player = event.entityPlayer;
        if (dirty) {
            dirty = false;
            injectModel(player);
        }
        should_render_mask = !hideMask(player) && isNameWhitelisted(player.getName());
        if (should_render_mask) {
            LmpMaskRenderer.rendering_player = (AbstractClientPlayer) player;
        }
    }

    boolean invalid = false;
    void injectModel(EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        Render<EntityPlayer> pr = mc.getRenderManager().getEntityRenderObject(player);
        if (!((Render) pr instanceof RenderPlayer)) {
            if (!invalid) {
                Core.logWarning("Unable to patch Player model!");
            }
            invalid = true;
            return;
        }
        RenderPlayer playerRenderer = (RenderPlayer) (Render) pr;
        ModelRenderer head = playerRenderer.getMainModel().bipedHead;
        if (head.childModels != null) {
            for (ModelRenderer child : head.childModels) {
                if (child instanceof LmpMaskRenderer) return;
            }
        }
        head.addChild(new LmpMaskRenderer(playerRenderer.getMainModel()));
    }
    
    boolean dirty = true;
    @SubscribeEvent
    public void resourcePackChanged(TextureStitchEvent.Post event) {
        dirty = true;
    }
    
    static class LmpMaskRenderer extends ModelRenderer {
        static FzModel LMP = new FzModel("lmpMask");
        static AbstractClientPlayer rendering_player;

        public LmpMaskRenderer(ModelBase base) {
            super(base);
            base.boxList.remove(this); // Prevents arrows from rendering inside us; else rendering will crash
        }
        
        @Override
        public void render(float partial) {
            if (!should_render_mask) return;
            if (rendering_player == null) return; // Model may be rendered twice for the hurt animation
            GL11.glPushMatrix();
            
            float s = 12F/16F; GL11.glScalef(s, s, s);
            
            GL11.glRotatef(-90, 0, 0, 1);
            
            GL11.glTranslatef(3.5F/16F, -7.5F/16F, -5.5F/16F);
            
            GL11.glTranslatef(0.9375F, 0.0625F, -0.0F);
            GL11.glRotatef(-335.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(-50.0F, 0.0F, 1.0F, 0.0F);
            
            
            LMP.draw();
            GL11.glPopMatrix();
            Minecraft.getMinecraft().renderEngine.bindTexture(rendering_player.getLocationSkin());
            rendering_player = null;
        }
        
    }
}
