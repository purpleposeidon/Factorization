package factorization.beauty;

import cpw.mods.fml.common.registry.GameRegistry;
import factorization.shared.Core;
import factorization.shared.ItemFactorization;
import factorization.util.DataUtil;
import factorization.util.ItemUtil;
import factorization.util.PlayerUtil;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;

public class ItemLeafBomb extends ItemFactorization {
    public ItemLeafBomb() {
        super("leafBomb", Core.TabType.TOOLS);
    }

    public ItemStack getLeaves(ItemStack stack) {
        if (!stack.hasTagCompound()) return null;
        return DataUtil.tag2item(stack.getTagCompound().getCompoundTag("leaves"), null);
    }

    public void setLeaves(ItemStack stack, ItemStack leaves) {
        ItemUtil.getTag(stack).setTag("leaves", DataUtil.item2tag(leaves));
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        ItemStack leaves = getLeaves(stack);
        if (leaves == null) return stack;

        world.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));

        if (!world.isRemote) {
            world.spawnEntityInWorld(new EntityLeafBomb(world, player, stack.copy()));
        }

        PlayerUtil.decr(player, stack);

        return stack;
    }

    @Override
    public boolean getHasSubtypes() {
        return true;
    }

    @Override
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        list.addAll(all);
    }

    ArrayList<ItemStack> all = new ArrayList<ItemStack>();

    transient ArrayList<ItemStack> _leaves = new ArrayList<ItemStack>();
    transient ArrayList<String> known = new ArrayList<String>();

    private void add(ItemStack leaf) {
        String hash = leaf.getUnlocalizedName();
        if (known.contains(hash)) return;
        known.add(hash);
        _leaves.add(leaf);
    }

    public void addRecipes() {
        for (ItemStack is : OreDictionary.getOres("treeLeaves")) {
            if (is == null) continue;
            if (!ItemUtil.isWildcard(is, false)) {
                add(is);
            } else if (!is.getItem().getHasSubtypes()) {
                is = is.copy();
                is.setItemDamage(0);
                add(is);
            } else {
                for (int i = 0; i < 16; i++) {
                    ItemStack leaf = is.copy();
                    leaf.setItemDamage(i);
                    add(leaf);
                }
            }
        }

        for (ItemStack leaf : _leaves) {
            leaf = leaf.copy();
            leaf.stackSize = 1;
            ItemStack bomb = new ItemStack(this);
            setLeaves(bomb, ItemUtil.copyWithSize(leaf, 8 + 4));

            GameRegistry.addShapelessRecipe(bomb, Core.registry.sap, leaf, leaf, leaf, leaf, leaf, leaf, leaf, leaf);
            all.add(bomb);
        }
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        NBTTagList enchants = Items.enchanted_book.func_92110_g(book);
        for (int i = 0; i < enchants.tagCount(); i++) {
            NBTTagCompound tag = enchants.getCompoundTagAt(i);
            int enchantId = tag.getInteger("id");
            if (enchantId == Enchantment.fortune.effectId) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getItemEnchantability() {
        return 0;
    }

    @Override
    protected void addExtraInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        ItemStack leaves = getLeaves(is);
        if (leaves == null) return;
        list.add(leaves.getItem().getItemStackDisplayName(leaves));
    }
}
