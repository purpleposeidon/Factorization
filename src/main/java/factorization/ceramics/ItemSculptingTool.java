package factorization.ceramics;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Coord;
import factorization.api.Quaternion;
import factorization.ceramics.TileEntityGreenware.ClayLump;
import factorization.ceramics.TileEntityGreenware.ClayState;
import factorization.common.FactoryType;
import factorization.common.ItemIcons;
import factorization.notify.Notice;
import factorization.shared.BlockRenderHelper;
import factorization.shared.Core;
import factorization.shared.Core.TabType;
import factorization.shared.FzUtil;
import factorization.shared.FzUtil.FzInv;
import factorization.shared.ItemFactorization;

public class ItemSculptingTool extends ItemFactorization {

    public ItemSculptingTool() {
        super("sculptTool", TabType.ART);
        setNoRepair();
        setMaxDamage(0);
        setMaxStackSize(4);
        setFull3D();
        setHasSubtypes(true);
    }
    
    public static void addModeChangeRecipes() {
        int length = ToolMode.values().length;
        ToolMode mode[] = ToolMode.values();
        for (int i = 0; i < length; i++) {
            int j = i + 1;
            for (; j < length; j++) {
                if (mode[j].craftable) {
                    break;
                }
            }
            if (j == length) {
                j = 0;
            }
            Core.registry.shapelessOreRecipe(fromMode(mode[j]), fromMode(mode[i]));
        }
    }
    
    @Override
    public void registerIcons(IIconRegister reg) { }

    static enum ToolMode {
        MOVER("move", true),
        STRETCHER("stretch", false),
        ROTATE_GLOBAL("rotate_global", true),
        ROTATE_LOCAL("rotate_local", false),
        RESETTER("reset", true),
        MOLD("mold", true);
        
        String name;
        boolean craftable;
        ToolMode next;
        
        private ToolMode(String english, boolean craftable) {
            this.name = english;
            this.craftable = craftable;
            this.next = this;
        }
        
        static void group(ToolMode ...group) {
            ToolMode prev = group[group.length - 1];
            for (ToolMode me : group) {
                me.next = prev;
                prev = me;
            }
        }
        
        static {
            group(MOVER, STRETCHER);
            group(RESETTER);
            group(ROTATE_GLOBAL, ROTATE_LOCAL);
            group(MOLD);
        }
    }
    
    ToolMode getMode(int damage) {
        if (damage < 0) {
            return ToolMode.MOVER;
        }
        if (damage >= ToolMode.values().length) {
            return ToolMode.MOVER;
        }
        return ToolMode.values()[damage];
    }
    
    static ItemStack fromMode(ToolMode mode) {
        return new ItemStack(Core.registry.sculpt_tool, 1, mode.ordinal());
    }
    
    @Override
    public IIcon getIconFromDamage(int damage) {
        //A bit lame. Lame.
        switch (getMode(damage)) {
        default:
        case MOVER: return ItemIcons.move;
        case RESETTER: return ItemIcons.reset;
        case ROTATE_LOCAL: return ItemIcons.rotate_local;
        case ROTATE_GLOBAL: return ItemIcons.rotate_global;
        case STRETCHER: return ItemIcons.stretch;
        case MOLD: return ItemIcons.mold;
        }
    }
    
    @Override
    public void addExtraInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        ToolMode mode = getMode(is.getItemDamage());
        String pre = "item.factorization:sculptTool.";
        list.add(StatCollector.translateToLocal(pre + mode));
        for (ToolMode nextMode = mode.next; nextMode != mode; nextMode = nextMode.next) {
            list.add(EnumChatFormatting.DARK_GRAY + "(" + StatCollector.translateToLocal(pre + nextMode) + ")");
        }
    }
    
    void changeMode(ItemStack is) {
        ToolMode mode = getMode(is.getItemDamage());
        is.setItemDamage(mode.next.ordinal());
    }
    
    @Override
    public boolean onItemUse(ItemStack par1ItemStack,
            EntityPlayer par2EntityPlayer, World par3World, int par4, int par5,
            int par6, int par7, float par8, float par9, float par10) {
        return tryPlaceIntoWorld(par1ItemStack, par2EntityPlayer, par3World, par4, par5,
                par6, par7, par8, par9, par10);
    }
    
    public static MovingObjectPosition doRayTrace(EntityPlayer player) {
        return Core.registry.sculpt_tool.getMovingObjectPositionFromPlayer(player.worldObj, player, true);
    }
    
    public boolean tryPlaceIntoWorld(ItemStack is, EntityPlayer player,
            World w, int x, int y, int z, int side,
            float vx, float vy, float vz) {
        if (w.isRemote) {
            return true;
        }
        Coord here = new Coord(w, x, y, z);
        TileEntityGreenware gw = here.getTE(TileEntityGreenware.class);
        if (gw == null) {
            if (player.isSneaking()) {
                changeMode(is);
                //creative mode has to go and over-complicate this.
                if (player.inventory.mainInventory[player.inventory.currentItem] == is) {
                    player.inventory.mainInventory[player.inventory.currentItem] = is.copy();
                }
                return true;
            }
            return false;
        }
        ClayState state = gw.getState();
        ToolMode mode = getMode(is.getItemDamage());
        if (mode == ToolMode.MOLD) {
            int is_fired = state.compareTo(ClayState.BISQUED);
            if (is_fired < 0 && !player.capabilities.isCreativeMode) {
                new Notice(here, "Not fired").send(player);
                return true;
            }
            FzInv inv = FzUtil.openInventory(player.inventory, 0);
            if (!player.capabilities.isCreativeMode) {
                ItemStack theSlab = null;
                int materialCount = 0;
                int neededClay = gw.parts.size();
                for (int i = 0; i < inv.size(); i++) {
                    ItemStack it = inv.get(i);
                    if (it == null) {
                        continue;
                    }
                    if (it.getItem() == Items.clay_ball) {
                        materialCount += it.stackSize;
                    }
                    if (theSlab == null && FzUtil.oreDictionarySimilar("slabWood", it)) {
                        theSlab = it;
                    }
                }
                if (theSlab == null || materialCount < neededClay) {
                    new Notice(here, "Need wood slab\nAnd %s clay", "" + neededClay).send(player); //TODO: Localize properly
                    return false;
                }
                inv.pull(theSlab, 1, false);
                inv.pull(new ItemStack(Items.clay_ball), gw.parts.size(), false);
            }
            TileEntityGreenware rep = (TileEntityGreenware) FactoryType.CERAMIC.getRepresentative();
            rep.loadParts(gw.getItem().getTagCompound());
            rep.totalHeat = 0;
            rep.glazesApplied = false;
            rep.lastTouched = 0;
            for (ClayLump part : rep.parts) {
                part.icon_id = null;
                part.icon_md = 0;
                part.icon_side = -1;
            }
            ItemStack toDrop = rep.getItem();
            if (gw.customName != null) {
                toDrop.setStackDisplayName(gw.customName);
            }
            if (inv.push(toDrop) != null) {
                player.dropPlayerItemWithRandomChoice(toDrop, false);
            }
            Core.proxy.updatePlayerInventory(player);
            return true;
        }
        if (state != ClayState.WET) {
            if (w.isRemote) {
                return false;
            }
            Notice msg = new Notice(gw.getCoord(), "");
            switch (state) {
            case DRY:
                msg.withItem(new ItemStack(Items.water_bucket)).setMessage("The clay is dry\nUse a {ITEM_NAME}");
                break;
            case BISQUED:
            case HIGHFIRED:
                msg.setMessage("This has been fired");
                break;
            default:
                msg.setMessage("This clay can not be reshaped.");
                break;
            }
            msg.send(player);
            return false;
        }
        if (w.isRemote) {
            return true;
        }
        
        //See EntityLiving.rayTrace
        MovingObjectPosition hitPart = getMovingObjectPositionFromPlayer(w, player, true);
        
        if (hitPart == null) {
            return false;
        }
        if (hitPart.subHit == -1) {
            return true;
        }
        int strength = Math.max(1, is.stackSize);
        
        ClayLump selection = gw.parts.get(hitPart.subHit);
        ClayLump test = selection.copy();
        boolean sneaking = player.isSneaking();
        switch (mode) {
        case MOVER:
            move(test, sneaking, side, strength);
            break;
        case STRETCHER:
            //move the nearest face of selected cube towards (of away from) the player
            stretch(test, sneaking, side, strength);
            break;
        case ROTATE_LOCAL:
            rotate_local(test, sneaking, side, strength);
            break;
        case ROTATE_GLOBAL:
            rotate_global(test, sneaking, side, strength);
            break;
        case RESETTER:
            if (sneaking) {
                Quaternion orig = test.quat;
                test.asDefault();
                test.quat = orig;
            } else {
                test.quat = new Quaternion();
            }
            break;
        case MOLD:
            new Notice(here, "Not fired").send(player);
            return true;
        }
        if (gw.isValidLump(test)) {
            gw.changeLump(hitPart.subHit, test);
        }
        return true;
    }
    
    void rotate_local(ClayLump cube, boolean reverse, int side, int strength) {
        float delta = (float) Math.toRadians(-360F/32F*strength);
        if (reverse) {
            delta *= -1;
        }
        ForgeDirection direction = ForgeDirection.getOrientation(side);
        cube.quat.incrMultiply(Quaternion.getRotationQuaternionRadians(delta, direction.offsetX, direction.offsetY, direction.offsetZ));
    }
    
    void rotate_global(ClayLump cube, boolean reverse, int side, int strength) {
        float delta = (float) Math.toRadians(-360F/32F*strength);
        if (reverse) {
            delta *= -1;
        }
        ForgeDirection direction = ForgeDirection.getOrientation(side);
        Quaternion global = Quaternion.getRotationQuaternionRadians(delta, direction.offsetX, direction.offsetY, direction.offsetZ);
        global.incrMultiply(cube.quat);
        cube.quat = global;
    }
    
    void move(ClayLump cube, boolean reverse, int side, int strength) {
        //shift origin 0.5, and corner by 0.5.
        ForgeDirection dir = ForgeDirection.getOrientation(side);
        stretch(cube, reverse, dir.ordinal(), strength);
        stretch(cube, !reverse, dir.getOpposite().ordinal(), strength);
    }
    
    void stretch(ClayLump cube, boolean reverse, int side, int strength) {
        //shift origin 0.5, and corner by 0.5.
        ForgeDirection dir = ForgeDirection.getOrientation(side);
        int delta = reverse ? -strength : strength;
        switch (dir) {
        case SOUTH:
            cube.maxZ += delta;
            break;
        case NORTH:
            cube.minZ -= delta;
            break;
        case EAST:
            cube.maxX += delta;
            break;
        case WEST:
            cube.minX -= delta;
            break;
        case UP:
            cube.maxY += delta;
            break;
        case DOWN:
            cube.minY -= delta;
            break;
        case UNKNOWN: break;
        }
    }
    
    @Override
    public boolean isItemTool(ItemStack is) {
        return true;
    }
}
