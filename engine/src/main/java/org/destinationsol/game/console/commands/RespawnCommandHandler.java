/*
 * Copyright 2019 MovingBlocks
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
package org.destinationsol.game.console.commands;

import org.destinationsol.game.Hero;
import org.destinationsol.game.SolGame;
import org.destinationsol.game.console.Message;
import org.destinationsol.game.console.annotations.Command;
import org.destinationsol.game.console.annotations.Game;
import org.destinationsol.game.console.annotations.RegisterCommands;

/**
 * A command used to respawn player's ship
 */
@RegisterCommands
public class RespawnCommandHandler {

    @Command(shortDescription = "Respawns player if dead")
    public Message respawn(@Game SolGame game) {
        Hero hero = game.getHero();
        if (hero.isAlive()) {
            return Message.FAILURE("Cannot respawn hero when not dead!");
        }
        game.respawn();
        return Message.SUCCESS("Hero respawned!");
    }
}
