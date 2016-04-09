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
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/socket")
public class WebSocketEndpoint extends Endpoint{
    @OnOpen
    @Override
    public void onOpen(Session session, EndpointConfig config) {
        System.out.println("open "+session.getId());
        if(session.getQueryString() == null || session.getQueryString().isEmpty()){
            Room.newRoom(session);
        }else{
            String str = session.getQueryString();
            if(GameStorge.rooms.containsKey(str.toLowerCase())){
                Room room = GameStorge.rooms.get(str.toLowerCase());
                room.join(session);
            }else{
                try {
                    session.getBasicRemote().sendText("err:roomId not found");
                    session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "roomId not found"));
                } catch (IOException ex) {
                }
            }
        }
    }

    @OnClose
    @Override
    public void onClose(Session session, CloseReason closeReason){
        System.out.println("close "+session.getId()+" cause by " +closeReason.toString());
        for(Room room:GameStorge.rooms.values()){
            room.onQuit(session);
        }
    }

    @OnError
    @Override
    public void onError(Session session, Throwable throwable) {
        if(throwable.getCause() != null && throwable.getCause().getClass().equals(java.util.concurrent.ExecutionException.class)){
            try {
                session.close();
            } catch (IOException|IllegalStateException ex) {
            }
            return;
        }
        for(Room room:GameStorge.rooms.values()){
            room.onQuit(session);
        }
        System.out.println("a");
        throwable.printStackTrace();
    }
}
