package factorization.common;

import java.util.List;

import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.common.Core.TabType;

public class ItemSculptingTool extends Item {

    protected ItemSculptingTool(int id) {
        super(id);
        setNoRepair();
        setMaxDamage(0);
        setMaxStackSize(1);
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
    public void registerIcon(IconRegister reg) { }

    static enum ToolMode {
        SELECTOR("select", true),
        MOVER("move", true),
        STRETCHER("stretch", false),
        REMOVER("delete", true),
        ROTATOR("rotate", true),
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
        }
    }
    
    ToolMode getMode(int damage) {
        if (damage < 0) {
            return ToolMode.SELECTOR;
        }
        if (damage >= ToolMode.values().length) {
            return ToolMode.SELECTOR;
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
        case ROTATOR: return ItemIcons.rotate;
        case SELECTOR: return ItemIcons.select;
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
    
    /*
    @Override
    public boolean onItemUse(ItemStack par1ItemStack,
            EntityPlayer par2EntityPlayer, World par3World, int par4, int par5,
            int par6, int par7, float par8, float par9, float par10) {
        return tryPlaceIntoWorld(par1ItemStack, par2EntityPlayer, par3World, par4, par5,
                par6, par7, par8, par9, par10);
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
            if (mode == ToolMode.SELECTOR) {
                if (gw.parts.size() == 0) {
                    return true;
                }
                int id = gw.parts.indexOf(gw.selected);
                if (player.isSneaking()) {
                    id--;
                    if (id <= -1) {
                        id = gw.parts.size() - 1;
                    }
                } else {
                    id++;
                    if (id == gw.parts.size()) {
                        id = 0;
                    }
                }
                gw.selected = gw.parts.get(id);
                gw.broadcastMessage(null, MessageType.SculptSelect, id);
            }
            return true;
        }
        if (mode == ToolMode.SELECTOR) {
            return true;
        }
        SelectionInfo sel = TileEntityGreenware.selections.get(player.username);
        if (sel == null) {
            return false;
        }
        if (sel.gw != gw) {
            return false;
        }
        if (sel.id < 0 || sel.id >= gw.parts.size()) {
            return false;
        }
        RenderingCube selection = gw.parts.get(sel.id);
        RenderingCube test;
        switch (mode) {
        case MOVER:
            test = selection.copy();
            move(test, player.isSneaking(), side);
            if (TileEntityGreenware.isValidLump(test)) {
                move(selection, player.isSneaking(), side);
                gw.shareLump(sel.id, selection);
            }
            break;
        case STRETCHER:
            //move the nearest face of selected cube towards (of away from) the player
            test = selection.copy();
            stretch(test, player.isSneaking(), side);
            if (TileEntityGreenware.isValidLump(test)) {
                stretch(selection, player.isSneaking(), side);
                gw.shareLump(sel.id, selection);
            }
            break;
        case REMOVER:
            //delete selected
            gw.removeLump(sel.id);
            EntityItem drop;
            if (gw.parts.size() == 0) {
                here.setId(0);
                drop = new EntityItem(w, gw.xCoord, gw.yCoord, gw.zCoord, Core.registry.greenware_item.copy());
            } else {
                drop = new EntityItem(w, gw.xCoord, gw.yCoord, gw.zCoord, new ItemStack(Item.clay));
            }
            w.spawnEntityInWorld(drop);
            break;
        case ROTATOR:
            test = selection.copy();
            rotate(test, player.isSneaking(), side);
            if (TileEntityGreenware.isValidLump(test)) {
                rotate(selection, player.isSneaking(), side);
                gw.shareLump(sel.id, selection);
            }
            break;
        case RESETTER:
            selection.trans.reset();
            gw.shareLump(sel.id, selection);
            break;
        }
        
        return true;
    }
    
    void rotate(RenderingCube cube, boolean reverse, int side) {
        float delta = (float) Math.toRadians(360F/32F);
        if (reverse) {
            delta *= -1;
        }
        ForgeDirection direction = ForgeDirection.getOrientation(side);
        cube.trans.rotate(direction.offsetX, direction.offsetY, direction.offsetZ, delta);
    }
    
    void move(RenderingCube cube, boolean reverse, int side) {
        //shift origin 0.5, and corner by 0.5.
        ForgeDirection dir = ForgeDirection.getOrientation(side);
        stretch(cube, reverse, dir.ordinal());
        stretch(cube, !reverse, dir.getOpposite().ordinal());
    }
    
    void stretch(RenderingCube cube, boolean reverse, int side) {
        //shift origin 0.5, and corner by 0.5.
        ForgeDirection dir = ForgeDirection.getOrientation(side);
        float delta = reverse ? -0.5F : 0.5F;
        switch (dir) {
        case SOUTH:
            cube.corner.z += delta;
            cube.trans.translate(0, 0, delta);
            break;
        case NORTH:
            cube.corner.z += delta;
            cube.trans.translate(0, 0, -delta);
            break;
        case EAST:
            cube.corner.x += delta;
            cube.trans.translate(delta, 0, 0);
            break;
        case WEST:
            cube.corner.x += delta;
            cube.trans.translate(-delta, 0, 0);
            break;
        case UP:
            cube.corner.y += delta;
            cube.trans.translate(0, delta, 0);
            break;
        case DOWN:
            cube.corner.y += delta;
            cube.trans.translate(0, -delta, 0);
            break;
        }
    }
    */
}
