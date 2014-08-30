package factorization.utiligoo;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.shader.ShaderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;

import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.common.BlockIcons;
import factorization.common.Command;
import factorization.common.ItemIcons;
import factorization.coremodhooks.HandleAttackKeyEvent;
import factorization.coremodhooks.HandleUseKeyEvent;
import factorization.shared.BlockRenderHelper;
import factorization.shared.Core;
import factorization.shared.Core.TabType;
import factorization.shared.DropCaptureHandler;
import factorization.shared.FzUtil;
import factorization.shared.FzUtil.FzInv;
import factorization.shared.ICaptureDrops;
import factorization.shared.ItemFactorization;
import factorization.shared.NetworkFactorization.MessageType;

public class ItemGoo extends ItemFactorization {
    public ItemGoo(String name, TabType tabType) {
        super(name, tabType);
        setMaxStackSize(32);
        setHasSubtypes(true);
        Core.loadBus(this);
    }
    
    @Override
    public void registerIcons(IIconRegister reg) { }
    
    @Override
    public boolean onItemUseFirst(ItemStack is, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return false;
        }
        GooData data = GooData.getGooData(is, world);
        if (data.checkWorld(player, new Coord(world, x, y, z))) return false;
        if (player.isSneaking()) {
            ForgeDirection fd = ForgeDirection.getOrientation(side);
            for (int i = 0; i < data.coords.length; i += 3) {
                data.coords[i + 0] = data.coords[i + 0] + fd.offsetX;
                data.coords[i + 1] = data.coords[i + 1] + fd.offsetY;
                data.coords[i + 2] = data.coords[i + 2] + fd.offsetZ;
            }
            data.markDirty();
            return false;
        }
        if (is.stackSize <= 1) return false;
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
        data.dimensionId = world.provider.dimensionId;
        data.markDirty();
        is.stackSize--;
        return true;
    }
    
    public void executeCommand(Command command, EntityPlayerMP player) {
        MovingObjectPosition mop = getMovingObjectPositionFromPlayer(player.worldObj, player, false);
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        
        ItemStack held = player.getHeldItem();
        
        for (int slot = 0; slot < 9; slot++) {
            ItemStack is = player.inventory.getStackInSlot(slot);
            if (is == null || is.getItem() != this) continue;
            GooData data = GooData.getGooData(is, player.worldObj);
            if (data.checkWorld(player, null)) continue;
            for (int i = 0; i < data.coords.length; i += 3) {
                int ix = data.coords[i + 0];
                int iy = data.coords[i + 1];
                int iz = data.coords[i + 2];
                if (ix == mop.blockX && iy == mop.blockY && iz == mop.blockZ) {
                    final FzInv playerInv = FzUtil.openInventory(player, true);
                    DropCaptureHandler.startCapture(new ICaptureDrops() {
                        @Override
                        public boolean captureDrops(int x, int y, int z, ArrayList<ItemStack> stacks) {
                            boolean any = false;
                            for (ItemStack is : stacks) {
                                if (FzUtil.normalize(is) == null) continue;
                                is.stackSize = FzUtil.getStackSize(playerInv.push(is.copy()));
                                any = true;
                            }
                            return any;
                        }
                    });
                    try {
                        if (command == Command.gooLeftClick) {
                            leftClick(player, data, is, held, mop);
                        } else if (command == Command.gooRightClick) {
                            rightClick(player, data, is, held, mop);
                        }
                    } finally {
                        DropCaptureHandler.endCapture();
                    }
                    return;
                }
            }
        }
    }
    
    private void leftClick(EntityPlayerMP player, GooData data, ItemStack gooItem, ItemStack held, MovingObjectPosition mop) {
        // Punch with tool: remove all blocks that can be harvested by the tool
        // Normal punch: degoo 3x3x3 goo area
        // shift-punch: degoo punched block
        if (held != null && (held.getItem() instanceof ItemTool || !held.getItem().getToolClasses(held).isEmpty())) {
            // mineSelection(gooItem, data, player.worldObj, mop, player, held);
        } else {
            int radius = player.isSneaking() ? 0 : 1;
            degooArea(player, data, gooItem, mop, radius);
        }
    }
    
    private void rightClick(EntityPlayer player, GooData data, ItemStack gooItem, ItemStack held, MovingObjectPosition mop) {
        // goo click: expand the selection.
        // ItemBlock click: replace everything with held item
        if (held == null) {
            Coord at = new Coord(player.worldObj, mop);
            Block b = at.getBlock();
            int md = at.getMd();
            if (b.hasTileEntity(md)) return;
            if (b instanceof BlockStairs) {
                if (player.isSneaking()) {
                    md ^= 0x8;
                } else {
                    md = (md + 1) % 0xF;
                }
                at.setMd(md);
            } else if (b instanceof BlockSlab) {
                at.setMd(md ^ 0x8);
            }
        } else if (held.getItem() == this) {
            expandSelection(gooItem, data, player.worldObj, mop.blockX, mop.blockY, mop.blockZ, ForgeDirection.getOrientation(mop.sideHit));
        } else if (held.getItem() instanceof ItemBlock) {
            replaceBlocks(gooItem, data, player.worldObj, player, mop, held);
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack is = player.inventory.getStackInSlot(i);
                if (is != null && FzUtil.normalize(is) == null) {
                    player.inventory.setInventorySlotContents(i, null);
                }
            }
        }
    }
    
    private boolean check(int offset, int i, int x) {
        return offset == 0 || i == x;
    }
    
    private void expandSelection(ItemStack is, GooData data, World world, int x, int y, int z, ForgeDirection dir) {
        HashSet<Coord> found = new HashSet();
        for (int i = 0; i < data.coords.length; i += 3) {
            int ix = data.coords[i + 0];
            int iy = data.coords[i + 1];
            int iz = data.coords[i + 2];
            if (check(dir.offsetX, ix, x) && check(dir.offsetY, iy, y) && check(dir.offsetZ, iz, z)) {
                Coord at = new Coord(world, ix, iy, iz);
                Coord adj = at.add(dir);
                if (adj.isSolid() || adj.isSolidOnSide(dir.getOpposite())) continue;
                Block atBlock = at.getBlock();
                ItemStack atDrop = at.getBrokenBlock();
                for (ForgeDirection fd : ForgeDirection.VALID_DIRECTIONS) {
                    if (fd == dir || fd == dir.getOpposite()) continue;
                    Coord n = at.add(fd);
                    Block nBlock = n.getBlock();
                    if (atBlock == nBlock && (atDrop == null || FzUtil.couldMerge(atDrop, n.getBrokenBlock())) && !n.add(dir).isSolid()) {
                        Coord nadj = n.add(dir);
                        if (nadj.isSolidOnSide(dir.getOpposite())) continue;
                        if (nadj.getBlock() instanceof BlockSlab && (nadj.getMd() & 8) == 0) {
                            continue;
                        }
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
            }
            addable.add(c);
        }
        Collections.sort(addable);
        int count = Math.min(addable.size(), is.stackSize - 1);
        count = Math.min(maxStackSize - 1 - data.coords.length / 3, count);
        if (count <= 0) return;
        int[] use = new int[count * 3];
        for (int i = 0; i < count; i++) {
            Coord c = addable.get(i);
            use[i * 3 + 0] = c.x;
            use[i * 3 + 1] = c.y;
            use[i * 3 + 2] = c.z;
        }
        is.stackSize -= count;
        data.coords = ArrayUtils.addAll(data.coords, use);
        data.markDirty();
    }
    
    private boolean degooArea(EntityPlayer player, GooData data, ItemStack gooItem, MovingObjectPosition mop, int radius) {
        if (player.worldObj.isRemote) return false;
        if (data == null) return false;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return false;
        for (int i = 0; i < data.coords.length; i += 3) {
            int ix = data.coords[i + 0];
            int iy = data.coords[i + 1];
            int iz = data.coords[i + 2];
            if (mop.blockX == ix && mop.blockY == iy && mop.blockZ == iz) {
                int d = radius;
                boolean any = false;
                for (int dx = -d; dx <= d; dx++) {
                    for (int dy = -d; dy <= d; dy++) {
                        for (int dz = -d; dz <= d; dz++) {
                            any |= deselectCoord(gooItem, data, player.worldObj, ix + dx, iy + dy, iz + dz, false);
                        }
                    }
                }
                if (any) {
                    data.markDirty();
                }
                return false;
            }
        }
        return false;
    }
    
    private void replaceBlocks(ItemStack is, GooData data, World world, EntityPlayer player, MovingObjectPosition mop, ItemStack source) {
        if (FzUtil.normalize(source) == null) return;
        int removed = 0;
        Coord at = new Coord(world, 0, 0, 0);
        boolean creative = player.capabilities.isCreativeMode;
        ArrayList<Integer> to_remove = new ArrayList();
        for (int i = 0; i < data.coords.length; i += 3) {
            if (source.stackSize <= 0) {
                for (ItemStack replace : player.inventory.mainInventory) {
                    if (source != replace && FzUtil.identical(source, replace)) {
                        if (FzUtil.normalize(replace) != null) {
                            source = replace;
                            break;
                        }
                    }
                }
                if (FzUtil.normalize(source) == null) break;
            }
            at.x = data.coords[i + 0];
            at.y = data.coords[i + 1];
            at.z = data.coords[i + 2];
            if (creative) {
                at.setAir();
            } else {
                Block block = at.getBlock();
                int md = at.getMd();
                EntityPlayerMP emp = (EntityPlayerMP) player;
                block.onBlockHarvested(world, at.x, at.y, at.z, md, emp);
                boolean destroyed = block.removedByPlayer(world, emp, at.x, at.y, at.z, true);
                if (destroyed) {
                    block.onBlockDestroyedByPlayer(world, at.x, at.y, at.z, md);
                }
                block.harvestBlock(world, emp, at.x, at.y, at.z, md);
            }
            to_remove.add(i + 0);
            to_remove.add(i + 1);
            to_remove.add(i + 2);
            
            ItemBlock ib = (ItemBlock) source.getItem();
            int origSize = source.stackSize;
            ib.onItemUse(source, player, player.worldObj, at.x, at.y, at.z, mop.sideHit, (float) mop.hitVec.xCoord, (float) mop.hitVec.yCoord, (float) mop.hitVec.zCoord);
            if (player.capabilities.isCreativeMode) {
                source.stackSize = origSize; // Great work, guys.
            }
            removed++;
        }
        if (removed <= 0) return;
        data.removeIndices(to_remove, is, world);
        if (!player.capabilities.isCreativeMode) misplaceSomeGoo(is, world.rand, removed);
    }
    
    private boolean deselectCoord(ItemStack is, GooData data, World world, int x, int y, int z, boolean bulkAction) {
        for (int i = 0; i < data.coords.length; i += 3) {
            int ix = data.coords[i + 0];
            int iy = data.coords[i + 1];
            int iz = data.coords[i + 2];
            if (ix == x && iy == y && iz == z) {
                data.coords = ArrayUtils.removeAll(data.coords, i, i + 1, i + 2);
                is.stackSize++;
                if (data.coords.length == 0) {
                    data.wipe(is, world);
                } else if (!bulkAction) {
                    data.markDirty();
                }
                return true;
            }
        }
        return false;
    }
    
    private void mineSelection(ItemStack is, GooData data, World world, MovingObjectPosition mop, EntityPlayerMP player, ItemStack tool) {
        Item toolItem = tool.getItem();
        ArrayList<Integer> toRemove = new ArrayList();
        int removed = 0;
        for (int i = 0; i < data.coords.length; i += 3) {
            int ix = data.coords[i + 0];
            int iy = data.coords[i + 1];
            int iz = data.coords[i + 2];
            Block b = world.getBlock(ix, iy, iz);
            if (b.isAir(world, ix, iy, iz)) continue;
            float hardness = b.getBlockHardness(world, ix, iy, iz);
            if (hardness < 0) continue;
            if (hardness == 0 || toolItem.canHarvestBlock(b, tool) || toolItem.func_150893_a(tool, b) > 1)  {
                if (player.theItemInWorldManager.tryHarvestBlock(ix, iy, iz)) {
                    removed++;
                    toRemove.add(i);
                    toRemove.add(i + 1);
                    toRemove.add(i + 2);
                }
            }
            if (FzUtil.normalize(tool) == null) break;
        }
        if (removed == 0) return;
        data.removeIndices(toRemove, is, world);
        if (!player.capabilities.isCreativeMode) misplaceSomeGoo(is, world.rand, removed);
    }
    
    private void misplaceSomeGoo(ItemStack is, Random rand, int removed) {
        if (rand.nextInt(100) < 20) {
            removed--;
        }
        is.stackSize += removed;
    }
    
    @Override
    public void onUpdate(ItemStack is, World world, Entity player, int inventoryIndex, boolean isHeld) {
        if (world.isRemote) return;
        GooData data = GooData.getNullGooData(is, world);
        if (data == null) return;
        if (!(player instanceof EntityPlayer)) return;
        if (!data.isPlayerOutOfDate(player)) return;
        NBTTagCompound dataTag = new NBTTagCompound();
        data.writeToNBT(dataTag);
        FMLProxyPacket toSend = Core.network.entityPacket(player, MessageType.UtilityGooState, dataTag);
        Core.network.broadcastPacket((EntityPlayer) player, new Coord(player), toSend);
    }
    
    @SideOnly(Side.CLIENT)
    public static void handlePacket(DataInput input) throws IOException {
        NBTTagCompound dataTag = FzUtil.readTag(input);
        World world = Minecraft.getMinecraft().theWorld;
        GooData data = new GooData(dataTag.getString("mapname")); // NOTE: this resets data.last_traced_index to -1. We might have to reset it manually if networking gets more complicated.
        data.readFromNBT(dataTag);
        world.setItemData(data.mapName, data);
    }
    

    @Override
    public IIcon getIconIndex(ItemStack is) {
        int size = is.stackSize;
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
    
    Minecraft mc = Minecraft.getMinecraft();
    ShaderManager sobel = null;
    boolean loaded = false;
    
    javax.vecmath.Matrix4f projectionMatrix;
    
    void resetProjectionMatrix() {
        projectionMatrix = new javax.vecmath.Matrix4f();
        projectionMatrix.setIdentity();
        projectionMatrix.m00 = 2.0F / (float)mc.displayWidth;
        projectionMatrix.m11 = 2.0F / (float)(-mc.displayHeight);
        projectionMatrix.m22 = -0.0020001999F;
        projectionMatrix.m33 = 1.0F;
        projectionMatrix.m03 = -1.0F;
        projectionMatrix.m13 = 1.0F;
        projectionMatrix.m23 = -1.0001999F;
    }

    
    private boolean useShaders() {
        return false;
/*        if (loaded) return sobel != null;
        loaded = true;
        try {
            sobel = new ShaderManager(mc.getResourceManager(), "invert");
            sobel.func_147992_a("DiffuseSampler", mc.getFramebuffer());
        } catch (IOException e) {
            e.printStackTrace();
            sobel = null;
            return false;
        }
        return true;*/
    }
    
    
    private void beginGlWithShaders() {
        resetProjectionMatrix();
        int width = mc.getFramebuffer().framebufferWidth;
        int height = mc.getFramebuffer().framebufferHeight;
        //sobel.func_147984_b("ProjMat").setProjectionMatrix(projectionMatrix);
        //sobel.func_147984_b("InSize").func_148087_a((float)width, (float)height);
        //sobel.func_147984_b("OutSize").func_148087_a(width, height);
        //sobel.func_147984_b("Time").func_148090_a(0);
        sobel.func_147995_c();
    }
    
    private void endGlWithShaders() {
        sobel.func_147993_b();
    }
    
    private void beginGlNoShaders() {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glColor4d(1, 1, 1, 0.9);
        OpenGlHelper.glBlendFunc(774, 768, 1, 0);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-3.0F, -3F);
    }
    
    private void endGlNoShaders() {
        GL11.glPolygonOffset(0.0F, 0.0F);
        GL11.glPopAttrib();
    }
    
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void renderGoo(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null) return;
        boolean rendered_something = false;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack is = player.inventory.getStackInSlot(slot);
            if (is == null || is.getItem() != this) continue;
            GooData data = GooData.getNullGooData(is, mc.theWorld);
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
                GL11.glTranslated(-cx, -cy + 1, -cz);
                if (useShaders()) {
                    beginGlWithShaders();
                } else {
                    beginGlNoShaders();
                }
            }
            renderGooFor(event, data, player);
        }
        if (rendered_something) {
            GL11.glPopMatrix();
            if (useShaders()) {
                endGlWithShaders();
            } else {
                endGlNoShaders();
            }
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
            int md = player.worldObj.getBlockMetadata(x, y, z);
            if (useShaders()) {
                if (mat.blocksMovement() && !b.hasTileEntity(md)) {
                    rb.renderBlockByRenderType(b, x, y, z);
                } else {
                    b.setBlockBoundsBasedOnState(player.worldObj, x, y, z);
                    block.setBlockBounds((float)b.getBlockBoundsMinX(), (float)b.getBlockBoundsMinY(), (float)b.getBlockBoundsMinZ(), (float)b.getBlockBoundsMaxX(), (float)b.getBlockBoundsMaxY(), (float)b.getBlockBoundsMaxZ()); // Hello, Notch! 
                    block.useTexture(BlockIcons.utiligoo$invasion);
                    block.render(rb, x, y, z);
                }
            } else {
                if (mat.blocksMovement() && !b.hasTileEntity(md)) {
                    rb.renderBlockUsingTexture(b, x, y, z, BlockIcons.utiligoo$invasion);
                } else if (b.getRenderType() == 2 /* torches */) {
                    // Torches stupidly don't support setBlockBoundsBasedOnState. #blamenotch
                    float d = 0.25F;
                    block.setBlockBoundsOffset(d, d, d);
                    block.useTexture(BlockIcons.utiligoo$invasion);
                    block.render(rb, x, y, z);
                } else {
                    b.setBlockBoundsBasedOnState(player.worldObj, x, y, z);
                    block.setBlockBounds((float)b.getBlockBoundsMinX(), (float)b.getBlockBoundsMinY(), (float)b.getBlockBoundsMinZ(), (float)b.getBlockBoundsMaxX(), (float)b.getBlockBoundsMaxY(), (float)b.getBlockBoundsMaxZ()); // Hello, Notch! 
                    block.useTexture(BlockIcons.utiligoo$invasion);
                    block.render(rb, x, y, z);
                }
            }
        }
        if (rendered_something) {
            tess.draw();
        }
    }
    
    @Override
    protected void addExtraInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        super.addExtraInformation(is, player, list, verbose);
        GooData data = GooData.getNullGooData(is, player.worldObj);
        if (data != null) {
            list.add(I18n.format("item.factorization:utiligoo.placed", data.coords.length / 3));
            if (player != null && player.worldObj != null) {
                if (data.dimensionId != player.worldObj.provider.dimensionId) {
                    list.add(I18n.format("item.factorization:utiligoo.wrongDimension"));
                }
            }
            if (Core.dev_environ) {
                list.add("#" + is.getItemDamage());
            }
        }
    }
    
    boolean gooHilighted(EntityPlayer player, MovingObjectPosition mop) {
        if (mop == null || mop.typeOfHit != MovingObjectType.BLOCK) return false;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack is = player.inventory.getStackInSlot(slot);
            if (is == null || is.getItem() != this) continue;
            GooData data = GooData.getNullGooData(is, player.worldObj);
            if (data == null) continue;
            for (int i = 0; i < data.coords.length; i += 3) {
                int x = data.coords[i + 0];
                int y = data.coords[i + 1];
                int z = data.coords[i + 2];
                if (x == mop.blockX && y == mop.blockY && z == mop.blockZ) {
                    return true;
                }
            }
        }
        return false;
    }
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void interceptGooClick(HandleUseKeyEvent event) {
        // Only used for replacing blocks.
        Minecraft mc = Minecraft.getMinecraft();
        MovingObjectPosition mop = mc.objectMouseOver;
        EntityPlayer player = mc.thePlayer;
        if (gooHilighted(player, mop)) {
            Command.gooRightClick.call(player);
            event.setCanceled(true);
            mc.rightClickDelayTimer = 4;
        }
    }
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void interceptGooBreak(HandleAttackKeyEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        MovingObjectPosition mop = mc.objectMouseOver;
        EntityPlayer player = mc.thePlayer;
        if (gooHilighted(player, mop)) {
            Command.gooLeftClick.call(player);
            event.setCanceled(true);
        }
    }
    
    
    ThreadLocal<Boolean> processing = new ThreadLocal<Boolean>();
    
    @SubscribeEvent
    public void mineGooeyBlocks(BreakEvent event) {
        if (processing.get() != null) return;
        EntityPlayer p = event.getPlayer();
        if (!(p instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) p;
        ItemStack held = player.getHeldItem();
        if (held == null) return;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack is = FzUtil.normalize(player.inventory.getStackInSlot(slot));
            if (is == null || is.getItem() != this) continue;
            GooData data = GooData.getNullGooData(is, player.worldObj);
            if (data == null) continue;
            MovingObjectPosition mop = getMovingObjectPositionFromPlayer(player.worldObj, player, false);
            for (int i = 0; i < data.coords.length; i += 3) {
                int ix = data.coords[i + 0];
                int iy = data.coords[i + 1];
                int iz = data.coords[i + 2];
                if (ix == mop.blockX && iy == mop.blockY && iz == mop.blockZ) {
                    processing.set(true);
                    try {
                        mineSelection(is, data, player.worldObj, mop, player, held);
                    } finally {
                        processing.remove();
                    }
                    return;
                }
            }
        }
    }
}
