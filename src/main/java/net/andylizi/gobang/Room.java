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

import java.beans.Beans;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

public class Room extends Beans {

    private static final int EMPTY = 0;
    private static final int BLACK = 1;
    private static final int WHITE = 2;
    private static final Random random = new SecureRandom();

    private final String roomId;
    private Session player1;
    private RemoteEndpoint.Basic remote1;
    private Session player2;
    private RemoteEndpoint.Basic remote2;
    private final Set<Session> spectators = new HashSet<>();
    private boolean playing = false;
    private final boolean player1IsWhite;

    private boolean turnToBlack = false;
    private int steps = 0;
    private int rounds = 0;
    private int[][] data = new int[15][15];

    public static Room newRoom(Session owner) {
        StringBuilder builder = new StringBuilder(5);
        builder.append(random.nextInt(9) + 1);
        for (int i = 0; i < 4; i++) {
            builder.append(random.nextInt(10));
        }
        builder.append(owner.getId());
        Room room = new Room(builder.toString().toLowerCase(), owner);
        owner.addMessageHandler(room.new Player1Handler());
        GameStorage.rooms.put(room.roomId, room);
        try {
            owner.getBasicRemote().sendText("join:" + room.getRoomId());
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
        try {
            return room;
        } finally {
            clean();
        }
    }

    public synchronized static void clean() {
        Set<String> removeList = new HashSet<>(0);
        for (Room r : GameStorage.rooms.values()) {
            if (!isAvailable(r.player1) && !isAvailable(r.player2)) {
                removeList.add(r.getRoomId());
            }
        }
        System.out.println("clean "+removeList);
        GameStorage.rooms.keySet().removeAll(removeList);
    }
    
    private static boolean isAvailable(Session session){
        return session != null && session.isOpen();
    }

    private Room(String roomId, Session player1) {
        this.roomId = roomId.toLowerCase();
        this.player1 = player1;
        this.remote1 = player1.getBasicRemote();
        player1IsWhite = random.nextBoolean();

    }

    public String getRoomId() {
        return roomId;
    }

    public Session getPlayer1() {
        return player1;
    }

    public Session getPlayer2() {
        return player2;
    }

    public void join(Session player2) {
        if (playing && this.player2 != null && this.player2.isOpen()) {
            try {
                spectators.add(player2);
                player2.getBasicRemote().sendText("start:spectator");
                updateAllMap(-1, player2.getBasicRemote());
            } catch (IOException ex) {
            }
            return;
        }
        this.player2 = player2;
        this.remote2 = player2.getBasicRemote();
        if (canStart()) {
            start();
        }
        player2.addMessageHandler(this.new Player2Handler());
    }

    public void start() {
        if (!canStart()) {
            throw new IllegalStateException();
        }
        steps = 0;
        if (player1IsWhite) {
            sendToPlayer1("start:white");
            sendToPlayer2("start:black");
        } else {
            sendToPlayer1("start:black");
            sendToPlayer2("start:white");
        }
        updateAllMap(EMPTY, remote1, remote2);
        broadcast("clear");
        playing = true;
        rounds++;
    }

    public boolean canStart() {
        return !playing && player1 != null && player2 != null && player1.isOpen() && player2.isOpen();
    }

    public void onQuit(Session session) {
        if (session.equals(player1)) {
            if (player2.isOpen()) {
                gameOver((player1IsWhite ? "White" : "Black") + " left the game", false);
            }
        } else if (session.equals(player2)) {
            if (player1.isOpen()) {
                gameOver((!player1IsWhite ? "White" : "Black") + " left the game", false);
            }
        }else if(spectators.contains(session)){
            if(session.isOpen()){
                try {
                    session.getBasicRemote().sendText("closesocket", true);
                    session.close();
                } catch (IOException ex) {
                }
            }
            spectators.remove(session);
        }
    }

    public boolean isPlaying() {
        return playing;
    }

    public void checkWin(int x, int y, int d) {
        if (d == EMPTY) {
            return;
        }
        System.out.println("x=" + x + ",y=" + y + ",d=" + d);

        //↑
        if ((getTimes(x, y, 0, 1, d) + getTimes(x, y, 0, -1, d)) >= 4 // transverse
                || //←
                (getTimes(x, y, 1, 0, d) + getTimes(x, y, -1, 0, d)) >= 4 // longitudinal
                || //↘
                (getTimes(x, y, 1, 1, d) + getTimes(x, y, -1, -1, d)) >= 4 // oblique
                || //↙
                (getTimes(x, y, 1, -1, d) + getTimes(x, y, -1, 1, d)) >= 4) //  oblique
        {
            gameOver((d == WHITE ? "White" : "Back") + " win!", true);
        }
    }

    public int getTimes(int cx, int cy, int dx, int dy, int c) {
        if (c == EMPTY) {
            return 0;
        }
        if (dx == 0 && dy == 0) {
            return 0;
        }
        int times = 0;
        for (int k = 1; k <= 5; k++) {
            int nx = cx + (dx * k);
            int ny = cy + (dy * k);
            if (nx < 0 || ny < 0 || nx >= data.length || ny >= data[0].length) {
                continue;
            }
            int nc = data[nx][ny];
            if (nc == EMPTY || c != nc) {
                break;
            }
            times++;
        }
        return times;
    }

    private class Player1Handler implements MessageHandler.Whole<String> {

        @OnMessage
        @Override
        public void onMessage(String message) {
            StringTokenizer tokenizer = new StringTokenizer(message, ":");
            String[] args = new String[tokenizer.countTokens()];
            if (args.length == 0) {
                System.out.println("error = 0");
                return;
            }
            int i = 0;
            while (tokenizer.hasMoreTokens()) {
                args[i++] = tokenizer.nextToken();
            }
            tokenizer = null;
            if (args[0].equals("update")) {
                try {
                    int x = Integer.parseInt(args[1]);
                    int y = Integer.parseInt(args[2]);
                    if (Player1IsWhite() && turnToBlack) {
                        updateTo1(x, y);
                        return;
                    }
                    if (data[x][y] != EMPTY) {
                        updateTo1(x, y);
                        return;
                    }
                    steps++;
                    Room.this.data[x][y] = Player1Color();
                    update(x, y);
                    checkWin(x, y, data[x][y]);
                    turnToBlack = !turnToBlack;
                    broadcast("turn:" + (turnToBlack ? "BLACK" : "WHITE"));
                } catch (NumberFormatException ex) {
                    return;
                }
            }
        }
    }

    public Set<Session> getSpectators() {
        return spectators;
    }

    public int getSteps() {
        return steps;
    }

    public int getRounds() {
        return rounds;
    }

    public boolean Player1IsWhite() {
        return player1IsWhite;
    }

    public boolean Player2IsWhite() {
        return !player1IsWhite;
    }

    public int Player1Color() {
        return Player1IsWhite() ? WHITE : BLACK;
    }

    public int Player2Color() {
        return Player2IsWhite() ? WHITE : BLACK;
    }

    private class Player2Handler implements MessageHandler.Whole<String> {

        @OnMessage
        @Override
        public void onMessage(String message) {
            StringTokenizer tokenizer = new StringTokenizer(message, ":");
            String[] args = new String[tokenizer.countTokens()];
            if (args.length == 0) {
                System.out.println("error = 0");
                return;
            }
            int i = 0;
            while (tokenizer.hasMoreTokens()) {
                args[i++] = tokenizer.nextToken();
            }
            tokenizer = null;
            if (args[0].equals("update")) {
                try {
                    int x = Integer.parseInt(args[1]);
                    int y = Integer.parseInt(args[2]);
                    if (Player2IsWhite() && turnToBlack) {
                        updateTo1(x, y);
                        return;
                    }
                    if (data[x][y] != EMPTY) {
                        updateTo1(x, y);
                        return;
                    }
                    steps++;
                    Room.this.data[x][y] = Player2Color();
                    update(x, y);
                    turnToBlack = !turnToBlack;
                    broadcast("turn:" + (turnToBlack ? "BLACK" : "WHITE"));
                    checkWin(x, y, data[x][y]);
                } catch (NumberFormatException ex) {
                    return;
                }
            }
        }
    }

    public void broadcast(String message) {
        sendToPlayer1(message);
        sendToPlayer2(message);
        for (Session session : spectators) {
            if (session != null && session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message,true);
                } catch (IOException ex) {
                }
            }
        }
    }

    public void sendToPlayer1(String message) {
        if (remote1 != null && player1 != null && player1.isOpen()) {
            try {
                remote1.sendText(message);
            } catch (IOException ex) {
            }
        }
    }

    public void sendToPlayer2(String message) {
        if (remote2 != null && player2 != null && player2.isOpen()) {
            try {
                remote2.sendText(message);
            } catch (IOException ex) {
            }
        }
    }

    public synchronized void gameOver(String message, boolean canRestart) {
        if (!playing) {
            return;
        }
        playing = false;
        broadcast("gameover:" + message);
        System.out.println("gameover " + message);
        if (canRestart && canStart()) {
            start();
        } else {
            closeAll();
            player1 = null;
            remote1 = null;
            player2 = null;
            remote2 = null;
            GameStorage.rooms.remove(roomId);
        }
    }

    public void closeAll() {
        if (remote1 != null && player1 != null && player1.isOpen()) {
            try {
                player1.getBasicRemote().sendText("closesocket", true);
                player1.close();
            } catch (IOException ex) {
            }
        }
        if (remote2 != null && player2 != null && player2.isOpen()) {
            try {
                player2.getBasicRemote().sendText("closesocket", true);
                player2.close();
            } catch (IOException ex) {
            }
        }
        for (Session session : spectators) {
            if (session != null && session.isOpen()) {
                try {
                    session.getBasicRemote().sendText("closesocket", true);
                    session.close();
                } catch (IOException ex) {
                }
            }
        }
        spectators.clear();
    }

    public void update(int x, int y) {
        update(x, y, data[x][y]);
    }

    public void updateTo1(int x, int y) {
        sendToPlayer1("update:" + x + ":" + y + ":" + data[x][y]);
    }

    public void updateTo2(int x, int y) {
        sendToPlayer2("update:" + x + ":" + y + ":" + data[x][y]);
    }

    public void update(int x, int y, int data) {
        broadcast("update:" + x + ":" + y + ":" + data);
    }

    public void updateAllMap(int nc, RemoteEndpoint.Basic... remotes) {
        for (int x = 0; x < data.length; x++) {
            for (int y = 0; y < data[0].length; y++) {
                if (nc != -1) {
                    data[x][y] = nc;
                }
                String msg = "update:" + x + ":" + y + ":" + data[x][y];
                for (RemoteEndpoint.Basic remote : remotes) {
                    try {
                        remote.sendText(msg);
                    } catch (IOException ex) {
                    }
                }
            }
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
            return false;
        }
        final Room other = (Room) obj;
        return Objects.equals(this.roomId, other.roomId);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(this.roomId);
        return hash;
    }
    
}
