package com.enosistudio.doom.javafx;

import com.enosistudio.doom.doom.event_t;
import com.enosistudio.doom.doom.evtype_t;
import com.enosistudio.doom.g.Signals;
import com.enosistudio.doom.g.Signals.ScanCode;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import java.util.function.Consumer;

/**
 * JavaFX node that renders Doom and forwards input to the engine.
 * Embed this in any JavaFX scene: myPane.getChildren().add(doomFXNode).
 */
public class DoomFXNode extends Pane {

    private final ImageView imageView = new ImageView();
    private final Consumer<? super event_t> eventSink;

    private double lastMouseX = -1;
    private double lastMouseY = -1;
    private int mouseButtons = 0;

    public DoomFXNode(Consumer<? super event_t> eventSink, int width, int height) {
        this.eventSink = eventSink;

        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        imageView.setPreserveRatio(false);
        setPrefSize(width, height);
        setFocusTraversable(true);
        getChildren().add(imageView);

        setOnKeyPressed(this::onKeyPressed);
        setOnKeyReleased(this::onKeyReleased);
        setOnMouseMoved(this::onMouseMoved);
        setOnMouseDragged(this::onMouseMoved);
        setOnMousePressed(this::onMousePressed);
        setOnMouseReleased(this::onMouseReleased);
        setOnMouseEntered(e -> { requestFocus(); lastMouseX = -1; });
    }

    /** Must be called on the JavaFX thread (via Platform.runLater). */
    public void setFxImage(WritableImage image) {
        imageView.setImage(image);
    }

    private void onKeyPressed(KeyEvent e) {
        ScanCode sc = Signals.getScanCode(e.getCode().getCode());
        if (sc != ScanCode.SC_NULL) {
            eventSink.accept(sc.doomEventDown);
        }
        e.consume();
    }

    private void onKeyReleased(KeyEvent e) {
        ScanCode sc = Signals.getScanCode(e.getCode().getCode());
        if (sc != ScanCode.SC_NULL) {
            eventSink.accept(sc.doomEventUp);
        }
        e.consume();
    }

    private void onMouseMoved(MouseEvent e) {
        if (lastMouseX < 0) {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            return;
        }
        int dx = (int) (e.getX() - lastMouseX);
        int dy = (int) (e.getY() - lastMouseY);
        lastMouseX = e.getX();
        lastMouseY = e.getY();
        if (dx != 0 || dy != 0) {
            eventSink.accept(new event_t.mouseevent_t(evtype_t.ev_mouse, mouseButtons, dx, -dy));
        }
    }

    private void onMousePressed(MouseEvent e) {
        requestFocus();
        mouseButtons |= buttonMask(e.getButton());
        eventSink.accept(new event_t.mouseevent_t(evtype_t.ev_mouse, mouseButtons, 0, 0));
    }

    private void onMouseReleased(MouseEvent e) {
        mouseButtons &= ~buttonMask(e.getButton());
        eventSink.accept(new event_t.mouseevent_t(evtype_t.ev_mouse, mouseButtons, 0, 0));
    }

    private static int buttonMask(MouseButton b) {
        return switch (b) {
            case PRIMARY   -> 1;
            case SECONDARY -> 2;
            case MIDDLE    -> 4;
            default        -> 0;
        };
    }
}
