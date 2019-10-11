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
package org.destinationsol.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerCommunicator extends Thread {

    private static Logger logger = LoggerFactory.getLogger(ServerCommunicator.class);

    @Override
    public void run() {

        try {
            Socket socket = new Socket("localhost", 8888);
            BufferedReader incoming = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter outgoing = new PrintWriter(socket.getOutputStream(), true);
            outgoing.println("hello from client!");
            String message;
            while (!Thread.interrupted() && (message = incoming.readLine()) != null) {
                logger.info(message);
            }
            socket.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
