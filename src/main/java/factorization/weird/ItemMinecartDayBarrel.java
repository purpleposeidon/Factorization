package factorization.weird;

import com.mojang.authlib.GameProfile;
import factorization.common.FactoryType;
import factorization.shared.Core;
import factorization.shared.ItemFactorization;
import factorization.util.LangUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

import java.util.List;

/*
 * Created by asie on 6/11/15.
 */
// @Optional.Interface(iface = "mods.railcraft.api.core.items.IMinecartItem", modid = "Railcraft")
public class ItemMinecartDayBarrel extends ItemFactorization {
    // NORELEASE: I'm using more raw EnumProperty<EnumFacing> rather than a more specific FacingProperty in places
    private static final IBehaviorDispenseItem dispenserMinecartBehavior = new BehaviorDefaultDispenseItem() {
        private final BehaviorDefaultDispenseItem behaviourDefaultDispenseItem = new BehaviorDefaultDispenseItem();

        public ItemStack dispenseStack(IBlockSource at, ItemStack is) {
            World world = at.getWorld();
            IBlockState dispenserState = world.getBlockState(at.getBlockPos());
            EnumFacing enumfacing = dispenserState.getValue(BlockDispenser.FACING);
            double x = at.getX() + (double) ((float) enumfacing.getFrontOffsetX() * 1.125F);
            double y = at.getY() + (double) ((float) enumfacing.getFrontOffsetY() * 1.125F);
            double z = at.getZ() + (double) ((float) enumfacing.getFrontOffsetZ() * 1.125F);
            BlockPos target = at.getBlockPos().offset(enumfacing);
            IBlockState bs = world.getBlockState(target);
            Block block = bs.getBlock();
            double yOffset;

            if (BlockRailBase.isRailBlock(bs)) {
                yOffset = 0.0D;
            } else {
                if (block.getMaterial() != Material.air || !BlockRailBase.isRailBlock(world.getBlockState(target.down()))) {
                    return this.behaviourDefaultDispenseItem.dispense(at, is);
                }

                yOffset = -1.0D;
            }

            EntityMinecart entityminecart = Core.registry.barrelCart.placeCart(null, is, at.getWorld(), new BlockPos((int) x, (int) y, (int) z));

            if (is.hasDisplayName()) {
                entityminecart.setCustomNameTag(is.getDisplayName());
            }
            return is;
        }

        protected void playDispenseSound(IBlockSource at) {
            at.getWorld().playAuxSFX(1000, at.getBlockPos(), 0);
        }
    };

    public ItemMinecartDayBarrel() {
        super("barrelCart", Core.TabType.TOOLS);
        setMaxStackSize(1); // Just for now. It gets reset later to copy railcraft.
        BlockDispenser.dispenseBehaviorRegistry.putObject(this, dispenserMinecartBehavior);
        setHasSubtypes(true);
    }

    @Override
    public boolean onItemUse(ItemStack is, EntityPlayer player, World w, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (w.isRemote) return true;
        if (!BlockRailBase.isRailBlock(w.getBlockState(pos))) {
            return true;
        }
        placeCart(null, is, w, pos);
        return true;
    }

    @Override
    protected void addExtraInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        Core.registry.daybarrel.addExtraInformation(is, player, list, verbose);
    }

    @Override
    public String getItemStackDisplayName(ItemStack is) {
        if (is.hasTagCompound()) {
            String name = Core.registry.daybarrel.getItemStackDisplayName(is);
            return LangUtil.translateWithCorrectableFormat("item.factorization:barrelCart.known", name);
        }
        return super.getItemStackDisplayName(is);
    }

    /* NORELEASE: Railcraft?
    @Override
    public boolean canBePlacedByNonPlayer(ItemStack cart) {
        return true;
    }

    */

    // @Override
    public EntityMinecart placeCart(GameProfile owner, ItemStack cart, World world, BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        EntityMinecartDayBarrel minecart = new EntityMinecartDayBarrel(world, x + 0.5F, y + 0.5F, z + 0.5F);
        minecart.initFromStack(cart);
        cart.stackSize--;
        world.spawnEntityInWorld(minecart);
        return minecart;
    }

    ItemStack creative_cart = null;

    @Override
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        super.getSubItems(item, tab, list);
        if (creative_cart == null) {
            ItemStack creative = null;
            for (ItemStack barrel : TileEntityDayBarrel.barrel_items) {
                TileEntityDayBarrel.Type type = TileEntityDayBarrel.getUpgrade(barrel);
                if (type == TileEntityDayBarrel.Type.CREATIVE) {
                    creative = barrel;
                    break;
                }
            }
            if (creative == null) return;
            creative_cart = makeBarrel(creative);
        }
        if (creative_cart != null) {
            list.add(creative_cart);
        }
    }

    public ItemStack makeBarrel(ItemStack barrelItem) {
        ItemStack ret = new ItemStack(this, 1, barrelItem.getItemDamage());
        ret.setTagCompound((NBTTagCompound) barrelItem.getTagCompound().copy());
        return ret;
    }

    @Override
    public boolean hasContainerItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getContainerItem(ItemStack stack) {
        if (stack == null) return null;
        TileEntityDayBarrel barrel = (TileEntityDayBarrel) FactoryType.DAYBARREL.getRepresentative();
        barrel.loadFromStack(stack);
        return barrel.getPickedBlock();
    }
}
