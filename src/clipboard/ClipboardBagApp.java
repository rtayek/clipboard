package clipboard;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;

public class ClipboardBagApp extends Application {

    // Keep track of the item currently being dragged
    private static TreeItem<String> draggedItem;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Clipboard Holding Bags");

        // 🎒 Bag A (Inbox Container)
        var bagARoot = new TreeItem<>("🎒 Holding Bag A (Inbox)");
        bagARoot.setExpanded(true);
        var treeBagA = new TreeView<>(bagARoot);
        setupDragAndDrop(treeBagA);

        // 🎒 Bag B (Organizer Container)
        var bagBRoot = new TreeItem<>("🎒 Holding Bag B (Organizer)");
        bagBRoot.setExpanded(true);
        var treeBagB = new TreeView<>(bagBRoot);
        setupDragAndDrop(treeBagB);

        // Start listening to the operating system's clipboard in a separate thread
        startClipboardListener(bagARoot);

        // Layout layout container: Place them side-by-side
        var splitPane = new SplitPane(treeBagA, treeBagB);
        splitPane.setDividerPositions(0.5);

        var scene = new Scene(new StackPane(splitPane), 800, 500);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(_ -> System.exit(0)); // Clean up the listening thread on close
        primaryStage.show();
    }

    /**
     * Implements full drag-and-drop mechanics for a TreeView
     */
    private void setupDragAndDrop(TreeView<String> treeView) {
        treeView.setCellFactory(_ -> {
            TreeCell<String> cell = new TreeCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                }
            };

            // 1. Detect when a user starts dragging a snippet
            cell.setOnDragDetected(event -> {
                TreeItem<String> item = cell.getTreeItem();
                // Prevent dragging the root "Bag" folders
                if (item != null && item.getParent() != null) {
                    draggedItem = item;
                    var db = cell.startDragAndDrop(TransferMode.MOVE);
                    var content = new ClipboardContent();
                    content.putString(item.getValue());
                    db.setContent(content);
                    event.consume();
                }
            });

            // 2. Allow the item to slide over other items/folders
            cell.setOnDragOver(event -> {
                if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }
                event.consume();
            });

            // 3. Handle dropping the snippet into a tree or another node
            cell.setOnDragDropped(event -> {
                var db = event.getDragboard();
                boolean success = false;

                if (db.hasString() && draggedItem != null) {
                    TreeItem<String> targetItem = cell.getTreeItem();
                    
                    // If dropped on empty space or root bag, attach to that tree's root
                    if (targetItem == null) {
                        targetItem = cell.getTreeView().getRoot();
                    } else if (targetItem.getParent() != null) {
                        // Dropped directly onto another snippet? Nest it inside that snippet's parent folder
                        targetItem = targetItem.getParent();
                    }

                    // Remove snippet from its old bag, add it to the target bag
                    draggedItem.getParent().getChildren().remove(draggedItem);
                    targetItem.getChildren().add(draggedItem);
                    cell.getTreeView().getSelectionModel().select(draggedItem);
                    
                    success = true;
                    draggedItem = null;
                }
                event.setDropCompleted(success);
                event.consume();
            });

            return cell;
        });
    }

    /**
     * Background daemon thread monitoring the system clipboard
     */
    private void startClipboardListener(TreeItem<String> inboxRoot) {
        Thread listenerThread = new Thread(() -> {
            try {
                Clipboard sysClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                String lastCopiedText = "";

                while (!Thread.currentThread().isInterrupted()) {
                    // Check if clipboard has readable text string
                    if (sysClipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                        String currentText = (String) sysClipboard.getData(DataFlavor.stringFlavor);
                        
                        // If it's a completely new copy operation
                        if (currentText != null && !currentText.equals(lastCopiedText)) {
                            lastCopiedText = currentText;
                            
                            // Format a punchy, clean preview of just the first few words
                            String preview = currentText.strip().lines().findFirst().orElse("");
                            if (preview.length() > 60) {
                                preview = preview.substring(0, 57) + "...";
                            }
                            
                            final String snippetText = preview;
                            
                            // Push the update safely to the JavaFX Application thread
                            Platform.runLater(() -> inboxRoot.getChildren().add(new TreeItem<>(snippetText)));
                        }
                    }
                    Thread.sleep(500); // Poll the clipboard twice a second
                }
            } catch (Exception e) {
                // Fail silently or handle background context errors
            }
        });
        
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

