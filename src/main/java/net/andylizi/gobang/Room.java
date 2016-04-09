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
import java.util.Random;
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

    private String roomId;
    private final Session player1;
    private final RemoteEndpoint.Basic remote1;
    private Session player2;
    private RemoteEndpoint.Basic remote2;
    private boolean playing = false;
    private final boolean player1IsWhite;

    private boolean turnToBlack = false;

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
        GameStorge.rooms.put(room.roomId, room);
        try {
            owner.getBasicRemote().sendText("join:" + room.getRoomId());
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
        return room;
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
        if (player1IsWhite) {
            sendToPlayer1("start:white");
            sendToPlayer2("start:black");
        } else {
            sendToPlayer1("start:black");
            sendToPlayer2("start:white");
        }
        for(int x = 0;x < data.length;x++){
            for(int y = 0;y < data[0].length;y++){
                data[x][y] = EMPTY;
                update(x, y);
            }
        }
        playing = true;
    }

    public boolean canStart() {
        return player1 != null && player2 != null && player1.isOpen() && player2.isOpen();
    }

    public void onQuit(Session session) {
        if (session.equals(player1)) {
            gameOver((player1IsWhite ? "White" : "Black") + " left the game");
        } else if (session.equals(player2)) {
            gameOver((!player1IsWhite ? "White" : "Black") + " left the game");
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
        i:
        for (int i = -1; i <= 1; i++) {
            j:
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) {
                    continue;
                }
                int k = 1;
                k:
                for (; k <= 5; k++) {
                    int nx = x + (i * k);
                    int ny = y + (j * k);
                    System.out.println("k=" + k + ",i=" + i + ",j=" + j + ",nx=" + nx + ",ny=" + ny);
                    if (nx < 0 || ny < 0 || nx >= data.length || ny >= data[0].length) {
                        break;
                    }
                    int nd = data[nx][ny];
                    System.out.println("data=" + nd);
                    if (nd == EMPTY || d != nd) {
                        break;
                    }
                }
                System.out.println("k=" + k);
                if (k >= 5) {
                    gameOver((d == WHITE ? "White" : "Black") + " win!");
                    return;
                }
            }
        }
        System.out.println("================");
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
                    Room.this.data[x][y] = Player2Color();
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

    public void broadcast(String message) {
        sendToPlayer1(message);
        sendToPlayer2(message);
    }

    public void sendToPlayer1(String message) {
        if (remote1 != null && player1 != null && player1.isOpen()) {
            try {
                remote1.sendText(message);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void sendToPlayer2(String message) {
        if (remote2 != null && player2 != null && player2.isOpen()) {
            try {
                remote2.sendText(message);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public synchronized void gameOver(String message) {
        if (!playing) {
            return;
        }
        playing = false;
        broadcast("gameover:" + message);
        System.out.println("gameover" + message);
        if (canStart()) {
            start();
        } else {
            closeAll();
            player2 = null;
            remote2 = null;
            GameStorge.rooms.remove(roomId);
        }
    }

    public void closeAll() {
        if (remote1 != null && player1 != null && player1.isOpen()) {
            try {
                player1.close();
            } catch (IOException ex) {
            }
        }
        if (remote2 != null && player2 != null && player2.isOpen()) {
            try {
                player2.close();
            } catch (IOException ex) {
            }
        }
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
}
