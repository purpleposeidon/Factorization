package factorization.weird;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.TextureStitchEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import factorization.shared.Core;

public class NeptuneCape {
    {
        Core.loadBus(this);
    }
    
    static boolean hideMask(EntityPlayer player) {
        return player.getHideCape() || player.getCurrentArmor(3) != null;
    }
    
    static boolean should_render_mask = false;
    private static int total_hash = 0;
    private static final int[] mask_wearers = build_list("neptunepink");
    private static int[] build_list(String... names) {
        int[] ret = new int[names.length];
        int hash_or = 0;
        for (int i = 0; i < ret.length; i++) {
            hash_or |= ret[i] = names[i].hashCode();
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
        EntityPlayer player = event.entityPlayer;
        should_render_mask = !hideMask(player) && isNameWhitelisted(player.getCommandSenderName());
        if (should_render_mask) {
            LmpMaskRenderer.rendering_player = (AbstractClientPlayer) player;
        }
    }
    

    @SubscribeEvent
    public void resourcePackChanged(TextureStitchEvent.Post event) {
        RenderPlayer playerRenderer = (RenderPlayer) RenderManager.instance.entityRenderMap.get(EntityPlayer.class);
        ModelRenderer head = playerRenderer.modelBipedMain.bipedHead;
        if (head.childModels != null) {
            for (ModelRenderer child : (Iterable<ModelRenderer>) head.childModels) {
                if (child instanceof LmpMaskRenderer) return;
            }
        }
        head.addChild(new LmpMaskRenderer(playerRenderer.modelBipedMain));
    }
    
    static class LmpMaskRenderer extends ModelRenderer {
        ItemStack LMP = new ItemStack(Core.registry.logicMatrixProgrammer);
        EntityEnderman dummy_entity = new EntityEnderman(null);
        static AbstractClientPlayer rendering_player;
        
        public LmpMaskRenderer(ModelBase base) {
            super(base);
        }
        
        @Override
        public void render(float partial) {
            if (!should_render_mask) return;
            GL11.glPushMatrix();
            
            float s = 12F/16F; GL11.glScalef(s, s, s);
            
            GL11.glRotatef(-90, 0, 0, 1);
            
            GL11.glTranslatef(3.5F/16F, -7.5F/16F, -5.5F/16F);
            
            GL11.glTranslatef(0.9375F, 0.0625F, -0.0F);
            GL11.glRotatef(-335.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(-50.0F, 0.0F, 1.0F, 0.0F);
            
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
            GL11.glEnable(GL11.GL_CULL_FACE);
            RenderManager.instance.itemRenderer.renderItem(dummy_entity, LMP, 0);
            GL11.glPopAttrib();
            GL11.glPopMatrix();
            Minecraft.getMinecraft().renderEngine.bindTexture(rendering_player.getLocationSkin());
            rendering_player = null;
        }
        
    }
}
