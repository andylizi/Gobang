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
import javax.websocket.CloseReason;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.RemoteEndpoint;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;

public class Room extends Beans {

    private static final int EMPTY = 0;
    private static final int BLACK = 1;
    private static final int WHITE = 2;

    private static final int NONE = 0;
    private static final int PLAYER1 = 1;
    private static final int PLAYER2 = 2;

    private static final Random random = new SecureRandom();
    private static final CloseReason NORMAL_CLOSE = new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "socketclose");

    public boolean isGarbage = false;
    private final String roomId;
    private final Handler handler;
    private User player1;
    private User player2;
    private final Set<User> spectators = new HashSet<>();
    private boolean playing = false;
    private final boolean player1IsWhite;

    private int undoRequest = 0;
    private boolean turnToBlack = false;
    private int steps = 0;
    private int rounds = 0;
    private int[][] data = new int[15][15];
    
    private int lastStepX = -1;
    private int lastStepY = -1;

    public static Room newRoom(User owner) {
        StringBuilder builder = new StringBuilder(5);
        builder.append(random.nextInt(9) + 1);
        for (int i = 0; i < 4; i++) {
            builder.append(random.nextInt(10));
        }
        builder.append(owner.getSocketId());
        Room room = new Room(builder.toString().toLowerCase(), owner);
        GameStorage.rooms.put(room.roomId, room);
        try {
            owner.getBasicRemote().sendText("room:" + room.getRoomId());
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
        if (!GameStorage.rooms.isEmpty()) {
            Set<String> removeList = new HashSet<>(0);
            for (Room r : GameStorage.rooms.values()) {
                if (r.isGarbage) {
                    removeList.add(r.getRoomId());
                    r.data = null;
                }
            }
            System.out.println("clean " + removeList);
            GameStorage.rooms.keySet().removeAll(removeList);
        }
    }

    public static boolean checkOpen(User user) {
        return user != null && user.isOpen();
    }

    private Room(String roomId, User player1) {
        this.roomId = roomId.toLowerCase();
        this.player1 = player1;
        player1IsWhite = random.nextBoolean();
        handler = new Handler();
        player1.addMessageHandler(handler.new Player1Handler());
    }

    public String getRoomId() {
        return roomId;
    }

    public User getPlayer1() {
        return player1;
    }

    public User getPlayer2() {
        return player2;
    }

    public void join(User player2) {
        if (player2 == null) {
            return;
        }
        if (playing && checkOpen(player2)) {
            try {
                spectators.add(player2);
                player2.addMessageHandler(handler.new SpectatorHandler(player2));
                RemoteEndpoint.Basic remote = player2.getBasicRemote();
                remote.sendText("start:spectator");
                updateAllMap(-1, true, player2.getBasicRemote());
                remote.sendText("status:white:" + (turnToBlack ? "Waiting..." : "Holding..."));
                remote.sendText("status:black:" + (turnToBlack ? "Holding..." : "Waiting..."));
                remote.sendText("join:white:" + (player1IsWhite ? player1.getName() : this.player2.getName()));
                remote.sendText("join:black:" + (!player1IsWhite ? player1.getName() : this.player2.getName()));
                broadcast("join:spectator:" + player2.getName());
            } catch (IOException ex) {
                spectators.remove(player2);
            }
            return;
        }
        this.player2 = player2;
        broadcast("join:" + (player1IsWhite ? "black" : "white") + ":" + player2.getName());
        sendToPlayer2("join:" + (player1IsWhite ? "white" : "black") + ":" + player1.getName());
        if (canStart()) {
            start(false);
        }
        player2.addMessageHandler(this.handler.new Player2Handler());
    }

    public void start(boolean restart) {
        if (!canStart()) {
            throw new IllegalStateException();
        }
        steps = 0;
        if (!restart) {
            if (player1IsWhite) {
                sendToPlayer1("start:white");
                sendToPlayer2("start:black");
            } else {
                sendToPlayer1("start:black");
                sendToPlayer2("start:white");
            }
            broadcast("status:white:Holding...");
            broadcast("status:black:Waiting...");
        }
        broadcast("turn:" + (turnToBlack ? "BLACK" : "WHITE"));
        updateAllMap(EMPTY, false, player1.getBasicRemote(), player2.getBasicRemote());
        broadcast("clear");
        playing = true;
        rounds++;
    }

    public boolean canStart() {
        return !playing && checkOpen(player1) && checkOpen(player2);
    }

    public void onQuit(User user) {
        if (user == null) {
            return;
        }
        if (player1 != null && user.equals(player1)) {
            gameOver((player1IsWhite ? "White" : "Black") + " left the game", false);
            isGarbage = true;
        } else if (player2 != null && user.equals(player2)) {
            gameOver((!player1IsWhite ? "White" : "Black") + " left the game", false);
            isGarbage = true;
        } else if (spectators.contains(user)) {
            if (checkOpen(user)) {
                try {
                    user.getBasicRemote().sendText("closesocket", true);
                    user.close(NORMAL_CLOSE);
                } catch (IOException ex) {
                }
            }
            spectators.remove(user);
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
            gameOver((d == WHITE ? "White" : "Black") + " win!", true);
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

    public Set<User> getSpectators() {
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

    private class Handler {

        public Handler() {

        }

        private String[] tokenizerMessage(String message) {
            StringTokenizer tokenizer = new StringTokenizer(message.replace("\\:", "▶卍"), ":");
            String[] args = new String[tokenizer.countTokens()];
            if (args.length == 0) {
                System.out.println("error = 0");
                return new String[0];
            }
            int i = 0;
            while (tokenizer.hasMoreTokens()) {
                args[i++] = tokenizer.nextToken().replace("▶卍", ":");
            }
            tokenizer = null;
            return args;
        }

        private class SpectatorHandler implements MessageHandler.Whole<String> {

            private User user;
            private String name;

            public SpectatorHandler(User user) {
                this.user = user;
                this.name = user.getName();
            }

            @OnMessage
            @Override
            public void onMessage(String message) {
                if (message.startsWith("chat:")) {
                    String[] args = tokenizerMessage(message);
                    if (args[2].length() > 50) {
                        try {
                            user.getBasicRemote().sendText("chat:System:Too long!");
                        } catch (IOException ex) {
                        }
                        return;
                    }
                    broadcast(args[0] + ":[S]" + args[1] + ":" + args[2]);
                }
            }

        }

        private class Player1Handler implements MessageHandler.Whole<String> {
            @OnMessage
            @Override
            public void onMessage(String message) {
                try {
                    String[] args = tokenizerMessage(message);
                    switch (args[0]) {
                        case "update":
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
                                lastStepX = x;
                                lastStepY = y;
                                update(x, y);
                                turnToBlack = !turnToBlack;
                                broadcast("turn:" + (turnToBlack ? "BLACK" : "WHITE"));
                                checkWin(x, y, data[x][y]);
                            } catch (NumberFormatException ex) {
                                return;
                            }
                            break;
                        case "status":
                            broadcast(message);
                            break;
                        case "chat":
                            if (args[2].length() > 50) {
                                sendToPlayer1("chat:System:Too long!");
                                return;
                            }
                            broadcast(args[0] + ':' + (player1IsWhite ? "[W]" : "[B]") + args[1] + ":" + args[2]);
                            break;
                        case "undo":
                            switch (args[1]) {
                                case "request":
                                    if(!((player1IsWhite && turnToBlack) || (!player1IsWhite && !turnToBlack))){
                                        return;
                                    }
                                    if (undoRequest != NONE) {
                                        return;
                                    }
                                    if(lastStepX == -1 || lastStepY == -1){
                                        return;
                                    }
                                    undoRequest = PLAYER1;
                                    broadcast("chat:System:"+(player1IsWhite ? "White" : "Black")+" requests to undo one step");
                                    broadcast("undo:request:"+(player1IsWhite ? "white" : "black"));
                                    break;
                                case "accept":
                                    if(undoRequest != PLAYER2){
                                        return;
                                    }
                                    if(lastStepX == -1 || lastStepY == -1){
                                        return;
                                    }
                                    Room.this.data[lastStepX][lastStepY] = EMPTY;
                                    update(lastStepX, lastStepY);
                                    sendToPlayer2("undo:accept");
                                    broadcast("chat:System:"+(!player1IsWhite ? "White" : "Black")+" undid one step...");
                                    broadcastToSepctators("undo:accept:"+(!player1IsWhite ? "White" : "Black"));
                                    turnToBlack = !turnToBlack;
                                    broadcast("turn:"+(!player1IsWhite ? "WHITE" : "BLACK")+":n");
                                    undoRequest = NONE;
                                    break;
                                case "deny":
                                    if(undoRequest != PLAYER2){
                                        return;
                                    }
                                    if(lastStepX == -1 || lastStepY == -1){
                                        return;
                                    }
                                    sendToPlayer2("undo:deny");
                                    broadcast("chat:System:Undo denied...");
                                    broadcastToSepctators("undo:deny:"+(!player1IsWhite ? "White" : "Black"));
                                    undoRequest = NONE;
                            }
                            break;
                        default:
                            break;
                    }
                } catch (IndexOutOfBoundsException ex) {
                    //ignore
                }
            }
        }

        private class Player2Handler implements MessageHandler.Whole<String> {

            @OnMessage
            @Override
            public void onMessage(String message) {
                try {
                    String[] args = tokenizerMessage(message);
                    switch (args[0]) {
                        case "update":
                            try {
                                int x = Integer.parseInt(args[1]);
                                int y = Integer.parseInt(args[2]);
                                if (Player2IsWhite() && turnToBlack) {
                                    updateTo2(x, y);
                                    return;
                                }
                                if (data[x][y] != EMPTY) {
                                    updateTo2(x, y);
                                    return;
                                }
                                steps++;
                                Room.this.data[x][y] = Player2Color();
                                lastStepX = x;
                                lastStepY = y;
                                update(x, y);
                                turnToBlack = !turnToBlack;
                                broadcast("turn:" + (turnToBlack ? "BLACK" : "WHITE"));
                                checkWin(x, y, data[x][y]);
                            } catch (NumberFormatException ex) {
                                return;
                            }   break;
                        case "status":
                            broadcast(message);
                            break;
                        case "chat":
                            if (args[2].length() > 50) {
                                sendToPlayer2("chat:System:Too long!");
                                return;
                            }   broadcast(args[0] + ':' + (!player1IsWhite ? "[W]" : "[B]") + args[1] + ":" + args[2]);
                            break;
                        case "undo":
                            switch (args[1]) {
                                case "request":
                                    if((player1IsWhite && turnToBlack) || (!player1IsWhite && !turnToBlack)){
                                        return;
                                    }
                                    if (undoRequest != NONE) {
                                        return;
                                    }
                                    if(lastStepX == -1 || lastStepY == -1){
                                        return;
                                    }
                                    undoRequest = PLAYER2;
                                    broadcast("chat:System:"+(!player1IsWhite ? "White" : "Black")+" requests to undo one step");
                                    broadcast("undo:request:"+(!player1IsWhite ? "white" : "black"));
                                    break;
                                case "accept":
                                    if(undoRequest != PLAYER1){
                                        return;
                                    }
                                    if(lastStepX == -1 || lastStepY == -1){
                                        return;
                                    }
                                    Room.this.data[lastStepX][lastStepY] = EMPTY;
                                    update(lastStepX, lastStepY);
                                    sendToPlayer1("undo:accept");
                                    broadcast("chat:System:"+(player1IsWhite ? "White" : "Black")+" undid one step...");
                                    broadcastToSepctators("undo:accept:"+(player1IsWhite ? "White" : "Black"));
                                    turnToBlack = !turnToBlack;
                                    broadcast("turn:"+(player1IsWhite ? "WHITE" : "BLACK")+":n");
                                    undoRequest = NONE;
                                    break;
                                case "deny":
                                    if(undoRequest != PLAYER1){
                                        return;
                                    }
                                    if(lastStepX == -1 || lastStepY == -1){
                                        return;
                                    }
                                    sendToPlayer1("undo:deny");
                                    broadcast("chat:System:Undo denied...");
                                    broadcastToSepctators("undo:deny:"+(player1IsWhite ? "White" : "Black"));
                                    undoRequest = NONE;
                            }
                            break;
                        default:
                            break;
                    }
                } catch (IndexOutOfBoundsException ex) {
                    //ignore
                }
            }
        }
    }

    public void broadcast(String message) {
        sendToPlayer1(message);
        sendToPlayer2(message);
        broadcastToSepctators(message);
    }

    public void sendToPlayer1(String message) {
        if (checkOpen(player1)) {
            try {
                player1.getBasicRemote().sendText(message);
            } catch (IOException ex) {
            }
        }
    }

    public void sendToPlayer2(String message) {
        if (checkOpen(player2)) {
            try {
                player2.getBasicRemote().sendText(message);
            } catch (IOException ex) {
            }
        }
    }
    
    public void broadcastToSepctators(String message){
        for (User user : spectators) {
            if (checkOpen(user)) {
                try {
                    user.getBasicRemote().sendText(message);
                } catch (IOException ex) {
                }
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
            start(true);
        } else {
            isGarbage = true;
            closeAll();
            player1 = null;
            player2 = null;
            GameStorage.rooms.remove(roomId);
        }
    }

    public void closeAll() {
        if (checkOpen(player1)) {
            player1.getAsyncRemote().sendText("closesocket", new SendHandler() {
                @Override
                public void onResult(SendResult result) {
                    try {
                        player1.close(NORMAL_CLOSE);
                    } catch (IOException ex) {
                    }
                }
            });
        }
        if (checkOpen(player2)) {
            player2.getAsyncRemote().sendText("closesocket", new SendHandler() {
                @Override
                public void onResult(SendResult result) {
                    try {
                        player2.close(NORMAL_CLOSE);
                    } catch (IOException ex) {
                    }
                }
            });
        }
        for (final User player : spectators) {
            if (checkOpen(player)) {
                player.getAsyncRemote().sendText("closesocket", new SendHandler() {
                    @Override
                    public void onResult(SendResult result) {
                        try {
                            player.close(NORMAL_CLOSE);
                        } catch (IOException ex) {
                        }
                    }
                });
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

    public void updateAllMap(int nc, boolean send, RemoteEndpoint.Basic... remotes) {
        for (int x = 0; x < data.length; x++) {
            for (int y = 0; y < data[0].length; y++) {
                if (nc != -1) {
                    data[x][y] = nc;
                }
                if (send) {
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
