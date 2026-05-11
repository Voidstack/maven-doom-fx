package com.enosistudio.doom.javafx;

import com.enosistudio.doom.doom.event_t;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;

/**
 * JavaFX counterpart of DoomWindowController.
 * Owns the DoomFXNode and bridges the game's image supplier to JavaFX rendering.
 */
public class DoomFXWindowController {

    private final DoomFXNode node;
    private final Supplier<? extends Image> imageSupplier;

    public DoomFXWindowController(
        Supplier<Image> imageSupplier,
        Consumer<? super event_t> eventConsumer,
        int width,
        int height
    ) {
        this.imageSupplier = imageSupplier;
        this.node = new DoomFXNode(eventConsumer, width, height);
    }

    /** Returns the node to embed in a JavaFX scene. */
    public DoomFXNode getNode() {
        return node;
    }

    private WritableImage fxImage;

    /** Called each frame by the game loop (not on the FX thread). */
    public void updateFrame() {
        Image img = imageSupplier.get();
        if (!(img instanceof BufferedImage bi)) return;
        // Copy pixels here, on the game loop thread, before the next frame overwrites the buffer.
        fxImage = SwingFXUtils.toFXImage(bi, fxImage);
        Platform.runLater(() -> node.setFxImage(fxImage));
    }
}
