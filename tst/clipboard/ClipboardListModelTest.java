package clipboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class ClipboardListModelTest {

    @Test
    void addsNewItemsAtTheTop() {
        var model = new ClipboardListModel();

        model.addToTop("first");
        model.addToTop("second");

        assertEquals(List.of("second", "first"), model.snapshot());
    }

    @Test
    void copyingTheSameTextAgainMovesItToTheTop() {
        var model = new ClipboardListModel();
        model.addToTop("first");
        model.addToTop("second");

        model.addToTop("first");

        assertEquals(List.of("first", "second"), model.snapshot());
    }

    @Test
    void editsMovesAndDeletesItems() {
        var model = new ClipboardListModel();
        model.addToTop("first");
        model.addToTop("second");
        model.addToTop("third");

        int editedIndex = model.replace(1, "edited");
        model.moveToTop(editedIndex);
        model.remove(2);

        assertEquals(List.of("edited", "third"), model.snapshot());
    }

    @Test
    void rejectsBlankItems() {
        var model = new ClipboardListModel();

        assertThrows(IllegalArgumentException.class, () -> model.addToTop("  "));
    }
}
