package factorization.mechanics;

import java.lang.ref.WeakReference;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import factorization.algos.FastBag;
import factorization.shared.Core;
import factorization.util.SpaceUtil;

public class ChainRender {
    public static final ChainRender instance = new ChainRender();

    private ChainRender() {
        Core.loadBus(this);
    }

    private FastBag<WeakReference<ChainLink>> chains = new FastBag<WeakReference<ChainLink>>();
    boolean needsRebag = true;

    public ChainLink add() {
        ChainLink ret = new ChainLink();
        ret.bagIndex = chains.size();
        chains.add(new WeakReference<ChainLink>(ret));
        rebag();
        return ret;
    }

    void release(ChainLink link) {
        final int index = link.bagIndex;
        chains.remove(index);
        if (index >= chains.size()) return;
        ChainLink newEntry = chains.get(index).get();
        if (newEntry == null) {
            rebag();
            return;
        }
        newEntry.bagIndex = index;
    }

    void rebag() {
        if (!needsRebag) return;
        needsRebag = false;
        for (int i = 0; i < chains.size(); i++) {
            if (chains.get(i).get() != null) continue;
            chains.remove(i);
            i--;
        }
        for (int i = 0; i < chains.size(); i++) {
            WeakReference<ChainLink> ref = chains.get(i);
            ChainLink chain = ref.get();
            if (chain == null) {
                needsRebag = true; // That'd be pretty obnoxious! But would only happen if a GC happened to trigger while this function is running.
                continue;
            }
            chain.bagIndex = i;
        }
    }

    boolean setup = false;
    Tessellator tessI = null;
    WorldRenderer tess = null;
    WorldClient world;


    void cleanup() {
        setup = false;
        tessI = null;
        tess = null;
        world = null;
    }


    @SubscribeEvent
    public void drawChains(RenderWorldLastEvent event) {
        if (chains.isEmpty()) return;
        final float partial = event.partialTicks;
        final ICamera camera = getFrustum(partial);

        final Minecraft mc = Minecraft.getMinecraft();
        final EntityRenderer er = mc.entityRenderer;
        final TextureManager textureManager = mc.getTextureManager();
        setup = false;
        world = mc.theWorld;

        for (WeakReference<ChainLink> ref : chains) {
            ChainLink chain = ref.get();
            if (chain == null) {
                needsRebag = true;
                continue;
            }
            chain.visitChain(camera, partial, this);
        }
        if (!setup) {
            cleanup();
            return;
        }

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT );
        textureManager.bindTexture(new ResourceLocation("factorization", "textures/chain.png"));
        er.enableLightmap();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);

        tessI.draw();
        er.disableLightmap();
        GL11.glPopAttrib();
        textureManager.bindTexture(Core.blockAtlas);
        cleanup();
    }

    @SubscribeEvent
    public void reset(WorldEvent.Unload unload) {
        if (!unload.world.isRemote) return;
        for (WeakReference<ChainLink> ref : chains) {
            ChainLink chain = ref.get();
            if (chain == null) continue;
            chain.bagIndex = -1;
        }
        chains.clear();
    }

    ICamera getFrustum(float partial) {
        // Unfortunately we have to make our own Frustum.
        final Minecraft mc = Minecraft.getMinecraft();
        final Entity eye = mc.getRenderViewEntity();
        double eyeX = eye.lastTickPosX + (eye.posX - eye.lastTickPosX) * (double)partial;
        double eyeY = eye.lastTickPosY + (eye.posY - eye.lastTickPosY) * (double)partial;
        double eyeZ = eye.lastTickPosZ + (eye.posZ - eye.lastTickPosZ) * (double)partial;

        Frustum frustum = new Frustum(); // Notch can't spell
        frustum.setPosition(eyeX, eyeY, eyeZ);
        return frustum;
    }


    public void drawChain(Vec3 s, Vec3 e, double partial) {
        if (!setup) {
            setup = true;
            tessI = Tessellator.getInstance();
            tess = tessI.getWorldRenderer();
            tess.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

            Entity eyePos = Minecraft.getMinecraft().getRenderViewEntity();
            double cx = eyePos.lastTickPosX + (eyePos.posX - eyePos.lastTickPosX) * partial;
            double cy = eyePos.lastTickPosY + (eyePos.posY - eyePos.lastTickPosY) * partial;
            double cz = eyePos.lastTickPosZ + (eyePos.posZ - eyePos.lastTickPosZ) * partial;
            tess.setTranslation(-cx, -cy, -cz);
        }
        draw(world, tess, s, e);
    }

    @SideOnly(Side.CLIENT)
    static void draw(WorldClient world, WorldRenderer tess, Vec3 s, Vec3 e) {
        Vec3 forward = s.subtract(e);
        double length = forward.lengthVector();
        Vec3 side1 = forward.crossProduct(new Vec3(1, 0, 1));
        if (SpaceUtil.isZero(side1)) {
            side1 = forward.crossProduct(new Vec3(-1, 0, -1));
        }
        side1 = side1.normalize();
        Vec3 side2 = forward.crossProduct(side1).normalize();
        final double d = 0.25;
        final double iconLength = 2 * d;
        side1 = SpaceUtil.scale(side1, d);
        side2 = SpaceUtil.scale(side2, d);

        double linkCount = length / 2 / iconLength;
        Vec3 normForward = forward.normalize();

        double extraLinkage = length % iconLength;
        extraLinkage *= -1;
        extraLinkage += 0.5;
        s = s.add(SpaceUtil.scale(normForward, extraLinkage));
        linkCount += extraLinkage;


        double g = 9F/32F;
        double h = g + 0.5;
        drawPlane(world, tess, s, e, side1, g - linkCount, 1 + g);
        drawPlane(world, tess, s, e, side2, h - linkCount, 1 + h);
    }

    static void setupLight(WorldClient world, WorldRenderer tess, Vec3 at) {
        int x = (int) at.xCoord;
        int y = (int) at.yCoord;
        int z = (int) at.zCoord;
        BlockPos pos = new BlockPos(x, y, z);
        Block b = world.getBlockState(pos).getBlock();
        int brightness = b.getMixedBrightnessForBlock(world, pos);
        int sky = brightness >> 16 & 65535;
        int block = brightness & 65535;
        tess.lightmap(sky, block);
    }

    static void drawPlane(WorldClient world, WorldRenderer tess, Vec3 workStart, Vec3 workEnd, Vec3 right, double uStart, double uEnd) {
        setupLight(world, tess, workStart);
        tess.tex(uStart, 1).putPosition(workStart.xCoord + right.xCoord,
                workStart.yCoord + right.yCoord,
                workStart.zCoord + right.zCoord);
        setupLight(world, tess, workStart);
        tess.tex(uStart, 0).putPosition(workStart.xCoord - right.xCoord,
                workStart.yCoord - right.yCoord,
                workStart.zCoord - right.zCoord);
        setupLight(world, tess, workEnd);
        tess.tex(uEnd, 0).putPosition(workEnd.xCoord - right.xCoord,
                workEnd.yCoord - right.yCoord,
                workEnd.zCoord - right.zCoord);
        setupLight(world, tess, workEnd);
        tess.tex(uEnd, 1).putPosition(workEnd.xCoord + right.xCoord,
                workEnd.yCoord + right.yCoord,
                workEnd.zCoord + right.zCoord);

    }


}
