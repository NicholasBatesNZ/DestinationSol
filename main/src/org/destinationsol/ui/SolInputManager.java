/*
 * Copyright 2017 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.destinationsol.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import org.destinationsol.Const;
import org.destinationsol.GameOptions;
import org.destinationsol.SolApplication;
import org.destinationsol.TextureManager;
import org.destinationsol.assets.audio.PlayableSound;
import org.destinationsol.common.SolColor;
import org.destinationsol.common.SolMath;
import org.destinationsol.game.SolGame;
import org.destinationsol.game.sound.OggSoundManager;

import java.util.ArrayList;
import java.util.List;

import static com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888;

public class SolInputManager {
    private static final float CURSOR_SZ = .07f;
    private static final float WARN_PERC_GROWTH_TIME = 1f;
    private static final int POINTER_COUNT = 4;
    private static final float CURSOR_SHOW_TIME = 3;
    private static final float initialRatio = ((float) Gdx.graphics.getWidth()) / ((float) Gdx.graphics.getHeight());

    private static Cursor hiddenCursor;

    private final List<SolUiScreen> screens = new ArrayList<>();
    private final List<SolUiScreen> screenToRemove = new ArrayList<>();
    private final List<SolUiScreen> screensToAdd = new ArrayList<>();
    private final Pointer[] pointers;
    private final Pointer flashPointer;
    private final Vector2 mousePos;
    private final Vector2 mousePrevPos;
    private final PlayableSound hoverSound;
    private final TextureAtlas.AtlasRegion uiCursor;
    private final Color warnColor;
    private float mouseIdleTime;
    private TextureAtlas.AtlasRegion currCursor;
    private boolean mouseOnUi;
    private float warnPerc;
    private boolean warnPercGrows;
    private Boolean scrolledUp;

    public SolInputManager(TextureManager textureManager, OggSoundManager soundManager) {
        pointers = new Pointer[POINTER_COUNT];
        for (int i = 0; i < POINTER_COUNT; i++) {
            pointers[i] = new Pointer();
        }
        SolInputProcessor sip = new SolInputProcessor(this);
        Gdx.input.setInputProcessor(sip);
        flashPointer = new Pointer();
        mousePos = new Vector2();
        mousePrevPos = new Vector2();

        // Create an empty 1x1 pixmap to use as hidden cursor
        Pixmap pixmap = new Pixmap(1, 1, RGBA8888);
        hiddenCursor = Gdx.graphics.newCursor(pixmap, 0, 0);
        pixmap.dispose();

        // We want the original mouse cursor to be hidden as we draw our own mouse cursor.
        Gdx.input.setCursorCatched(false);
        setMouseCursorHidden();
        uiCursor = textureManager.getTexture("ui/cursor");
        warnColor = new Color(SolColor.UI_WARN);

        hoverSound = soundManager.getSound("Core:uiHover");
    }

    private static void setPointerPosition(Pointer pointer, int screenX, int screenY) {
        int h = Gdx.graphics.getHeight();
        float currentRatio = ((float) Gdx.graphics.getWidth()) / ((float) Gdx.graphics.getHeight());

        pointer.x = 1f * screenX / h * (initialRatio / currentRatio);
        pointer.y = 1f * screenY / h;
    }

    /**
     * Hides the mouse cursor by setting it to a transparent image.
     */
    private void setMouseCursorHidden() {
        Gdx.graphics.setCursor(hiddenCursor);
    }

    void maybeFlashPressed(int keyCode) {
        for (SolUiScreen screen : screens) {
            boolean consumed = false;
            List<SolUiControl> controls = screen.getControls();
            for (SolUiControl control : controls) {
                if (control.maybeFlashPressed(keyCode)) {
                    consumed = true;
                }
            }
            if (consumed) {
                return;
            }
        }

    }

    void maybeFlashPressed(int x, int y) {
        setPointerPosition(flashPointer, x, y);
        for (SolUiScreen screen : screens) {
            List<SolUiControl> controls = screen.getControls();
            for (SolUiControl control : controls) {
                if (control.maybeFlashPressed(flashPointer)) {
                    return;
                }
            }
            if (screen.isCursorOnBg(flashPointer)) {
                return;
            }
        }

    }

    public void setScreen(SolApplication solApplication, SolUiScreen screen) {
        for (SolUiScreen oldScreen : screens) {
            removeScreen(oldScreen, solApplication);
        }
        addScreen(solApplication, screen);
    }

    public void addScreen(SolApplication solApplication, SolUiScreen screen) {
        screensToAdd.add(screen);
        screen.onAdd(solApplication);
    }

    private void removeScreen(SolUiScreen screen, SolApplication solApplication) {
        screenToRemove.add(screen);
        List<SolUiControl> controls = screen.getControls();
        for (SolUiControl control : controls) {
            control.blur();
        }
        screen.blurCustom(solApplication);
    }

    public boolean isScreenOn(SolUiScreen screen) {
        return screens.contains(screen);
    }

    public void update(SolApplication solApplication) {
        boolean mobile = solApplication.isMobile();
        SolGame game = solApplication.getGame();

        // This keeps the mouse within the window, but only when playing the game with the mouse.
        // All other times the mouse can freely leave and return.
        if (!mobile && (solApplication.getOptions().controlType == GameOptions.CONTROL_MIXED || solApplication.getOptions().controlType == GameOptions.CONTROL_MOUSE) &&
            game != null && getTopScreen() != game.getScreens().menuScreen) {
            if (!Gdx.input.isCursorCatched()) {
                Gdx.input.setCursorCatched(true);
            }
            maybeFixMousePos();
        } else {
            if (Gdx.input.isCursorCatched()) {
                Gdx.input.setCursorCatched(false);
            }
        }

        updatePointers();

        boolean consumed = false;
        mouseOnUi = false;
        boolean clickOutsideReacted = false;
        for (SolUiScreen screen : screens) {
            boolean consumedNow = false;
            List<SolUiControl> controls = screen.getControls();
            for (SolUiControl control : controls) {
                control.update(pointers, currCursor != null, !consumed, this, solApplication);
                if (control.isOn() || control.isJustOff()) {
                    consumedNow = true;
                }
                Rectangle area = control.getScreenArea();
                if (area != null && area.contains(mousePos)) {
                    mouseOnUi = true;
                }
            }
            if (consumedNow) {
                consumed = true;
            }
            boolean clickedOutside = false;
            if (!consumed) {
                for (Pointer pointer : pointers) {
                    boolean onBg = screen.isCursorOnBg(pointer);
                    if (pointer.pressed && onBg) {
                        clickedOutside = false;
                        consumed = true;
                        break;
                    }
                    if (!onBg && pointer.isJustUnPressed() && !clickOutsideReacted) {
                        clickedOutside = true;
                    }
                }
            }
            if (clickedOutside && screen.reactsToClickOutside()) {
                clickOutsideReacted = true;
            }
            if (screen.isCursorOnBg(pointers[0])) {
                mouseOnUi = true;
            }
            screen.updateCustom(solApplication, pointers, clickedOutside);
        }

        TutorialManager tutorialManager = game == null ? null : game.getTutMan();
        if (tutorialManager != null && tutorialManager.isFinished()) {
            solApplication.finishGame();
        }

        updateCursor(solApplication);
        addRemoveScreens();
        updateWarnPerc();
        scrolledUp = null;
    }

    private void updateWarnPerc() {
        float dif = SolMath.toInt(warnPercGrows) * Const.REAL_TIME_STEP / WARN_PERC_GROWTH_TIME;
        warnPerc += dif;
        if (warnPerc < 0 || 1 < warnPerc) {
            warnPerc = SolMath.clamp(warnPerc);
            warnPercGrows = !warnPercGrows;
        }
        warnColor.a = warnPerc * .5f;
    }

    private void addRemoveScreens() {
        for (SolUiScreen screen : screenToRemove) {
            screens.remove(screen);
        }
        screenToRemove.clear();

        for (SolUiScreen screen : screensToAdd) {
            if (isScreenOn(screen)) {
                continue;
            }
            screens.add(0, screen);
        }
        screensToAdd.clear();
    }

    private void updateCursor(SolApplication solApplication) {
        if (solApplication.isMobile()) {
            return;
        }
        SolGame game = solApplication.getGame();

        mousePos.set(pointers[0].x, pointers[0].y);
        if (solApplication.getOptions().controlType == GameOptions.CONTROL_MIXED || solApplication.getOptions().controlType == GameOptions.CONTROL_MOUSE) {
            if (game == null || mouseOnUi) {
                currCursor = uiCursor;
            } else {
                currCursor = game.getScreens().mainScreen.shipControl.getInGameTex();
                if (currCursor == null) {
                    currCursor = uiCursor;
                }
            }
            return;
        }
        if (mousePrevPos.epsilonEquals(mousePos, 0) && game != null && getTopScreen() != game.getScreens().menuScreen) {
            mouseIdleTime += Const.REAL_TIME_STEP;
            currCursor = mouseIdleTime < CURSOR_SHOW_TIME ? uiCursor : null;
        } else {
            currCursor = uiCursor;
            mouseIdleTime = 0;
            mousePrevPos.set(mousePos);
        }
    }

    private void maybeFixMousePos() {
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.input.getY();
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        mouseX = (int) SolMath.clamp(mouseX, 0, w);
        mouseY = (int) SolMath.clamp(mouseY, 0, h);
        Gdx.input.setCursorPosition(mouseX, mouseY);
    }

    private void updatePointers() {
        for (int i = 0; i < POINTER_COUNT; i++) {
            Pointer pointer = pointers[i];
            int screenX = Gdx.input.getX(i);
            int screenY = Gdx.input.getY(i);
            setPointerPosition(pointer, screenX, screenY);
            pointer.prevPressed = pointer.pressed;
            pointer.pressed = Gdx.input.isTouched(i);
        }
    }

    public void draw(UiDrawer uiDrawer, SolApplication solApplication) {
        for (int i = screens.size() - 1; i >= 0; i--) {
            SolUiScreen screen = screens.get(i);

            uiDrawer.setTextMode(false);
            screen.drawBg(uiDrawer, solApplication);
            List<SolUiControl> controls = screen.getControls();
            for (SolUiControl control : controls) {
                control.drawButton(uiDrawer, solApplication, warnColor);
            }
            screen.drawImgs(uiDrawer, solApplication);

            uiDrawer.setTextMode(true);
            screen.drawText(uiDrawer, solApplication);
            for (SolUiControl control : controls) {
                control.drawDisplayName(uiDrawer);
            }
        }
        uiDrawer.setTextMode(null);

        SolGame game = solApplication.getGame();
        TutorialManager tutorialManager = game == null ? null : game.getTutMan();
        if (tutorialManager != null && getTopScreen() != game.getScreens().menuScreen) {
            tutorialManager.draw(uiDrawer);
        }

        if (currCursor != null) {
            uiDrawer.draw(currCursor, CURSOR_SZ, CURSOR_SZ, CURSOR_SZ / 2, CURSOR_SZ / 2, mousePos.x, mousePos.y, 0, SolColor.W);
        }
    }

    public Vector2 getMousePos() {
        return mousePos;
    }

    public Pointer[] getPtrs() {
        return pointers;
    }

    public boolean isMouseOnUi() {
        return mouseOnUi;
    }

    public void playHover(SolApplication solApplication) {
        hoverSound.getOggSound().getSound().play(.7f * solApplication.getOptions().sfxVolumeMultiplier, .7f, 0);
    }

    public void playClick(SolApplication solApplication) {
        hoverSound.getOggSound().getSound().play(.7f * solApplication.getOptions().sfxVolumeMultiplier, .9f, 0);
    }

    public SolUiScreen getTopScreen() {
        return screens.isEmpty() ? null : screens.get(0);
    }

    public void scrolled(boolean up) {
        scrolledUp = up;
    }

    public Boolean getScrolledUp() {
        return scrolledUp;
    }

    public void dispose() {
        hoverSound.getOggSound().getSound().dispose();
    }

    public static class Pointer {
        public float x;
        public float y;
        public boolean pressed;
        public boolean prevPressed;

        public boolean isJustPressed() {
            return pressed && !prevPressed;
        }

        public boolean isJustUnPressed() {
            return !pressed && prevPressed;
        }
    }

}
