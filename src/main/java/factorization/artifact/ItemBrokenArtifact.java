package factorization.artifact;

import factorization.shared.Core;
import factorization.shared.ItemFactorization;
import factorization.util.ItemUtil;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ItemBrokenArtifact extends ItemFactorization {
    public ItemBrokenArtifact() {
        super("brokenArtifact", Core.TabType.ARTIFACT);
    }

    public static ItemStack build(ItemStack orig) {
        final NBTTagCompound out = new NBTTagCompound();
        orig.writeToNBT(out);
        ItemStack ret = new ItemStack(Core.registry.brokenTool);
        NBTTagCompound tag = ItemUtil.getTag(ret);
        tag.setTag("broken", out);
        return ret;
    }

    @Override
    public void registerIcons(IIconRegister register) { }

    public static ItemStack get(ItemStack orig) {
        if (orig == null) return null;
        if (orig.getItem() != Core.registry.brokenTool) return null;
        NBTTagCompound tag = orig.getTagCompound();
        if (tag == null) return null;
        return ItemStack.loadItemStackFromNBT(tag.getCompoundTag("broken"));
    }

    @Override
    public String getUnlocalizedNameInefficiently/* Who named this? */(ItemStack is) {
        ItemStack held = get(is);
        if (held == null) return super.getUnlocalizedNameInefficiently(is);
        return Core.translateWithCorrectableFormat("item.factorization:brokenArtifact.shards", held.getDisplayName());
    }
}
