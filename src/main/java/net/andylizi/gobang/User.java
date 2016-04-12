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
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.MessageHandler;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

public class User{
    public final Session socketSession;
    public final HttpSession httpSession;
    private boolean closed;

    public User(Session socketSession, HttpSession httpSession) {
        this.socketSession = Objects.requireNonNull(socketSession);
        this.httpSession = Objects.requireNonNull(httpSession);
        this.socketSession.setMaxIdleTimeout(0);
        this.closed = !checkOpen(socketSession);
    }

    public boolean isClosed() {
        return closed ? true : (closed = !checkOpen(socketSession));
    }
    public boolean isOpen(){
        return !isClosed();
    }
    public String getId(){
        return httpSession.getId()+socketSession.getId();
    }
    public String getSocketId(){
        return socketSession.getId();
    }
    public String getHttpSessionId(){
        return httpSession.getId();
    }
    
    public void addMessageHandler(MessageHandler handler){
        this.socketSession.addMessageHandler(handler);
    }
    
    public RemoteEndpoint.Basic getBasicRemote(){
        return socketSession.getBasicRemote();
    }
    public RemoteEndpoint.Async getAsyncRemote(){
        return socketSession.getAsyncRemote();
    }
    public Object getAttribute(String key){
        return httpSession.getAttribute(key);
    }
    public void close() throws IOException{
        socketSession.close();
        closed = true;
    }
    public void close(CloseReason reason) throws IOException{
        socketSession.close(reason);
        closed = true;
    }
    public String getName(){
        return (String) (httpSession.getAttribute("username") == null ? "Anonymous" : httpSession.getAttribute("username"));
    }
    
    private static Field REMOTE_ENDPOINT_FIELD;
    private static Field ENDPOINT_CLOESED_FIELD;
    private static final Map<Session, Object> REMOTE_CACHE = new WeakHashMap<>(2);

    public static boolean checkOpen(Session session) {
        if (session == null || !session.isOpen()) {
            return false;
        }
        try {
            Object remote = REMOTE_CACHE.get(session);
            if (remote == null) {
                if (REMOTE_ENDPOINT_FIELD == null) {
                    REMOTE_ENDPOINT_FIELD = session.getClass().getDeclaredField("wsRemoteEndpoint");
                    REMOTE_ENDPOINT_FIELD.setAccessible(true);
                }
                remote = REMOTE_ENDPOINT_FIELD.get(session);
                REMOTE_CACHE.put(session, remote);
            }
            if (ENDPOINT_CLOESED_FIELD == null) {
                ENDPOINT_CLOESED_FIELD = remote.getClass().getSuperclass().getDeclaredField("closed");
                ENDPOINT_CLOESED_FIELD.setAccessible(true);
            }
            return !ENDPOINT_CLOESED_FIELD.getBoolean(remote);
        } catch (ReflectiveOperationException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            if(obj instanceof Session){
                return obj.equals(socketSession);
            }
            return false;
        }
        final User other = (User) obj;
        return Objects.equals(this.socketSession, other.socketSession);
    }

    @Override
    public int hashCode() {
        return socketSession.hashCode();
    }

    @Override
    public String toString() {
        return socketSession.toString();
    }
}
