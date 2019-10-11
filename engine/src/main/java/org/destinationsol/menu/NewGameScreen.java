/*
 * Copyright 2018 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.destinationsol.menu;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Timer;
import org.destinationsol.GameOptions;
import org.destinationsol.SolApplication;
import org.destinationsol.assets.Assets;
import org.destinationsol.common.SolColor;
import org.destinationsol.game.SaveManager;
import org.destinationsol.network.ServerCommunicator;
import org.destinationsol.ui.DisplayDimensions;
import org.destinationsol.ui.SolInputManager;
import org.destinationsol.ui.SolUiBaseScreen;
import org.destinationsol.ui.SolUiControl;
import org.destinationsol.ui.UiDrawer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewGameScreen extends SolUiBaseScreen {
    private DisplayDimensions displayDimensions;

    private final TextureAtlas.AtlasRegion backgroundTexture;

    private final SolUiControl backControl;
    private final SolUiControl continueControl;
    private final SolUiControl joinControl;
    private final SolUiControl newControl;

    private static Logger logger = LoggerFactory.getLogger(NewGameScreen.class);

    NewGameScreen(MenuLayout menuLayout, GameOptions gameOptions) {
        displayDimensions = SolApplication.displayDimensions;

        continueControl = new SolUiControl(menuLayout.buttonRect(-1, 1), true, gameOptions.getKeyShoot());
        continueControl.setDisplayName("Continue");
        controls.add(continueControl);

        newControl = new SolUiControl(menuLayout.buttonRect(-1, 2), true);
        newControl.setDisplayName("New game");
        controls.add(newControl);

        joinControl = new SolUiControl(menuLayout.buttonRect(-1, 3), true);
        joinControl.setDisplayName("Join local game");
        controls.add(joinControl);

        backControl = new SolUiControl(menuLayout.buttonRect(-1, 4), true, gameOptions.getKeyEscape());
        backControl.setDisplayName("Cancel");
        controls.add(backControl);

        backgroundTexture = Assets.getAtlasRegion("engine:mainMenuBg", Texture.TextureFilter.Linear);
    }

    @Override
    public void onAdd(SolApplication solApplication) {
        continueControl.setEnabled(SaveManager.hasPrevShip("prevShip.ini"));
    }

    @Override
    public void updateCustom(SolApplication solApplication, SolInputManager.InputPointer[] inputPointers, boolean clickedOutside) {
        MenuScreens screens = solApplication.getMenuScreens();
        SolInputManager im = solApplication.getInputManager();
        ServerCommunicator serverCommunicator = new ServerCommunicator();
        if (backControl.isJustOff()) {
            im.setScreen(solApplication, screens.main);
            return;
        }
        if (continueControl.isJustOff()) {
            solApplication.loadGame(false, null, false);
            return;
        }
        if (joinControl.isJustOff()) {
            serverCommunicator.start();
            serverCommunicator.interrupt();
        }
        if (newControl.isJustOff()) {
            im.setScreen(solApplication, screens.newShip);
        }
    }

    @Override
    public boolean isCursorOnBackground(SolInputManager.InputPointer inputPointer) {
        return true;
    }

    @Override
    public void drawBackground(UiDrawer uiDrawer, SolApplication solApplication) {
        uiDrawer.draw(backgroundTexture, displayDimensions.getRatio(), 1, displayDimensions.getRatio() / 2, 0.5f, displayDimensions.getRatio() / 2, 0.5f, 0, SolColor.WHITE);
    }
}
