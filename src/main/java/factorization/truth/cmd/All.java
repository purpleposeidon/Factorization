package factorization.truth.cmd;

import factorization.truth.api.AgnosticCommand;
import factorization.truth.api.DocReg;
import factorization.truth.api.ITypesetCommand;
import net.minecraft.util.EnumChatFormatting;

public class All {
    private static void r(String name, InternalCmd cmd) {
        DocReg.registerCommand("\\" + name, cmd);
    }

    public static void register() {
        r("include", new CmdInclude());
        r("lmp", new CmdMacro("\\link{lmp}{LMP}"));
        r("p", new CmdP());
        r("-", new CmdDash());
        r("", new CmdSpace());
        r(" ", new CmdSpace());
        r("\\", new CmdSlash());
        r("newpage", new CmdNewpage());
        r("leftpage", new CmdLeftpage());
        r("b", new CmdStyle(EnumChatFormatting.BOLD, "b"));
        r("i", new CmdStyle(EnumChatFormatting.BOLD, "i"));
        r("u", new CmdStyle(EnumChatFormatting.BOLD, "u"));
        r("title", new CmdTitle());
        r("h1", new CmdHeader());
        r("link", new CmdLink(false));
        r("index", new CmdLink(true));
        r("#", new CmdItem());
        r("img", new CmdImg());
        r("figure", new CmdFigure());
        r("generate", new CmdGenerate());
        r("seg", new CmdSegStart());
        r("endseg", new CmdSegEnd());
        r("topic", new CmdTopic());
        r("checkmods", new CmdCheckMods());
        r("vpad", new CmdVpad());
        r("ifhtml", new CmdIfHtml());
        r("url", new CmdUrl());
        r("local", new CmdLocal());
    }
}
