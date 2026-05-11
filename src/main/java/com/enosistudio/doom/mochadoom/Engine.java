/*
 * Copyright (C) 2017 Good Sign
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.enosistudio.doom.mochadoom;

import com.enosistudio.doom.awt.DoomWindow;
import com.enosistudio.doom.awt.DoomWindowController;
import com.enosistudio.doom.awt.EventBase.KeyStateInterest;
import static com.enosistudio.doom.awt.EventBase.KeyStateSatisfaction.*;
import com.enosistudio.doom.awt.EventHandler;
import com.enosistudio.doom.doom.CVarManager;
import com.enosistudio.doom.doom.CommandVariable;
import com.enosistudio.doom.doom.ConfigManager;
import com.enosistudio.doom.doom.DoomMain;
import com.enosistudio.doom.javafx.DoomFXNode;
import com.enosistudio.doom.javafx.DoomFXWindowController;
import static com.enosistudio.doom.g.Signals.ScanCode.*;
import com.enosistudio.doom.i.Strings;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Engine {
    private static volatile Engine instance;
    
    /**
     * Mocha Doom engine entry point
     */
    public static void main(final String[] argv) throws IOException {
        final Engine local;
        synchronized (Engine.class) {
            local = new Engine(argv);
        }
        
        /**
         * Add eventHandler listeners to JFrame and its Canvas elememt
         */
        /*content.addKeyListener(listener);        
        content.addMouseListener(listener);
        content.addMouseMotionListener(listener);
        frame.addComponentListener(listener);
        frame.addWindowFocusListener(listener);
        frame.addWindowListener(listener);*/
        // never returns
        try {
            local.DOOM.setupLoop();
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }  
    
    public final CVarManager cvm;
    public final ConfigManager cm;
    public final DoomWindowController<?, EventHandler> windowController;
    private final DoomFXWindowController fxWindowController;
    private final DoomMain<?, ?> DOOM;

    /**
     * Starts the engine in JavaFX mode.
     * Run this before showing your JavaFX stage; returns the node to embed in your scene.
     * The game loop runs in a daemon thread.
     */
    public static DoomFXNode startJavaFX(final String wadPath) throws IOException {
        return startJavaFX("-iwad", wadPath);
    }

    public static DoomFXNode startJavaFX(final String... argv) throws IOException {
        final Engine eng;
        synchronized (Engine.class) {
            eng = new Engine(true, argv);
        }
        Thread gameThread = new Thread(() -> {
            try {
                eng.DOOM.setupLoop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "doom-loop");
        gameThread.setDaemon(true);
        gameThread.start();
        return eng.fxWindowController.getNode();
    }

    @SuppressWarnings("unchecked")
    private Engine(final String... argv) throws IOException {
        this(false, argv);
    }

    @SuppressWarnings("unchecked")
    private Engine(final boolean fxMode, final String... argv) throws IOException {
        instance = this;
        this.cvm = new CVarManager(Arrays.asList(argv));
        this.cm = new ConfigManager();
        this.DOOM = new DoomMain<>();

        if (fxMode) {
            this.fxWindowController = new DoomFXWindowController(
                DOOM.graphicSystem::getScreenImage,
                DOOM::PostEvent,
                DOOM.graphicSystem.getScreenWidth(),
                DOOM.graphicSystem.getScreenHeight()
            );
            this.windowController = null;
            return;
        }

        this.fxWindowController = null;
        this.windowController = DoomWindow.createCanvasWindowController(
                DOOM.graphicSystem::getScreenImage,
                DOOM::PostEvent,
                DOOM.graphicSystem.getScreenWidth(),
                DOOM.graphicSystem.getScreenHeight()
            );

        windowController.getObserver().addInterest(
            new KeyStateInterest<>(obs -> {
                EventHandler.fullscreenChanges(windowController.getObserver(), windowController.switchFullscreen());
                return WANTS_MORE_ATE;
            }, SC_LALT, SC_ENTER)
        ).addInterest(
            new KeyStateInterest<>(obs -> {
                if (!windowController.isFullscreen()) {
                    if (DOOM.menuactive || DOOM.paused || DOOM.demoplayback) {
                        EventHandler.menuCaptureChanges(obs, DOOM.mousecaptured = !DOOM.mousecaptured);
                    } else { // can also work when not DOOM.mousecaptured
                        EventHandler.menuCaptureChanges(obs, DOOM.mousecaptured = true);
                    }
                }
                return WANTS_MORE_PASS;
            }, SC_LALT)
        ).addInterest(
            new KeyStateInterest<>(obs -> {
                if (!windowController.isFullscreen() && !DOOM.mousecaptured && DOOM.menuactive) {
                    EventHandler.menuCaptureChanges(obs, DOOM.mousecaptured = true);
                }
                
                return WANTS_MORE_PASS;
            }, SC_ESCAPE)
        ).addInterest(
            new KeyStateInterest<>(obs -> {
                if (!windowController.isFullscreen() && !DOOM.mousecaptured && DOOM.paused) {
                    EventHandler.menuCaptureChanges(obs, DOOM.mousecaptured = true);
                }
                return WANTS_MORE_PASS;
            }, SC_PAUSE)
        );
    }
    
    public static void updateFrame() {
        Engine eng = instance;
        if (eng == null) return;
        if (eng.fxWindowController != null) {
            eng.fxWindowController.updateFrame();
        } else if (eng.windowController != null) {
            eng.windowController.updateFrame();
        }
    }
        
    public String getWindowTitle(double frames) {
        if (cvm.bool(CommandVariable.SHOWFPS)) {
            return String.format("%s - %s FPS: %.2f", Strings.MOCHA_DOOM_TITLE, DOOM.bppMode, frames);
        } else {
            return String.format("%s - %s", Strings.MOCHA_DOOM_TITLE, DOOM.bppMode);
        }
    }

    public static Engine getEngine() {
        Engine local = Engine.instance;
        if (local == null) {
            synchronized (Engine.class) {
                local = Engine.instance;
                if (local == null) {
                    try {
                        Engine.instance = local = new Engine();
                    } catch (IOException ex) {
                        Logger.getLogger(Engine.class.getName()).log(Level.SEVERE, null, ex);
                        throw new Error("This launch is DOOMed");
                    }
                }
            }
        }
        
        return local;
    }
    
    public static CVarManager getCVM() {
        return getEngine().cvm;
    }
    
    public static ConfigManager getConfig() {
        return getEngine().cm;
    }
}
