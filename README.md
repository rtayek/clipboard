# Clipboard List

A small, persistent JavaFX window for holding several pieces of text while
working among browser chats, terminals, and other applications.

## Current behavior

- Watches the system clipboard and places newly copied text at the top.
- Accepts text pasted with Ctrl+V or dropped onto the list.
- Copies the selected item with Ctrl+C or the Copy button.
- Edits complete item text with Ctrl+E, the Edit button, or the context menu.
- Moves items to the top and deletes items.
- Stays on top by default, with a checkbox to turn that behavior off.

Repeated copies of identical text move the existing item to the top instead of
creating duplicates. List rows show shortened previews; copying and editing
always use the complete text.

## Build and run

```sh
./gradlew test
./gradlew run
```

The project uses Java 25, the Gradle wrapper, JavaFX 25, and JUnit 5. Main
sources are under `src/`; tests are under `tst/`. Eclipse metadata is
generated with `./gradlew eclipse` and is not tracked.
