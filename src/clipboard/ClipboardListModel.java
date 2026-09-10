package clipboard;

import java.util.ArrayList;
import java.util.List;

final class ClipboardListModel {

    private final List<String> items = new ArrayList<>();

    void addToTop(String text) {
        requireText(text);
        items.remove(text);
        items.add(0, text);
    }

    int replace(int index, String text) {
        requireText(text);
        items.remove(index);
        items.remove(text);
        int replacementIndex = Math.min(index, items.size());
        items.add(replacementIndex, text);
        return replacementIndex;
    }

    void moveToTop(int index) {
        items.add(0, items.remove(index));
    }

    void remove(int index) {
        items.remove(index);
    }

    String get(int index) {
        return items.get(index);
    }

    int size() {
        return items.size();
    }

    List<String> snapshot() {
        return List.copyOf(items);
    }

    private void requireText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Clipboard text must not be blank");
        }
    }
}
