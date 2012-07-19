package factorization.src;

import net.minecraft.src.ItemStack;
import net.minecraft.src.TileEntity;

public enum FactoryType {
	ROUTER(0, true), // Send/retrieve items from connected inventories
	@Deprecated
	CUTTER(1, false),
	MAKER(2, true), // Create Craft Packets and put items into them.
	STAMPER(3, true), // Crafts craft packets, and outputs results
	@Deprecated
	QUEUE(4, false), // Store up to 16 items in a list, without merging.
	BARREL(5, false), // Store huge quantities of identical items
	LAMP(6, false), //spawn a bunch of AIR blocks around and below
	//7 -- this was the BlockDarkIron, which got moved.
	PACKAGER(8, true, STAMPER.gui), //crafts its input as a 3x3 or 2x2
	SENTRYDEMON(9, false), //load a chunk
	WRATHFIRE(10, false), //burn things
	SLAGFURNACE(11, true), //get extra ore output

	POCKETCRAFTGUI(101, true, 101),
	MECHATABLEGUI(102, true, 102), //Mecha-crafting
	MECHATABLEGUICONFIG(103, true, 103), //Mecha-armor editor
	NULLGUI(104, true, 104), ;

	final public int md;
	final public int gui;
	final public boolean hasGui;

	static class mapper {
		//bluh java
		static FactoryType mapping[] = new FactoryType[128];
	}

	FactoryType(int metadata, boolean use_gui, int gui_id) {
		md = metadata;
		if (use_gui) {
			gui = gui_id;
		} else {
			gui = -1;
		}
		hasGui = use_gui;
		assert mapper.mapping[md] == null;
		mapper.mapping[md] = this;
	}

	FactoryType(int md, boolean use_gui) {
		this(md, use_gui, md);
	}

	TileEntity makeTileEntity() {
		switch (this) {
		case ROUTER:
			return new TileEntityRouter();
		case CUTTER:
			return new TileEntityCutter();
		case MAKER:
			return new TileEntityMaker();
		case STAMPER:
			return new TileEntityStamper();
		case QUEUE:
			return new TileEntityQueue();
		case BARREL:
			return new TileEntityBarrel();
		case LAMP:
			return new TileEntityWrathLamp();
		case PACKAGER:
			return new TileEntityPackager();
		case SENTRYDEMON:
			return new TileEntityWatchDemon();
		case WRATHFIRE:
			return new TileEntityWrathFire();
		case SLAGFURNACE:
			return new TileEntitySlagFurnace();
		}
		return null;
	}

	public boolean isInstance(TileEntityFactorization ent) {
		if (ent == null) {
			return false;
		}
		return ent.getFactoryType() == this;
	}

	public boolean is(int md) {
		return md == this.md;
	}

	public static FactoryType fromMd(int md) {
		return mapper.mapping[md];
	}

	ItemStack itemStack(String name) {
		ItemStack ret = new ItemStack(Core.registry.item_factorization, 1, this.md);
		Core.instance.AddName(ret, name);
		return ret;
	}

	public static boolean isDark(int md) {
		return md == ROUTER.md || md == LAMP.md;
	}
}