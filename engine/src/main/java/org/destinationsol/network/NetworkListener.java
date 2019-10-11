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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;


public class NetworkListener extends Thread {

    private static Logger logger = LoggerFactory.getLogger(NetworkListener.class);
    private ArrayList<ClientCommunicator> clients = new ArrayList<>();

    @Override
    public void run() {

        try {
            ServerSocket serverSocket = new ServerSocket(8888);
            serverSocket.setSoTimeout(1);
            while (!Thread.interrupted()) {
                try {
                    ClientCommunicator client = new ClientCommunicator(serverSocket.accept());
                    client.start();
                    clients.add(client);
                } catch (SocketTimeoutException e) {
                    continue;
                }
            }
            serverSocket.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
        clients.forEach(ClientCommunicator::interrupt);
    }
}
