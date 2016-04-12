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
import java.util.Map;
import java.util.WeakHashMap;
import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import static net.andylizi.gobang.Room.clean;

@ServerEndpoint(value="/socket",configurator=HttpSessionConfigurator.class)
public class GameEndpoint extends Endpoint {
    private final Map<Session,User> USER_CACHE = new WeakHashMap<>();
    
    @OnOpen
    @Override
    public void onOpen(Session session, EndpointConfig config) {
        clean();
        User user = createUser(session, (HttpSession) config.getUserProperties().get("HttpSession"));
        System.out.println("Open Session " + user.getId());
        if (session.getQueryString() == null || session.getQueryString().isEmpty()) {
            Room.newRoom(user);
        } else {
            String str = session.getQueryString();
            if (GameStorage.rooms.containsKey(str.toLowerCase())) {
                Room room = GameStorage.rooms.get(str.toLowerCase());
                System.out.println("join "+room.getRoomId());
                room.join(user);
            } else {
                try {
                    session.getBasicRemote().sendText("err:roomId not found");
                    session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "roomId not found"));
                } catch (IOException ex) {
                }
            }
        }
    }
    public User createUser(Session session,HttpSession httpSession){
        User user = USER_CACHE.get(session);
        if(user == null){
            if(httpSession == null) return null;
            user = new User(session, httpSession);
            USER_CACHE.put(session, user);
        }
        return user;
    }

    @OnClose
    @Override
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("Close session " + session.getId() + " cause by " + closeReason.toString());
        for (Room room : GameStorage.rooms.values()) {
            room.onQuit(createUser(session, null));
        }
        clean();
    }

    @OnError
    @Override
    public void onError(Session session, Throwable throwable) {
        if(throwable == null) return;
        throwable.printStackTrace();
        if (session != null) {
            for (Room room : GameStorage.rooms.values()) {
                if(room != null)
                    room.onQuit(createUser(session, null));
            }
        }
    }
}
