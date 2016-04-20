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
(function ($) {
    new Image().src = "image/white.png";
    new Image().src = "image/black.png";
    var isWhite = false;
    var started = false;
    var spectator = false;
    var turn = false;
    var socket = new Socket();
    var msgBuffer = new Array();
    var Dialog = new window.Dialog(function () {
        while (msgBuffer.length != 0)
            msgBuffer.pop();
        socket.close();
        Dialog.closeDialog();
        $("#fbtn_close").fadeIn("slow");
    }, function () {
        while (msgBuffer.length != 0)
            msgBuffer.pop();
        socket.close();
        location.href = "index.jsp";
    }, function () {
        while (msgBuffer.length != 0) {
            var e = msgBuffer.shift();
            listener.onMessage(e);
            console.log("Pop message: " + e.data);
        }
    });
    var listener = {
        onConnected: function () {
            console.log("Connected");
            $("#bar_white .content").html("Ready.");
        },
        onMessage: function (evt) {
            var args = evt.data.split(":");
            console.log("Server: " + evt.data);
            if (args[0] == "room") {
                roomId = args[1];
                if (create)
                    window.history.pushState({}, 0, location.href.replace(/\?\w+$/, "") + "?" + args[1]);
                Dialog.openDialog("Waiting for join...", "<p>To invite a player, give the following URL:</p>" +
                        "<a class='can_select_text' href='" + location.href + "' onclick='return false;'>" + location.href + "</a><p>\n\
                The game will be started once any player joined the game.</p>\n\
                <p>Room id: <span class='can_select_text' style='color:#009688;'>" + args[1] + "</span></p>",
                        "<button class='flat' id='btn_cancel'>Cancel</button>",
                        function () {
                            $("#btn_cancel").click(function () {
                                location.href = "index.jsp";
                            });
                        });
                appendChat("System: Room #" + args[1] + " created");
                document.title = "Gobang - #" + args[1];
            } else if (args[0] == "start") {
                if (!create) {
                    document.title = "Gobang - #" + roomId;
                }
                appendChat("System: Game started");
                Dialog.closeDialog();
                started = true;
                $("#bar_white>.content,#bar_black>.content").html("Ready.");
                isWhite = args[1] == "white";
                spectator = args[1] == "spectator";
                if (spectator) {
                    $("#fbtn_undo").hide();
                }
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
                setTurn(turn);
                $("#bar_white").addClass("bar_highlight");
                $("#bar_white").removeClass("disable");
                $("#bar_black").addClass("disable");
                $("#bar_black").removeClass("bar_highlight");
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
                    appendChat(args[2] + " join the room as a spectator...");
                    return;
                }
                if (!spectator)
                    appendChat(args[2] + " join the room as " + args[1] + "...");
                $("#" + args[1] + "_name").html(" - " + args[2]);
            } else if (args[0] == "update") {
                if (buffer(evt))
                    return;
                setColor(args[1], args[2], args[3]);
            } else if (args[0] == "turn") {
                if (buffer(evt))
                    return;
                turn = (isWhite ? args[1] == "WHITE" : args[1] == "BLACK");
                if (!spectator) {
                    if (turn) {
                        $("table").addClass("turn");
                    } else {
                        $("table").removeClass("turn");
                    }
                    setTurn(turn);
                    $("#fbtn_undo").prop("disabled", args[2] || turn);
                }
                if (args[1] == "WHITE") {
                    $("#bar_black").addClass("disable").removeClass("bar_highlight").children(".content").html("Waiting...");
                    $("#bar_white").addClass("bar_highlight").removeClass("disable").children(".content").html("Holding...");
                } else {
                    $("#bar_white").addClass("disable").removeClass("bar_highlight").children(".content").html("Waiting...");
                    $("#bar_black").addClass("bar_highlight").removeClass("disable").children(".content").html("Holding...");
                }
            } else if (args[0] == "undo") {
                if (args[1] == "request") {
                    if (spectator) {
                        Dialog.makeToast(args[2].replace(/(\w)/, function (v) {
                            return v.toUpperCase();
                        }) + " requests to undo one step");
                        return;
                    }
                    if ((args[2] == "white" && isWhite) || (args[2] == "black" && !isWhite)) {
                        Dialog.makeToast("Undo request has been issued");
                        return;
                    }
                    $("#fbtn_undo").prop("disabled", true);
                    Dialog.openDialog("Undo request", args[2].replace(/(\w)/, function (v) {
                        return v.toUpperCase();
                    }) + " requests to undo one step",
                            "<button class='flat text_red' id='btn_deny'>Deny</button>\n\
                        <button class='flat text_blue' id='btn_accept'>Accept</button>",
                            function () {
                                $("#btn_accept").click(function () {
                                    Dialog.closeDialog();
                                    socket.send("undo:accept");
                                    $("#fbtn_undo").prop("disabled", true);
                                });
                                $("#btn_deny").click(function () {
                                    Dialog.closeDialog();
                                    socket.send("undo:deny");
                                    $("#fbtn_undo").prop("disabled", true);
                                });
                            });
                } else if (args[1] == "accept") {
                    if (spectator) {
                        Dialog.makeToast((args[2] == "White" ? "Black" : "White") + " accepted " + args[2] + "'s undo requst");
                    } else {
                        Dialog.makeToast((isWhite ? "Black" : "White") + " accepted your undo requst");
                    }
                } else if (args[1] == "deny") {
                    if (spectator) {
                        Dialog.makeToast((args[2] == "White" ? "Black" : "White") + " denied " + args[2] + "'s undo requst");
                    } else {
                        Dialog.makeToast((isWhite ? "Black" : "White") + " denied your undo requst");
                    }
                }
            } else if (args[0] == "gameover") {
                appendChat("System: Game over: " + args[1]);
                if (spectator) {
                    Dialog.openDialog("Game Over", args[1], "<button class='flat_button' id='btn_ok'>OK</button>");
                    $("#btn_ok").click(function () {
                        Dialog.closeDialog();
                    });
                } else {
                    var canRestart = (args[1].indexOf("win") != -1);
                    Dialog.openDialog("Game Over", args[1],
                            "<button class='flat" + (canRestart ? " text_red" : "") + "' style='display: inline;' id='btn_close'>Close</button>\n\
                    <button class='flat" + (canRestart ? " text_red" : " text_blue") + "' style='display: inline;' id='btn_back'>Back</button>" + (canRestart ?
                            "<button class='flat text_blue' style='display: inline;' id='btn_restart'>Restart</button>" : ""),
                            function () {
                                $("#btn_restart").click(function () {
                                    appendChat("System: Game restarted...");
                                    Dialog.closeDialog();
                                });
                            });
                }
                $("table").removeClass("turn");
                $("#bar_white>.content").html("Game Over");
                $("#bar_black>.content").html("Game Over");
                setTurn(false);
            } else if (args[0] == "clear") {
                if (buffer(evt))
                    return;
                $(".chessiece").remove();
                $("#fbtn_undo").prop("disabled", true);
            } else if (args[0] == "closesocket") {
                socket.close();
                appendChat("System: Connection closed");
                setTurn(false);
            } else if (args[0] == "err") {
                $("#bar_" + (isWhite ? "white" : "black") + " .content").html("Error: " + args[1]);
                socket.send("status:" + (isWhite ? "white" : "black") + ":Error\: " + args[1]);
            } else if (args[0] == "status") {
                if (buffer(evt))
                    return;
                $("#bar_" + args[1] + " .content").html(args[2]);
            } else if (args[0] == "chat") {
                var msg = args[2];
                for (var i = 3; i < args.length; i++) {
                    msg += ":" + args[i];
                }
                appendChat(args[1] + ": " + msg);
            } else {
                alert("Unknown message: \r\n" + evt.data);
            }
        },
        onError: function (err) {
            Dialog.openDialog("Error", err);
        },
        onClose: function () {
            if (!Dialog.isOpen()) {
                Dialog.openDialog("Socket Closed", "The connection to the server has been disconnected.",
                        "<button class='flat' style='margin-right: 10px;display: inline;' id='btn_close'>Close</button>\n\
                <button class='flat text_red' style='margin-right: 10px;display: inline;' id='btn_back'>Back</button>");
            }
            $("#fbtn_close").fadeIn("slow");
            $("#fbtn_undo").prop("disabled", true);
            window.history.pushState({}, 0, location.href.replace(/\?\w+$/, "?closed"));
            $("#bar_black,#bar_white").addClass("disable");
            if (started) {
                $("#bar_white>.content,#bar_black>.content").html("Socket Closed");
                started = false;
            }
            turn = false;
            setTurn(false);
        }
    };
    function buffer(evt) {
        if (Dialog.isOpen()) {
            msgBuffer.push(evt);
            console.log("Buffer: " + evt.data);
            return true;
        } else {
            return false;
        }
    }
    function setColor(x, y, c) {
        $(".last").removeClass("last");
        var e = $("#row_" + x + "_" + y);
        if (c == 0) { //EMPTY
            e.empty();
        } else if (c == 1) {  //BLACK
            $("#fbtn_undo").prop("disabled", isWhite);
            e.html("<span class='chessiece black'>&nbsp;</span>").children("span").addClass("last");
        } else if (c == 2) {  //WHITE
            $("#fbtn_undo").prop("disabled", !isWhite);
            e.html("<span class='chessiece white'>&nbsp;</span>").children("span").addClass("last");
        }
    }
    function appendChat(msg) {
        $("#bar_chat_content").prepend(htmlEncode(msg) + "<br/>");
    }
    function setTurn(t) {
        if (t) {
            document.title = "[Turn]" + document.title.replace("[Turn]", "");
        } else {
            document.title = document.title.replace("[Turn]", "");
        }
    }
    function htmlEncode(str) {
        var div = document.createElement("div");
        div.appendChild(document.createTextNode(str));
        return div.innerHTML;
    }
    $(function () {
        $("#board").fadeIn("slow");
        appendChat("System: Connecting to server...");
        var socketurl = location.href.replace(/^\w+:/, "ws:").replace("game.jsp", "socket").replace("?create", "");
        if (!socket) {
            location.href = "index.jsp";
        } else {
            socket.connect(socketurl, listener.onConnected, listener.onMessage, listener.onError, listener.onClose);
        }
        $("#txt_chat").keydown(function (e) {
            if (this.value && e.keyCode == 13) {
                socket.send("chat:" + username + ":" + this.value.replace(":", "\\:"));
                this.value = "";
            }
        });
        $("#fbtn_close").click(function (e) {
            location.href = "index.jsp";
        });
        $("#fbtn_undo").click(function (e) {
            if (turn || !started) {
                $(this).fadeOut("fast");
                return;
            }
            socket.send("undo:request");
            $(this).prop("disabled", true);
        });
    });
})(window.jQuery);
