package net.minecraft.src;

import java.util.ArrayList;

import net.minecraft.src.forge.NetworkMod;

public class mod_WorldEditTools extends NetworkMod {
    int itemStart = 1600 - 256;

    @Override
    public String getVersion() {
        return null;
    }

    class WeTool extends Item {
        protected WeTool(int par1) {
            super(par1);
            setIconIndex(Item.axeWood.iconIndex);
            setFull3D();
        }

        @Override
        public int getIconIndex(ItemStack stack, int renderPass, EntityPlayer player,
                ItemStack usingItem, int useRemaining) {
            return Item.axeWood.getIconFromDamage(0);
        }

        @Override
        public int getIconFromDamage(int par1) {
            return Item.axeWood.getIconFromDamage(0);
        }

        @Override
        public String getItemName() {
            int i = (this.shiftedIndex - 256 - itemStart);
            if (i == 0) {
                return "WorldEdit Selection Wand";
            }
            return "WorldEdit Tool #" + i;
        }

        @Override
        public String getItemNameIS(ItemStack par1ItemStack) {
            return getItemName();
        }

        @Override
        public String getLocalItemName(ItemStack par1ItemStack) {
            return getItemName();
        }

        @Override
        public String getItemDisplayName(ItemStack par1ItemStack) {
            return getItemName();
        }

        @Override
        public void addCreativeItems(ArrayList itemList) {
            itemList.add(new ItemStack(this));
        }

        @Override
        public EnumRarity getRarity(ItemStack par1ItemStack) {
            return EnumRarity.rare;
        }

        @Override
        public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int X,
                int Y, int Z, int side) {

            return true;
        }

        @Override
        public boolean onBlockStartBreak(ItemStack itemstack, int X, int Y, int Z,
                EntityPlayer player) {
            return true;
        }
    }

    @Override
    public void load() {
        for (int i = itemStart; i < itemStart + 16; i++) {
            Item it = new WeTool(i);
        }
    }

}
