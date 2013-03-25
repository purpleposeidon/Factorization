package factorization.common;

import java.util.List;

import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.Quaternion;
import factorization.common.Core.TabType;
import factorization.common.TileEntityGreenware.ClayLump;
import factorization.common.TileEntityGreenware.ClayState;

public class ItemSculptingTool extends Item {

    protected ItemSculptingTool(int id) {
        super(id);
        setNoRepair();
        setMaxDamage(0);
        setMaxStackSize(4);
        setUnlocalizedName("factorization:sculptTool");
        Core.tab(this, TabType.TOOLS);
        setFull3D();
    }
    
    static void addModeChangeRecipes() {
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
            Core.registry.shapelessRecipe(fromMode(mode[j]), fromMode(mode[i]));
        }
    }
    
    @Override
    public void updateIcons(IconRegister reg) { }

    static enum ToolMode {
        MOVER("move", true),
        STRETCHER("stretch", false),
        REMOVER("delete", true),
        ROTATE_GLOBAL("rotate_global", true),
        ROTATE_LOCAL("rotate_local", false),
        RESETTER("reset", false);
        
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
            group(REMOVER, RESETTER);
            group(ROTATE_GLOBAL, ROTATE_LOCAL);
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
    public Icon getIconFromDamage(int damage) {
        //A bit lame. Lame.
        switch (getMode(damage)) {
        default:
        case MOVER: return ItemIcons.move;
        case REMOVER: return ItemIcons.delete;
        case RESETTER: return ItemIcons.reset;
        case ROTATE_LOCAL: return ItemIcons.rotate_local;
        case ROTATE_GLOBAL: return ItemIcons.rotate_global;
        case STRETCHER: return ItemIcons.stretch;
        }
    }
    
    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        ToolMode mode = getMode(is.getItemDamage());
        list.add(mode.name);
        for (ToolMode nextMode = mode.next; nextMode != mode; nextMode = nextMode.next) {
            list.add("(" + nextMode.name + ")");
        }
        Core.brand(list);
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
        if (state != ClayState.WET) {
            if (w.isRemote) {
                return false;
            }
            switch (state) {
            case DRY:
                Core.notify(player, gw.getCoord(), "The clay is dry\nUse a %s.", Core.getTranslationKey(Item.bucketWater));
                break;
            case BISQUED:
            case GLAZED:
                Core.notify(player, gw.getCoord(), "This clay has been fired and can not be reshaped.");
                break;
            default:
                Core.notify(player, gw.getCoord(), "This clay can not be reshaped.");
                break;
            }
            return false;
        }
        if (w.isRemote) {
            return true;
        }
        BlockRenderHelper hit = Core.registry.serverTraceHelper;
        
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
        case REMOVER:
            //delete selected
            gw.removeLump(hitPart.subHit);
            EntityItem drop;
            if (gw.parts.size() == 0) {
                here.setId(0);
                drop = new EntityItem(w, gw.xCoord, gw.yCoord, gw.zCoord, Core.registry.greenware_item.copy());
            } else {
                drop = new EntityItem(w, gw.xCoord, gw.yCoord, gw.zCoord, new ItemStack(Item.clay));
            }
            w.spawnEntityInWorld(drop);
            return true;
        case ROTATE_LOCAL:
            rotate_local(test, sneaking, side, strength);
            break;
        case ROTATE_GLOBAL:
            rotate_global(test, sneaking, side, strength);
            break;
        case RESETTER:
            test.quat = new Quaternion();
            break;
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
        cube.quat.incrMultiply(Quaternion.getRotationQuaternion(delta, direction.offsetX, direction.offsetY, direction.offsetZ));
    }
    
    void rotate_global(ClayLump cube, boolean reverse, int side, int strength) {
        float delta = (float) Math.toRadians(-360F/32F*strength);
        if (reverse) {
            delta *= -1;
        }
        ForgeDirection direction = ForgeDirection.getOrientation(side);
        Quaternion global = Quaternion.getRotationQuaternion(delta, direction.offsetX, direction.offsetY, direction.offsetZ);
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
        }
    }
}
