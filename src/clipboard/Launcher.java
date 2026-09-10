package clipboard;

import javafx.application.Application;

/** Plain Java launcher for environments that do not launch JavaFX Application classes directly. */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(ClipboardBagApp.class, args);
    }
}
