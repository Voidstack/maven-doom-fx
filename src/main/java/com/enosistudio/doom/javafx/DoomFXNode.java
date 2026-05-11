package com.enosistudio.doom.javafx;

import com.enosistudio.doom.doom.event_t;
import com.enosistudio.doom.doom.evtype_t;
import com.enosistudio.doom.g.Signals;
import com.enosistudio.doom.g.Signals.ScanCode;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.robot.Robot;
import java.util.function.Consumer;

/**
 * JavaFX node that renders Doom and forwards input to the engine.
 * Click to capture mouse; Escape to release.
 */
public class DoomFXNode extends Pane {

    private final ImageView imageView = new ImageView();
    private final Consumer<? super event_t> eventSink;
    private final Robot robot = new Robot();

    /**
     * Single shared mouse event, accumulated between Doom ticks exactly like the AWT version.
     * Doom overwrites mousex on every ev_mouse it processes (not +=), so flooding the queue
     * with many small events causes only the last tiny delta to count. Instead, we keep one
     * event in the queue and accumulate deltas into it until Doom signals it was consumed
     * via processedNotify() (processed flag goes back to true).
     */
    private final event_t.mouseevent_t sharedMouseEvent =
        new event_t.mouseevent_t(evtype_t.ev_mouse, 0, 0, 0);

    private boolean captured = false;
    private int mouseButtons = 0;

    public DoomFXNode(Consumer<? super event_t> eventSink, int width, int height) {
        this.eventSink = eventSink;

        imageView.setPreserveRatio(false);
        imageView.fitWidthProperty().bind(widthProperty());
        imageView.fitHeightProperty().bind(heightProperty());
        setPrefSize(width, height);
        setFocusTraversable(true);
        getChildren().add(imageView);

        setOnKeyPressed(this::onKeyPressed);
        setOnKeyReleased(this::onKeyReleased);
        setOnMouseMoved(this::onMouseMoved);
        setOnMouseDragged(this::onMouseMoved);
        setOnMousePressed(this::onMousePressed);
        setOnMouseReleased(this::onMouseReleased);
    }

    /** Must be called on the JavaFX thread (via Platform.runLater). */
    public void setFxImage(WritableImage image) {
        imageView.setImage(image);
    }

    private void capture() {
        captured = true;
        setCursor(Cursor.NONE);
        requestFocus();
        recenter();
    }

    private void release() {
        captured = false;
        mouseButtons = 0;
        setCursor(Cursor.DEFAULT);
    }

    private void recenter() {
        Bounds bounds = localToScreen(getBoundsInLocal());
        if (bounds == null) return;
        robot.mouseMove(
            bounds.getMinX() + bounds.getWidth()  / 2,
            bounds.getMinY() + bounds.getHeight() / 2
        );
    }

    private void onKeyPressed(KeyEvent e) {
        if (e.getCode() == KeyCode.ESCAPE && captured) {
            release();
        }
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

    private static final int RECENTER_THRESHOLD = 2;

    private void onMouseMoved(MouseEvent e) {
        if (!captured) return;

        double centerX = getWidth()  / 2;
        double centerY = getHeight() / 2;
        int dx = (int) (e.getX() - centerX);
        int dy = (int) (e.getY() - centerY);

        if (Math.abs(dx) <= RECENTER_THRESHOLD && Math.abs(dy) <= RECENTER_THRESHOLD) return;

        sharedMouseEvent.buttons = mouseButtons;
        if (sharedMouseEvent.processed) {
            // Doom consumed the last event: start a fresh one and queue it
            sharedMouseEvent.x = dx << 2;
            sharedMouseEvent.y = -dy << 2;
            sharedMouseEvent.resetNotify();       // processed = false
            eventSink.accept(sharedMouseEvent);   // one entry in the queue
        } else {
            // Still in the queue: accumulate so Doom reads the total delta
            sharedMouseEvent.x += dx << 2;
            sharedMouseEvent.y += -dy << 2;
        }

        recenter();
    }

    private void onMousePressed(MouseEvent e) {
        if (!captured) {
            capture();
            return;
        }
        mouseButtons |= buttonMask(e.getButton());
        eventSink.accept(new event_t.mouseevent_t(evtype_t.ev_mouse, mouseButtons, 0, 0));
    }

    private void onMouseReleased(MouseEvent e) {
        if (!captured) return;
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
