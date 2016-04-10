/*
 * Copyright (C) 2016 andylizi
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
package net.andylizi.gobang;

import java.io.IOException;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/status")
public class StatusEndPoint extends Endpoint {

    private final Set<Session> sessions = new CopyOnWriteArraySet<>();

    {
        new Timer(true).scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!sessions.isEmpty()) {
                    StringBuilder builder = new StringBuilder();
                    builder.append('{');
                    if (!GameStorage.rooms.isEmpty()) {
                        Room[] rooms = GameStorage.rooms.values().toArray(new Room[0]);
                        for (int i = 0; i < rooms.length; i++) {
                            Room room = rooms[i];
                            builder.append('\"').append(room.getRoomId()).append("\":{");
                            builder.append("\"playing\":").append(room.isPlaying()).append(',');
                            builder.append("\"rounds\":").append(room.getRounds()).append(',');
                            builder.append("\"steps\":").append(room.getSteps()).append(',');
                            builder.append("\"watchers\":").append(room.getSpectators().size());
                            builder.append('}');
                            if (i != rooms.length - 1) {
                                builder.append(',');
                            }
                        }
                    }
                    builder.append('}');
                    String json = builder.toString();
                    for (Session session : sessions) {
                        if (!session.isOpen()) {
                            sessions.remove(session);
                        }
                        try {
                            session.getBasicRemote().sendText(json, true);
                        } catch (IOException ex) {
                        }
                    }
                }
            }
        }, 1000, TimeUnit.SECONDS.toMillis(3));
    }

    @OnOpen
    @Override
    public void onOpen(Session session, EndpointConfig config) {
        session.setMaxIdleTimeout(0);
        sessions.add(session);
    }

    @OnClose
    @Override
    public void onClose(Session session, CloseReason closeReason) {
        sessions.remove(session);
    }

    @OnError
    @Override
    public void onError(Session session, Throwable throwable) {
        if (throwable == null) {
            return;
        }
        throwable.printStackTrace();
        if (session != null) {
            sessions.remove(session);
        }
    }
}
