package factorization.src.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiContainer;
import net.minecraft.src.IInventory;
import net.minecraft.src.Slot;
import net.minecraft.src.StatCollector;
import net.minecraft.src.TileEntity;

import org.lwjgl.opengl.GL11;

import factorization.src.ContainerFactorization;
import factorization.src.Core;
import factorization.src.NetworkFactorization.MessageType;
import factorization.src.TileEntityRouter;
import factorization.src.render.ButtonSet.Predicate;

public class GuiRouter extends GuiContainer implements IClickable {
	TileEntityRouter router;
	final int mode_button_id = 1, upgrade_button_id = 2;
	final int direction_button_id = 10, slot_up = 11, slot_down = 12;
	final int strict_entity = 20, next_entity = 21;

	GuiButton mode_button, direction_button, upgrade_button;
	GuiButton slot_up_button, slot_down_button;
	GuiButton strict_entity_button, next_entity_button;

	ArrayList<String> inv_names;
	ButtonSet global_buttons = new ButtonSet();
	ButtonSet main_buttons = new ButtonSet();
	ButtonSet item_filter_buttons = new ButtonSet();
	ButtonSet machine_filter_buttons = new ButtonSet();
	ButtonSet speed_buttons = new ButtonSet();
	ButtonSet thorough_buttons = new ButtonSet();
	ButtonSet bandwidth_buttons = new ButtonSet();

	ButtonSet allSets[] = { main_buttons, item_filter_buttons, machine_filter_buttons, speed_buttons, thorough_buttons, bandwidth_buttons };
	ButtonSet current_set = allSets[0];

	String[] side_names = { "bottom sides", "top sides", "§asouth§r sides", "§3north§r sides", "§eeast§r sides",
			"§5west§r sides", "any sides" };
	static final String any_inv = "all machines in network";

	public GuiRouter(ContainerFactorization cont) {
		super(cont);

		this.router = (TileEntityRouter) cont.factory;
		HashMap<String, Integer> names = new HashMap<String, Integer>();
		int max_dist = 24 * 24;
		for (Object obj : router.worldObj.loadedTileEntityList) {
			if (obj instanceof IInventory && obj instanceof TileEntity) {
				TileEntity ent = (TileEntity) obj;
				double invDistance = ent.getDistanceFrom(router.xCoord,
						router.yCoord, router.zCoord);
				if (invDistance > max_dist) {
					continue;
				}
				String invName = router.getIInventoryName((IInventory) obj);
				Integer orig = names.get(invName);
				if (orig == null || invDistance < orig) {
					names.put(invName, new Integer((int) invDistance));
				}
			}
		}
		names.remove(router.getIInventoryName(router));
		class NamesComparator implements Comparator<String> {
			HashMap<String, Integer> src;

			NamesComparator(HashMap<String, Integer> s) {
				src = s;
			}

			@Override
			public int compare(String a, String b) {
				return src.get(a) - src.get(b);
			}
		}
		inv_names = new ArrayList<String>(names.keySet());
		Collections.sort(inv_names, new NamesComparator(names));
	}

	@Override
	public void initGui() {
		super.initGui();
		final int bh = 20;
		final int LEFT = guiLeft + 7;
		final int TOP = guiTop + bh;
		final int row_top = TOP;
		final int row2_top = row_top + bh + 2;
		global_buttons.clear();
		int mode_width = 51;
		mode_button = global_buttons.add(mode_button_id, LEFT, row_top, mode_width, bh, "Insert");
		upgrade_button = global_buttons.add(upgrade_button_id, global_buttons.currentRight + 24 + 1, -1, bh, bh, "--");

		main_buttons.clear();
		int dbw = 16; //delta button width
		slot_down_button = main_buttons.add(slot_down, LEFT, row2_top, dbw, bh, "-");
		direction_button = main_buttons.add(direction_button_id, -1, -1, (xSize - 16) - dbw * 2, bh, "Slot...");
		slot_up_button = main_buttons.add(slot_up, -1, -1, dbw, bh, "+");

		item_filter_buttons.clear();
		item_filter_buttons.add(global_buttons.currentRight + 8, row_top + 6, "Item Filter");
		item_filter_buttons.setTest(new Predicate<TileEntity>() {
			public boolean test(TileEntity a) {
				return ((TileEntityRouter) a).upgradeItemFilter;
			}
		});

		machine_filter_buttons.clear();
		strict_entity_button = machine_filter_buttons.add(strict_entity, global_buttons.currentRight + 4, row_top, 63, bh, "visit all"); //visit near/visit all
		next_entity_button = machine_filter_buttons.add(next_entity, LEFT, row2_top, xSize - 16, bh, any_inv);
		machine_filter_buttons.setTest(new Predicate<TileEntity>() {
			public boolean test(TileEntity a) {
				return ((TileEntityRouter) a).upgradeMachineFilter;
			}
		});

		int lh = 9;
		int line1 = row2_top + 2;
		int line2 = line1 + lh;
		int line3 = line2 + lh;

		speed_buttons.clear();
		speed_buttons.add(LEFT, line1, Core.registry.router_speed.getItemDisplayName(null));
		speed_buttons.add(LEFT, line2, "No delay when visiting machines");
		speed_buttons.setTest(new Predicate<TileEntity>() {
			public boolean test(TileEntity a) {
				return ((TileEntityRouter) a).upgradeSpeed;
			}
		});

		thorough_buttons.clear();
		thorough_buttons.add(LEFT, line1, Core.registry.router_thorough.getItemDisplayName(null));
		thorough_buttons.add(LEFT, line2, "Always finish serving machines");
		thorough_buttons.setTest(new Predicate<TileEntity>() {
			public boolean test(TileEntity a) {
				return ((TileEntityRouter) a).upgradeThorough;
			}
		});

		bandwidth_buttons.clear();
		bandwidth_buttons.add(LEFT, line1, Core.registry.router_throughput.getItemDisplayName(null));
		bandwidth_buttons.add(LEFT, line2, "Move stacks at a time");
		bandwidth_buttons.setTest(new Predicate<TileEntity>() {
			public boolean test(TileEntity a) {
				return ((TileEntityRouter) a).upgradeThroughput;
			}
		});

		if (router.guiLastButtonSet >= 0 && router.guiLastButtonSet < allSets.length) {
			current_set = allSets[router.guiLastButtonSet];
			if (!current_set.canShow(router)) {
				selectNextUpgrade();
			}
		}
		updateGui();
	}

	void updateGui() {
		if (router.is_input) {
			mode_button.displayString = "Insert";
		} else {
			mode_button.displayString = "Extract";
		}

		upgrade_button.enabled = false;
		for (int i = 1; i < allSets.length; i++) {
			if (allSets[i].canShow(router)) {
				upgrade_button.enabled = true;
				break;
			}
		}
		upgrade_button.drawButton = upgrade_button.enabled;

		String m = "";
		if (router.target_slot < 0) {
			m = side_names[router.target_side];
			slot_up_button.enabled = false;
			slot_down_button.enabled = false;
		} else {
			m = "slot " + router.target_slot;
			slot_up_button.enabled = true;
			slot_down_button.enabled = true;
		}
		if (router.is_input) {
			m = "into " + m;
		}
		else {
			m = "from " + m;
		}
		direction_button.displayString = m;
		if (router.match == null || router.match.equals("")) {
			next_entity_button.displayString = any_inv;
		} else {
			next_entity_button.displayString = router.match;
		}

		for (int i = 1; i <= 9; i++) {
			Slot s = (Slot) inventorySlots.inventorySlots.get(i);
			if (router.upgradeItemFilter && current_set == item_filter_buttons) {
				if (s.yDisplayPosition < 0) {
					s.yDisplayPosition += 0xFFFFFF;
				}
			}
			else {
				if (s.yDisplayPosition > 0) {
					s.yDisplayPosition -= 0xFFFFFF;
				}
			}
		}

		next_entity_button.displayString = StatCollector.translateToLocal(next_entity_button.displayString);
		strict_entity_button.displayString = router.match_to_visit ? "visit near" : "visit all";
	}

	protected void drawGuiContainerForegroundLayer() {
		fontRenderer.drawString("Item Router", 60, 6, 0x404040);
		fontRenderer.drawString("Inventory", 8, (ySize - 96) + 2, 0x404040);
	}

	protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
		int k = mc.renderEngine.getTexture(Core.texture_dir + "routergui.png");
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		mc.renderEngine.bindTexture(k);
		int l = (width - xSize) / 2;
		int i1 = (height - ySize) / 2;
		drawTexturedModalRect(l, i1, 0, 0, xSize, ySize);
		updateGui();
		if (current_set == item_filter_buttons) {
			drawTexturedModalRect(l + 8 - 1, i1 + 44 - 1, 0, 238, 162, 18);
		}
		global_buttons.draw(mc, i, j);
		current_set.draw(mc, i, j);
		String msg = "";
		if (global_buttons.focused_id == upgrade_button_id && current_set != main_buttons) {
			msg = "Press DELETE to remove this upgrade. ";
		}
		fontRenderer.drawString(msg, guiLeft, guiTop + ySize + 2, 0x707070);
	}

	void selectNextUpgrade() {
		for (int i = 0; i < allSets.length; i++) {
			if (allSets[i] == current_set) {
				for (int j = i + 1; j != i; j++) {
					if (j == allSets.length) {
						j = 0;
					}
					if (allSets[j].canShow(router)) {
						current_set = allSets[j];
						router.guiLastButtonSet = j;
						return;
					}
				}
				router.guiLastButtonSet = 0;
				return;
			}
		}
	}

	@Override
	public void actionPerformedMouse(GuiButton guibutton, boolean rightClick) {
		switch (guibutton.id) {
		case upgrade_button_id:
			selectNextUpgrade();
			break;
		case mode_button_id:
			router.is_input = !router.is_input;
			router.broadcastItem(MessageType.RouterIsInput, null);
			break;
		case direction_button_id:
			if (0 <= router.target_slot) {
				router.target_slot = ~router.target_slot;
				if (rightClick) {
					router.target_side = 6;
				} else {
					router.target_side = 0;
				}
			} else {
				if (rightClick) {
					router.target_side--;
				} else {
					router.target_side++;
				}
				if (router.target_side < 0) {
					router.target_slot = ~router.target_slot;
				} else if (router.target_side >= 7) {
					router.target_slot = ~router.target_slot;
					router.target_side = 0;
				}
			}
			router.broadcastItem(MessageType.RouterTargetSide, null);
			router.broadcastItem(MessageType.RouterSlot, null);
			break;
		case slot_up:
			if (router.target_slot < 0) {
				router.target_slot = ~router.target_slot;
			} else {
				if (rightClick) {
					router.target_slot += 10;
				} else {
					router.target_slot++;
				}
			}
			router.broadcastItem(MessageType.RouterSlot, null);
			break;
		case slot_down:
			if (router.target_slot < 0) {
				router.target_slot = ~router.target_slot;
			} else {
				if (rightClick) {
					router.target_slot -= 10;
				} else {
					router.target_slot--;
				}
				if (router.target_slot < 0) {
					router.target_slot = 0;
				}
			}
			router.broadcastItem(MessageType.RouterSlot, null);
			break;
		case strict_entity:
			router.match_to_visit = !router.match_to_visit;
			router.broadcastItem(MessageType.RouterMatchToVisit, null);
			break;
		case next_entity:
			if (inv_names.size() == 0) {
				// empty
				router.match = "";
				router.broadcastItem(MessageType.RouterMatch, null);
				return;
			}
			int i = inv_names.indexOf(router.match);

			if (rightClick) {
				i--;
			} else {
				i++;
			}

			if (i >= inv_names.size() || i == -1) {
				// at an end
				router.match = "";
			} else if (i < -1) {
				// went backwards
				router.match = inv_names.get(inv_names.size() - 1);
			} else {
				router.match = inv_names.get(i);
			}

			router.broadcastItem(MessageType.RouterMatch, null);
			break;
		default:
			return;
		}
	}

	protected void mouseClicked(int x, int y, int button) {
		super.mouseClicked(x, y, button);
		global_buttons.handleClick(this, mc, x, y, button);
		current_set.handleClick(this, mc, x, y, button);
		updateGui();
	}

	@Override
	protected void keyTyped(char c, int i) {
		if (i == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
			router.broadcastItem(MessageType.RouterMatch, null);
			super.keyTyped(c, i);
			return;
		}
		if (i == org.lwjgl.input.Keyboard.KEY_DELETE && global_buttons.focused_id == upgrade_button_id) {
			int upgrade_id = -1;

			if (current_set == item_filter_buttons) {
				upgrade_id = Core.registry.router_item_filter.upgradeId;
			}
			else if (current_set == machine_filter_buttons) {
				upgrade_id = Core.registry.router_machine_filter.upgradeId;
			}
			else if (current_set == speed_buttons) {
				upgrade_id = Core.registry.router_speed.upgradeId;
			}
			else if (current_set == thorough_buttons) {
				upgrade_id = Core.registry.router_thorough.upgradeId;
			}
			else if (current_set == bandwidth_buttons) {
				upgrade_id = Core.registry.router_throughput.upgradeId;
			}
			if (upgrade_id == -1) {
				return;
			}
			if (!Core.instance.isCannonical(router.worldObj)) {
				router.broadcastMessage(null, MessageType.RouterDowngrade, upgrade_id);
			}
			router.removeUpgrade(upgrade_id, Core.instance.getClientPlayer());
			selectNextUpgrade();
			return;
		}
		if (current_set.focused_id == next_entity) {
			if (c == '\n') {
				router.broadcastItem(MessageType.RouterMatch, null);
			} else if (i == org.lwjgl.input.Keyboard.KEY_BACK) {
				String text = router.match;
				if (text != null && text.length() > 0) {
					router.match = text.substring(0, text.length() - 1);
				}
			} else if (c != 0) {
				if (router.match == null) {
					router.match = "";
				}
				router.match = router.match + c;
				router.match = router.match.replaceAll("\\p{Cntrl}", "");
			}
			return;
		}
		if (current_set.focused_id == direction_button.id) {
			boolean change = false;
			int add_digit = -1;
			if (i == org.lwjgl.input.Keyboard.KEY_BACK) {
				change = true;
			} else if (c != 0) {
				try {
					add_digit = Integer.parseInt(c + "");
					change = true;
				} catch (NumberFormatException e) {

				}
			}
			if (change) {
				if (router.target_slot < 0) {
					router.target_slot = ~router.target_slot;
				}
				if (add_digit == -1) {
					router.target_slot /= 10;
				} else {
					router.target_slot *= 10;
					router.target_slot += add_digit;
				}
				router.broadcastItem(MessageType.RouterSlot, null);
			}
			return;
		}
		super.keyTyped(c, i);
	}
}
