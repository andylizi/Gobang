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
new Image().src = "image/white.png";
new Image().src = "image/black.png";
var isWhite = false;
var started = false;
var spectator = false;
var turn = false;
var msgboxOpened = false;
var socket = new Socket();
var msgs = new Array();
function onConnected() {
    console.log("Connected");
    $("#bar_white .content").html("Ready.");
}
function onMessage(evt) {
    console.log("Server: " + evt.data);
    var args = evt.data.split(":");
    if (args[0] == "room") {
        if (create)
            window.history.pushState({}, 0, location.href.replace("?create", "?" + args[1]));
        showMsgbox("Waiting for join...", "<p>To invite a player, give the following URL:</p>" +
                "<a class='can_select_text' href='" + location.href + "' onclick='return false;'>" + location.href + "</a><p>\n\
                The game will be started once any player joined the game.</p>\n\
                <p>Room id: <span class='can_select_text' style='color:#009688;'>" + args[1] + "</span></p>\n\
                <span class='button' style='background-color: #00D437;margin-left: 30px;' id='btn_cancel'>Cancel</span>");
        $("#btn_cancel").click(function () {
            location.href = "index.jsp";
        });
        appendChat("Room "+args[1]+" Created");
    } else if (args[0] == "start") {
        appendChat("Game Started");
        hideMsgbox();
        started = true;
        $("#bar_white>.content,#bar_black>.content").html("Ready.");
        isWhite = args[1] == "white";
        spectator = args[1] == "spectator";
        if (isWhite) {
            $("#bar_white").addClass("you");
            $("#white_name").html(" - " + username);
        } else if (!spectator) {
            $("#bar_black").addClass("you");
            $("#black_name").html(" - " + username);
        }
        turn = isWhite;
        if (turn) {
            $("table").addClass("turn");
        } else {
            $("table").removeClass("turn");
        }
        $("#bar_white").addClass("bar_turn");
        $("#bar_white").removeClass("disable");
        $("#bar_black").addClass("disable");
        $("#bar_black").removeClass("bar_turn");
        $("td").click(function () {
            if (!turn) {
                return;
            }
            var e = $(this);
            var pos = e.attr("id").split("_");
            socket.send("update:" + pos[1] + ":" + pos[2] + ":" + (isWhite ? 2 : 1));
        });
    } else if (args[0] == "join") {
        if (args[1] == "spectator") {
            appendChat(args[2]+" join the room as a spectator...");
            return;
        }
        if(!spectator)
            appendChat(args[2]+" join the room as "+args[1]+"...");
        $("#" + args[1] + "_name").html(" - " + args[2]);
    } else if (args[0] == "update") {
        setColor(args[1], args[2], args[3]);
    } else if (args[0] == "turn") {
        if (msgboxOpened) {
            msgs.push(evt);
            return;
        }
        turn = (isWhite ? args[1] == "WHITE" : args[1] == "BLACK");
        if (!spectator) {
            if (turn) {
                $("table").addClass("turn");
            } else {
                $("table").removeClass("turn");
            }
        }
        $("#bar_white").toggleClass("bar_turn disable");
        $("#bar_black").toggleClass("bar_turn disable");
        if (args[1] == "WHITE") {
            $("#bar_white>.content").html("Holding...");
            $("#bar_black>.content").html("Waiting...");
        } else {
            $("#bar_white>.content").html("Waiting...");
            $("#bar_black>.content").html("Holding...");
        }
    } else if (args[0] == "gameover") {
        appendChat("Game Over: "+args[1]);
        var canRestart = (args[1].indexOf("win") != -1);
        showMsgbox("Game Over", args[1] + (canRestart ? "<br/><br/>\n\
                <span class='button' style='background-color: #00D437;margin-left: 30px;margin-top: 10px;' id='btn_restart'>Restart</span>" : "<br/><br/>") + "\n\
                <span class='button' style='background-color: #9c27b0;margin-left: 30px;margin-top: 10px;' id='btn_close'>Close</span>");
        $("#btn_restart").click(function () {
            appendChat("Game Restarted...");
            hideMsgbox();
        });
        $("table").removeClass("turn");
        $("#bar_white>.content").html("Game Over");
        $("#bar_black>.content").html("Game Over");
    } else if (args[0] == "clear") {
        if (msgboxOpened) {
            msgs.push(evt);
            return;
        }
        $(".chessiece").remove();
    } else if (args[0] == "closesocket") {
        socket.close();
        appendChat("Connection closed");
    } else if (args[0] == "err") {
        $("#bar_" + (isWhite ? "white" : "black") + " .content").html("Error: " + args[1]);
        socket.send("status:" + (isWhite ? "white" : "black") + ":Error\: " + args[1]);
    } else if (args[0] == "status") {
        $("#bar_" + args[1] + " .content").html(args[2]);
    } else if(args[0] == "chat"){
        appendChat(args[1]+": "+args[2]);
    } else {
        alert(evt.data);
    }
}
function onError(err) {
    showMsgbox("Error", err);
}
function onClose() {
    window.history.pushState({}, 0, location.href.replace(/\?\w+$/, "?closed"));
    $("#bar_black,#bar_white").addClass("disable");
    if (started) {
        $("#bar_white>.content,#bar_black>.content").html("Socket Closed");
        started = false;
    }
}
$(function () {
    var socketurl = location.href.replace(/^\w+:/, "ws:").replace("game.jsp", "socket").replace("?create", "");
    if (!socket) {
        location.href = "index.jsp";
    } else {
        socket.connect(socketurl, onConnected, onMessage, onError, onClose);
    }
    $("#txt_chat").keydown(function(e){
        if(this.value && e.keyCode == 13){
            socket.send("chat:"+username+":"+this.value);
            this.value = "";
        }
    });
});
function setColor(x, y, c) {
    $(".last").removeClass("last");
    var e = $("#row_" + x + "_" + y);
    if (c == 0) { //EMPTY
        e.empty();
    } else if (c == 1) {  //BLACK
        e.html("<span class='chessiece black'>&nbsp;</span>").children("span").addClass("last");
    } else if (c == 2) {  //WHITE
        e.html("<span class='chessiece white'>&nbsp;</span>").children("span").addClass("last");
    }
}
var msgsQueue = new Array();
function showMsgbox(title, content) {
    if (msgboxOpened) {
        msgsQueue.push({"title": title, "content": content});
        return;
    }
    msgboxOpened = true;
    $("#msgbox_title").html(title);
    $("#msgbox_content").html(content);
    $("#mask").hide().fadeIn("fast");
    $("#msgbox").show().animate({
        "margin-top": "50px",
        "opacity": 1
    }, "fast");
    $("#btn_close").click(function () {
        while(msgs.length != 0) msgs.pop();
        socket.close();
        hideMsgbox();
    });
}
function hideMsgbox() {
    msgboxOpened = false;
    $("#mask").fadeOut("fast");
    $("#msgbox").show().animate({
        "margin-top": "0px",
        "opacity": 0
    }, "fast", "swing", function () {
        $(this).css("display", "none");
        if (msgsQueue.length != 0) {
            var task = msgsQueue.shift();
            showMsgbox(task.title, task.content);
            return;
        }
        while (msgs.length != 0) {
            onMessage(msgs.shift());
        }
    });
}
function appendChat(msg){
    $("#bar_chat_content").prepend(msg+"<br/>");
}
