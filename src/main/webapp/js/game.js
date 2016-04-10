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
new Image().src="image/white.png";
new Image().src="image/black.png";
var isWhite = false;
var started = false;
var spectator = false;
var turn = false;
var socket = new Socket();
function onConnected(){
    console.log("Connected");
}
function onMessage(evt){
    console.log("Server: "+evt.data);
    var args = evt.data.split(":");
    if(args[0] == "join"){
        if(create)
            window.history.pushState({},0,location.href.replace("?create","?"+args[1]));
    }else if(args[0] == "start"){
        started = true;
        isWhite = args[1] == "white";
        spectator = args[1] == "spectator";
        alert("You are "+args[1]);
	$("#holder").text("white");
	$("#yourself").text(args[1]);
        turn = isWhite;
        if(turn){
            $("table").addClass("turn");
        }else{
            $("table").removeClass("turn");
        }
        $("td").click(function(){
            if(!turn){
                return;
            }
            var e = $(this);
            var pos = e.attr("id").split("_");
            socket.send("update:"+pos[1]+":"+pos[2]+":"+(isWhite ? 2 : 1));
        });
    }else if(args[0] == "update"){
        setColor(args[1],args[2],args[3]);
    }else if(args[0] == "turn"){
        $("#holder").text(args[1].toLowerCase());
        if(spectator) return;
        turn = (isWhite ? args[1] == "WHITE" : args[1] == "BLACK");
        $("table").toggleClass("turn");
    }else if(args[0] == "gameover"){
        alert("Game Over!\r\n"+args[1]);
        started = false;
    }else if(args[0] == "clear"){
        $(".chessiece").remove();
    }else if(args[0] == "closesocket"){
        socket.close();
    }else{
        alert(evt.data);
    }
}
function onError(err){
    alert("Error " + err);
}
function onClose(){
    window.history.pushState({},0,location.href.replace(/\?\w+$/,"?closed"));
    alert("WebSocket closed...");
}
var socketurl = location.href.replace(/^\w+:/,"ws:").replace("game.jsp","socket").replace("?create","");
if(!socket){
    location.href = "index.jsp";
}else{
    socket.connect(socketurl,onConnected,onMessage,onError,onClose);
}
function setColor(x,y,c){
    var e = $("#row_"+x+"_"+y);
    if(c == 0){ //EMPTY
        e.empty();
    }else if(c == 1){  //BLACK
        e.html("<span class='chessiece black'>&nbsp;</span>")
    }else if(c == 2){  //WHITE
        e.html("<span class='chessiece white'>&nbsp;</span>")
    }
}

