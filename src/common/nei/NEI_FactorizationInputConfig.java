package factorization.nei;

import anf;
import aqh;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.forge.GuiContainerManager;
import codechicken.nei.forge.IContainerInputHandler;
import factorization.client.FactorizationClientProxy;
import factorization.common.Command;
import factorization.common.Core;
import factorization.common.FactorizationProxy;
import factorization.common.ItemPocketTable;
import factorization.common.Registry;

public class NEI_FactorizationInputConfig
  implements IConfigureNEI
{
  public void loadConfig()
  {
    GuiContainerManager.addInputHandler(new IContainerInputHandler()
    {
      public void onMouseUp(aqh gui, int mousex, int mousey, int button)
      {
      }

      public void onMouseScrolled(aqh gui, int mousex, int mousey, int scrolled)
      {
      }

      public void onMouseClicked(aqh gui, int mousex, int mousey, int button)
      {
      }

      public void onKeyTyped(aqh gui, char keyChar, int keyID)
      {
      }

      public boolean mouseScrolled(aqh gui, int mousex, int mousey, int scrolled)
      {
        return false;
      }

      public boolean mouseClicked(aqh gui, int mousex, int mousey, int button)
      {
        return false;
      }

      public boolean lastKeyTyped(aqh gui, char keyChar, int keyID)
      {
        if (FactorizationClientProxy.bag_swap_key.d == keyID) {
          Command.bagShuffle.call(Core.proxy.getClientPlayer());
          return true;
        }
        if (FactorizationClientProxy.pocket_key.d == keyID) {
          if (Core.registry.pocket_table.findPocket(Core.proxy.getClientPlayer()) != null) {
            Command.craftOpen.call(Core.proxy.getClientPlayer());
          }
          return true;
        }
        return false;
      }

      public boolean keyTyped(aqh gui, char keyChar, int keyCode)
      {
        return false;
      }
    });
  }
}

