package factorization.truth.word;

import factorization.shared.Sound;
import factorization.util.FzUtil;

public class ClipboardWord extends TextWord {
    final String clipboard;

    public ClipboardWord(String toCopy) {
        super("[X]", null);
        this.clipboard = toCopy;
        // TODO: Clipboard icon
    }

    @Override
    public boolean onClick() {
        FzUtil.copyStringToClipboard(clipboard);
        Sound.leftClick.play();
        return true;
    }
}
