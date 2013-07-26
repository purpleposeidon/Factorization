package factorization.api;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import factorization.common.Core;
import factorization.common.FzConfig;
import factorization.fzds.TransferLib;

public class ChargeMetalBlockConductance {
    //static HashMap<Integer, ArrayList<Integer>> validBlocks = new HashMap();
    static boolean[][] validBlocks = new boolean[Block.blocksList.length][];
    public static ArrayList<ItemStack> excludeOres = new ArrayList<ItemStack>();
    
    public static void setup() {
        if (!FzConfig.invasiveCharge) {
            return;
        }
        throw new IllegalArgumentException("Invasive charge disabled");
//		GameRegistry.registerTileEntity(InvasiveCharge.class, "factorization.invasiveCharge");
//		
//		for (String metalName : Arrays.asList(
//				"Copper", "Tin", "Lead", "Gold", "Silver", /* standard blocks */
//				"Emerald" /* a joke. */, 
//				"Aluminum", "Platinum", "Zinc", /* weird mod metals */
//				"AluminumBrass", "Cobalt", "Ardite", "Manyullyn" /* tcons metalss */ )) {
//			subIngots: for (ItemStack is : OreDictionary.getOres("block" + metalName)) {
//				int metadata = is.getItemDamage();
//				if (is.hasTagCompound() || metadata < 0 || metadata > 16) {
//					continue;
//				}
//				if (is.itemID >= Block.blocksList.length || is.itemID <= 0) {
//					continue;
//				}
//				Block b = Block.blocksList[is.itemID];
//				if (b == null) {
//					continue;
//				}
//				if (b.hasTileEntity(metadata)) {
//					continue;
//				}
//				for (ItemStack exclude : excludeOres) {
//					if (FactorizationUtil.couldMerge(exclude, is)) {
//						continue subIngots;
//					}
//				}
//				put(b.blockID, metadata);
//			}
//		}
//		for (Block block : new Block[] { Block.blockIron, Block.blockGold, Block.blockEmerald }) {
//			put(block.blockID, 0);
//		}
    }
    
    static void put(int id, int md) {
        boolean[] mds = validBlocks[id];
        if (mds == null) {
            validBlocks[id] = mds = new boolean[16];
        }
        mds[md] = true;
    }
    
    public static void taintBlock(Coord c) {
        int blockID = c.getId(), md = c.getMd();
        if (validBlocks[blockID] == null || !validBlocks[blockID][md]) {
            return;
        }
        if (c.getTE() != null) {
            return;
        }
        InvasiveCharge te = new InvasiveCharge();
        te.validate();
        te.initialize(blockID, md);
        int orig_id = c.getId(), orig_md = c.getMd();
        TransferLib.setRaw(c, Core.registry.factory_block.blockID, 0);
        c.setTE(te);
        TransferLib.setRaw(c, orig_id, orig_md, 0);
    }
}
