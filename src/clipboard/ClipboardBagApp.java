package clipboard;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ClipboardBagApp extends Application {

    private final ClipboardListModel model = new ClipboardListModel();
    private final ObservableList<String> visibleItems = FXCollections.observableArrayList();
    private final Label status = new Label("Copy text anywhere, paste here, or drop text onto the list.");

    private ListView<String> listView;
    private Timeline clipboardPoller;
    private String lastClipboardText;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Clipboard List");

        listView = new ListView<>(visibleItems);
        listView.setPlaceholder(new Label("No clipboard items yet"));
        listView.setCellFactory(_ -> createListCell());
        configureDropTarget();

        var copyButton = new Button("Copy");
        copyButton.setOnAction(_ -> copySelected());
        var editButton = new Button("Edit");
        editButton.setOnAction(_ -> editSelected());
        var topButton = new Button("Move to top");
        topButton.setOnAction(_ -> moveSelectedToTop());
        var deleteButton = new Button("Delete");
        deleteButton.setOnAction(_ -> deleteSelected());

        var alwaysOnTop = new CheckBox("Always on top");
        alwaysOnTop.setSelected(true);
        alwaysOnTop.selectedProperty().addListener((_, _, selected) -> stage.setAlwaysOnTop(selected));
        stage.setAlwaysOnTop(true);

        var toolbar = new HBox(8, copyButton, editButton, topButton, deleteButton, alwaysOnTop);
        toolbar.setPadding(new Insets(10));

        var root = new BorderPane(listView);
        root.setTop(toolbar);
        root.setBottom(status);
        BorderPane.setMargin(status, new Insets(8, 10, 10, 10));

        var scene = new Scene(root, 720, 520);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleShortcut);
        root.setStyle("-fx-font-size: 16px;");

        stage.setScene(scene);
        stage.show();
        startClipboardListener();
    }

    private ListCell<String> createListCell() {
        var cell = new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : preview(item));
                setWrapText(true);
                setStyle("-fx-padding: 10px;");
            }
        };

        var copy = new MenuItem("Copy");
        copy.setOnAction(_ -> {
            selectCell(cell);
            copySelected();
        });
        var edit = new MenuItem("Edit");
        edit.setOnAction(_ -> {
            selectCell(cell);
            editSelected();
        });
        var moveToTop = new MenuItem("Move to top");
        moveToTop.setOnAction(_ -> {
            selectCell(cell);
            moveSelectedToTop();
        });
        var delete = new MenuItem("Delete");
        delete.setOnAction(_ -> {
            selectCell(cell);
            deleteSelected();
        });
        var contextMenu = new ContextMenu(copy, edit, moveToTop, delete);
        contextMenu.setOnShowing(_ -> contextMenu.getItems().forEach(
                item -> item.setDisable(cell.isEmpty())));
        cell.setContextMenu(contextMenu);
        return cell;
    }

    private void selectCell(ListCell<String> cell) {
        if (!cell.isEmpty()) {
            listView.getSelectionModel().select(cell.getIndex());
        }
    }

    private void configureDropTarget() {
        listView.setOnDragOver(event -> {
            if (event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        listView.setOnDragDropped(event -> {
            boolean accepted = event.getDragboard().hasString();
            if (accepted) {
                addToTop(event.getDragboard().getString(), "Dropped text added");
            }
            event.setDropCompleted(accepted);
            event.consume();
        });
    }

    private void handleShortcut(KeyEvent event) {
        if (!event.isShortcutDown()) {
            return;
        }
        if (event.getCode() == KeyCode.V) {
            pasteAsNewItem();
            event.consume();
        } else if (event.getCode() == KeyCode.C) {
            copySelected();
            event.consume();
        } else if (event.getCode() == KeyCode.E) {
            editSelected();
            event.consume();
        }
    }

    private void pasteAsNewItem() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasString()) {
            addToTop(clipboard.getString(), "Pasted text added");
        } else {
            status.setText("The clipboard does not contain text");
        }
    }

    private void copySelected() {
        String selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            status.setText("Select an item to copy");
            return;
        }

        var content = new ClipboardContent();
        content.putString(selected);
        lastClipboardText = selected;
        Clipboard.getSystemClipboard().setContent(content);
        status.setText("Selected item copied");
    }

    private void editSelected() {
        int index = listView.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            status.setText("Select an item to edit");
            return;
        }

        var editor = new TextArea(model.get(index));
        editor.setWrapText(true);
        editor.setPrefColumnCount(72);
        editor.setPrefRowCount(16);

        var dialog = new Dialog<ButtonType>();
        dialog.setTitle("Edit clipboard item");
        dialog.setResizable(true);
        dialog.initOwner(listView.getScene().getWindow());
        dialog.getDialogPane().setContent(editor);
        var save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        dialog.setOnShown(_ -> editor.requestFocus());

        dialog.showAndWait().filter(save::equals).ifPresent(_ -> {
            if (editor.getText().isBlank()) {
                status.setText("Blank text was not saved");
                return;
            }
            int newIndex = model.replace(index, editor.getText());
            refreshAndSelect(newIndex);
            status.setText("Item updated");
        });
    }

    private void moveSelectedToTop() {
        int index = listView.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            status.setText("Select an item to move");
            return;
        }
        model.moveToTop(index);
        refreshAndSelect(0);
        status.setText("Item moved to top");
    }

    private void deleteSelected() {
        int index = listView.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            status.setText("Select an item to delete");
            return;
        }
        model.remove(index);
        refreshAndSelect(Math.min(index, model.size() - 1));
        status.setText("Item deleted");
    }

    private void addToTop(String text, String message) {
        if (text == null || text.isBlank()) {
            status.setText("Blank text was not added");
            return;
        }
        model.addToTop(text);
        refreshAndSelect(0);
        status.setText(message);
    }

    private void refreshAndSelect(int index) {
        visibleItems.setAll(model.snapshot());
        if (index >= 0 && index < visibleItems.size()) {
            listView.getSelectionModel().select(index);
            listView.scrollTo(index);
        }
    }

    private void startClipboardListener() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        lastClipboardText = clipboard.hasString() ? clipboard.getString() : null;
        clipboardPoller = new Timeline(new KeyFrame(Duration.millis(500), _ -> {
            if (!clipboard.hasString()) {
                return;
            }
            String text = clipboard.getString();
            if (text != null && !text.equals(lastClipboardText)) {
                lastClipboardText = text;
                addToTop(text, "New clipboard text added");
            }
        }));
        clipboardPoller.setCycleCount(Timeline.INDEFINITE);
        clipboardPoller.play();
    }

    private String preview(String text) {
        String compact = text.strip().replaceAll("\\s+", " ");
        return compact.length() <= 220 ? compact : compact.substring(0, 217) + "...";
    }

    @Override
    public void stop() {
        if (clipboardPoller != null) {
            clipboardPoller.stop();
        }
    }
}
