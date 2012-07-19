package factorization.src;

import net.minecraft.src.ItemBlock;
import net.minecraft.src.ItemStack;
import net.minecraft.src.mod_Factorization;

public class ItemLightAir extends ItemBlock {
	public ItemLightAir() {
		super(mod_Factorization.lightair_id
				+ Core.block_item_id_offset);
		setMaxDamage(0);
		setHasSubtypes(true);
	}

	public int getMetadata(int i) {
		return i;
	}

	@Override
	public String getItemNameIS(ItemStack itemstack) {
		// I don't think this actually gets called...
		int md = itemstack.getItemDamage();
		return "Bright Air";
	}

	@Override
	public String getItemName() {
		return "item.lightair";
	}
}
