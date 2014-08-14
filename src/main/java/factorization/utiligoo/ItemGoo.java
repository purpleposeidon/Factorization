package factorization.utiligoo;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.util.ForgeDirection;

import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.common.BlockIcons;
import factorization.common.ItemIcons;
import factorization.shared.BlockRenderHelper;
import factorization.shared.Core;
import factorization.shared.Core.TabType;
import factorization.shared.FzUtil;
import factorization.shared.ItemFactorization;
import factorization.shared.NetworkFactorization.MessageType;

public class ItemGoo extends ItemFactorization {
    public ItemGoo(String name, TabType tabType) {
        super(name, tabType);
        setMaxStackSize(32);
        Core.loadBus(this);
    }
    
    static final String fz_goo = "fz_goo";
    
    String getGooName(ItemStack is) {
        return fz_goo + "_" + is.getItemDamage();
    }
    
    GooData getGooData(ItemStack is, World world) {
        GooData data = (GooData) world.loadItemData(GooData.class, getGooName(is));
        if (data == null && !world.isRemote) {
            is.setItemDamage(world.getUniqueDataId(fz_goo));
            String name = getGooName(is);
            data = new GooData(name);
            data.markDirty();
            world.setItemData(name, data);
        }
        return data;
    }
    
    GooData getAndResetData(ItemStack is, World world) {
        GooData data = getGooData(is, world);
        if (!world.isRemote) {
            if (world.provider.dimensionId != data.dimensionId) {
                is.stackSize += data.coords.length / 3;
                data.coords = new int[0];
                data.dimensionId = world.provider.dimensionId;
                data.markDirty();
            }
        }
        return data;
    }
    
    @Override
    public boolean onItemUseFirst(ItemStack is, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (player.isSneaking()) return false;
        if (world.isRemote) {
            return false;
        }
        GooData data = getAndResetData(is, world); // stacksize may increase
        if (is.stackSize <= 1) return false;
        if (data == null) return false;
        for (int i = 0; i < data.coords.length; i += 3) {
            int ix = data.coords[i + 0];
            int iy = data.coords[i + 1];
            int iz = data.coords[i + 2];
            if (x == ix && y == iy && z == iz) {
                expandSelection(is, data, world, x, y, z, ForgeDirection.getOrientation(side));
                return true;
            }
        }
        data.coords = ArrayUtils.addAll(data.coords, x, y, z);
        data.markDirty();
        is.stackSize--;
        return true;
    }
    
    boolean check(int offset, int i, int x) {
        return offset == 0 || i == x;
    }
    
    void expandSelection(ItemStack is, GooData data, World world, int x, int y, int z, ForgeDirection dir) {
        HashSet<Coord> found = new HashSet();
        for (int i = 0; i < data.coords.length; i += 3) {
            int ix = data.coords[i + 0];
            int iy = data.coords[i + 1];
            int iz = data.coords[i + 2];
            if (check(dir.offsetX, ix, x) && check(dir.offsetY, iy, y) && check(dir.offsetZ, iz, z)) {
                Coord at = new Coord(world, ix, iy, iz);
                if (!at.add(dir).isSolid()) continue;
                Block atBlock = at.getBlock();
                ItemStack atDrop = at.getBrokenBlock();
                for (ForgeDirection fd : ForgeDirection.VALID_DIRECTIONS) {
                    if (fd == dir || fd == dir.getOpposite()) continue;
                    Coord n = at.add(fd);
                    Block nBlock = n.getBlock();
                    if (atBlock == nBlock && (atDrop == null || FzUtil.couldMerge(atDrop, n.getBrokenBlock()))) {
                        found.add(n);
                    }
                }
            }
        }
        ArrayList<Coord> addable = new ArrayList();
        nextCoord: for (Coord c : found) {
            for (int i = 0; i < data.coords.length; i += 3) {
                int ix = data.coords[i + 0];
                int iy = data.coords[i + 1];
                int iz = data.coords[i + 2];
                if (c.x == ix && c.y == iy && c.z == iz) continue nextCoord;
                addable.add(c);
            }
        }
        Collections.sort(addable);
        int count = Math.min(addable.size(), is.stackSize - 1);
        if (count <= 0) return;
        int[] use = new int[count * 3];
        for (int i = 0; i < count; i += 3) {
            Coord c = addable.get(i);
            use[i + 0] = c.x;
            use[i + 1] = c.y;
            use[i + 2] = c.z;
        }
        is.stackSize -= count;
        data.coords = ArrayUtils.addAll(data.coords, use);
    }
    
    @Override
    public boolean onEntitySwing(EntityLivingBase entityLiving, ItemStack is) {
        if (!(entityLiving instanceof EntityPlayer)) return false;
        EntityPlayer player = (EntityPlayer) entityLiving;
        MovingObjectPosition mop = getMovingObjectPositionFromPlayer(player.worldObj, player, false);
        if (mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return false;
        GooData data = getAndResetData(is, player.worldObj);
        if (data == null) return false;
        for (int i = 0; i < data.coords.length; i += 3) {
            int ix = data.coords[i + 0];
            int iy = data.coords[i + 1];
            int iz = data.coords[i + 2];
            if (mop.blockX == ix && mop.blockY == iy && mop.blockZ == iz) {
                if (player.isSneaking()) {
                    remove(is, data, ix, iy, iz);
                } else {
                    breakBlocks(is, data, player.worldObj, player, mop);
                }
                return false;
            }
        }
        return false;
    }
    
    void breakBlocks(ItemStack is, GooData data, World world, EntityPlayer player, MovingObjectPosition mop) {
        int removed = 0;
        Coord at = new Coord(world, 0, 0, 0);
        for (int i = 0; i < data.coords.length; i += 3) {
            at.x = data.coords[i + 0];
            at.y = data.coords[i + 1];
            at.z = data.coords[i + 2];
            Block block = at.getBlock();
            int md = at.getMd();
            block.onBlockHarvested(world, at.x, at.y, at.z, mop.sideHit, player);
            if (block.removedByPlayer(world, player, at.x, at.y, at.z, true)) {
                block.onBlockDestroyedByPlayer(world, at.x, at.y, at.z, md);
                block.harvestBlock(world, player, at.x, at.y, at.z, 0);
            }
            removed++;
        }
        if (world.rand.nextInt(100) < 80) {
            removed--;
            data.lost++;
            if (data.lost > maxStackSize * 0.9) {
                removed = is.stackSize;
            }
        }
        is.stackSize += removed;
        data.coords = new int[0];
        data.markDirty();
    }
    
    void remove(ItemStack is, GooData data, int x, int y, int z) {
        for (int i = 0; i < data.coords.length; i += 3) {
            int ix = data.coords[i + 0];
            int iy = data.coords[i + 1];
            int iz = data.coords[i + 2];
            if (ix == x && iy == y && iz == z) {
                data.coords = ArrayUtils.removeAll(data.coords, i, i + 1, i + 2);
                is.stackSize++;
                return;
            }
        }
    }
    
    @Override
    public void onUpdate(ItemStack is, World world, Entity player, int inventoryIndex, boolean isHeld) {
        if (world.isRemote) return;
        GooData data = getGooData(is, world);
        if (!data.isPlayerOutOfDate(player)) return;
        if (!(player instanceof EntityPlayer)) return;
        NBTTagCompound dataTag = new NBTTagCompound();
        data.writeToNBT(dataTag);
        FMLProxyPacket toSend = Core.network.entityPacket(player, MessageType.UtilityGooState, dataTag);
        Core.network.broadcastPacket((EntityPlayer) player, new Coord(player), toSend);
    }
    
    @SideOnly(Side.CLIENT)
    public static void handlePacket(DataInput input) throws IOException {
        NBTTagCompound dataTag = FzUtil.readTag(input);
        World world = Minecraft.getMinecraft().theWorld;
        GooData data = new GooData(dataTag.getString("mapname"));
        data.readFromNBT(dataTag);
        world.setItemData(data.mapName, data);
    }

    

    @Override
    public IIcon getIconIndex(ItemStack is) {
        int size = is.stackSize;
        if (size <= 1) return ItemIcons.utiligoo$empty;
        int third = is.getMaxStackSize() / 3;
        int fullness = size / third;
        if (fullness <= 1) return ItemIcons.utiligoo$low;
        if (fullness <= 2) return ItemIcons.utiligoo$medium;
        return ItemIcons.utiligoo$high;
    }
    
    @Override
    public IIcon getIcon(ItemStack is, int pass) {
        return getIconIndex(is);
    }
    
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void renderGoo(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null) return;
        boolean rendered_something = false;
        for (int i = 0; i < 9; i++) {
            ItemStack is = player.inventory.getStackInSlot(i);
            if (is == null || is.getItem() != this) continue;
            GooData data = getGooData(is, mc.theWorld);
            if (data == null) continue; 
            if (data.dimensionId != mc.theWorld.provider.dimensionId) continue;
            if (data.coords.length == 0) continue;
            if (!rendered_something) {
                rendered_something = true;
                EntityLivingBase camera = Minecraft.getMinecraft().renderViewEntity;
                double cx = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * event.partialTicks;
                double cy = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * event.partialTicks;
                double cz = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * event.partialTicks;
                mc.renderEngine.bindTexture(Core.blockAtlas);
                GL11.glPushMatrix();
                GL11.glTranslated(-cx, -cy, -cz);
                GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
                
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glColor4d(1, 1, 1, 0.9);
                OpenGlHelper.glBlendFunc(774, 768, 1, 0);
                GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
                GL11.glPolygonOffset(-3.0F, -3F);
            }
            renderGooFor(event, data, player);
        }
        if (rendered_something) {
            GL11.glPolygonOffset(0.0F, 0.0F);
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }
    
    @SideOnly(Side.CLIENT)
    void renderGooFor(RenderWorldLastEvent event, GooData data, EntityPlayer player) {
        boolean rendered_something = false;
        double render_dist_sq = 32*32;
        Tessellator tess = Tessellator.instance;
        BlockRenderHelper block = BlockRenderHelper.instance;
        RenderBlocks rb = FzUtil.getRB();
        for (int i = 0; i < data.coords.length; i += 3) {
            int x = data.coords[i + 0];
            int y = data.coords[i + 1];
            int z = data.coords[i + 2];
            if (player.getDistanceSq(x, y, z) > render_dist_sq) continue;
            if (!rendered_something) {
                tess.startDrawingQuads();
                tess.disableColor();
                rendered_something = true;
            }
            Block b = player.worldObj.getBlock(x, y, z);
            Material mat = b.getMaterial();
            if (mat.blocksMovement() && !b.hasTileEntity()) {
                rb.renderBlockUsingTexture(b, x, y, z, BlockIcons.utiligoo$invasion);
            } else {
                b.setBlockBoundsBasedOnState(player.worldObj, x, y, z);
                block.setBlockBounds((float)b.getBlockBoundsMinX(), (float)b.getBlockBoundsMinY(), (float)b.getBlockBoundsMinZ(), (float)b.getBlockBoundsMaxX(), (float)b.getBlockBoundsMaxY(), (float)b.getBlockBoundsMaxZ()); // Hello, Notch! 
                block.useTexture(BlockIcons.utiligoo$invasion);
                block.render(rb, x, y, z);
            }
        }
        if (rendered_something) {
            tess.draw();
        }
    }
    
    @Override
    protected void addExtraInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        super.addExtraInformation(is, player, list, verbose);
        if (verbose) {
            list.add("Goo Index: " + is.getItemDamage());
            GooData data = getGooData(is, player.worldObj);
            if (data == null) {
                list.add("Not loaded...");
            } else {
                list.add((data.coords.length / 3) + " points");
            }
        }
    }
}
