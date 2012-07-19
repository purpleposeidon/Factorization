package net.minecraft.src;

import net.minecraft.src.forge.NetworkMod;

public class mod_UU_tweak extends NetworkMod {
	// ic2.common.Ic2Items.matter
	@Override
	public String getVersion() {
		return "0";
	}

	ItemStack getExternalItem(String className, String classField,
			String description) {
		try {
			Class c = Class.forName(className);
			return (ItemStack) c.getField(classField).get(null);
		} catch (Exception err) {
			System.out.println("Could not get " + description);
			// err.printStackTrace();
		}
		return null;

	}

	@Override
	public void modsLoaded() {
		ItemStack uu = getExternalItem("ic2.common.Ic2Items", "matter",
				"IC2 UU-matter");
		if (uu == null) {
			System.out
					.println("IC2 not installed (or not initialized, or names changed). Will not add UU matter recipe");
			return;
		}
		ModLoader.addRecipe(new ItemStack(Item.coal, 64), new Object[] { "UUU",
				"U U", "UUU", 'U', uu });
	}

	@Override
	public void load() {
	}

}
