package fi.dwo.eawt;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import javax.swing.JDialog;

public interface EAWT {
    void setQuit(BooleanSupplier quit);
    void setAbout(Supplier<JDialog> about);
}
