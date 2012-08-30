package factorization.common;

import java.util.List;

import net.minecraft.src.ItemBlock;
import net.minecraft.src.ItemStack;

public class ItemBlockResource extends ItemBlock {

    public ItemBlockResource() {
        super(Core.registry.factory_block.blockID
                + Core.block_item_id_offset);
        new Exception().printStackTrace();
        setMaxDamage(0);
        setHasSubtypes(true);
    }

    public ItemBlockResource(int id) {
        super(id);
        //Y'know, that -256 is really retarded.
        setMaxDamage(0);
        setHasSubtypes(true);
    }

    public int getIconFromDamage(int damage) {
        return Core.registry.resource_block.getBlockTextureFromSideAndMetadata(0, damage);
    }

    public int getMetadata(int i) {
        return i;
    }

    @Override
    public String getItemNameIS(ItemStack itemstack) {
        // I don't think this actually gets called...
        int md = itemstack.getItemDamage();
        if (ResourceType.SILVERORE.is(md)) {
            return "Silver Ore";
        }
        if (ResourceType.SILVERBLOCK.is(md)) {
            return "Block of Silver";
        }
        if (ResourceType.LEADBLOCK.is(md)) {
            return "Block of Lead";
        }
        if (ResourceType.DARKIRONBLOCK.is(md)) {
            return "Block of Dark Iron";
        }
        if (ResourceType.MECHAMODDER.is(md)) {
            return "Mecha-Workshop";
        }
        System.err.println("NOTE: ItemBlock is missing a name: " + itemstack);
        System.err.println("   MD = " + md);
        return "??? It's a Mystery!!!";
    }

    @Override
    public String getItemName() {
        return "ItemFactorizationResource";
    }

    @Override
    public void addInformation(ItemStack is, List infoList) {
        super.addInformation(is, infoList);
        Core.brand(infoList);
    }
}
