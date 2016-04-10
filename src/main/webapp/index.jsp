<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <title>Gobang</title>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link href="style/normalize.css" rel="stylesheet" type="text/css">
        <link href="style/common.css" rel="stylesheet" type="text/css">
        <link href="style/index.css" rel="stylesheet" type="text/css">
        <script src="http://libs.baidu.com/jquery/2.0.3/jquery.min.js"></script>
        <script src="js/lib/hclass.js"></script>
        <script src="js/socket.js"></script>
    </head>
    <body>
        <span id="gear"></span>
        <div id="main">
            <script>
                <!--
                $("#main").hide();
                $(function () {
                    $("#title").css("background-image", "url('image/bgs/" + parseInt(Math.random() * 6) + ".svg')");
                    $("#main").fadeIn("fast");
                    $("#btn_create").click(function () {
                        $("#btn_join").fadeOut("fast");
                        var e = $(this);
                        e.html("Loading...");
                        location.href = "game.jsp?create";
                    });
                    $("#btn_list").click(function () {
                        $("#btn_list .icon").toggleClass("turn_off").toggleClass("turn_on");
                    });
                    $("#btn_join").one("click", showJoin);
                    if (location.hash == "#join") {
                        showJoin();
                    }
                    initStatusSocket();
                });
                function showJoin() {
                    location.hash = "#join";
                    $("#btn_create").css("visibility", "hidden");
                    $("#txt_join").css("visibility", "visible");
                    $("#btn_back").fadeIn("slow").one("click", function () {
                        location.hash = "";
                        $(this).fadeOut("normal", function () {
                            setTimeout(function () {
                                setTimeout(function () {
                                    $("#btn_create").css("visibility", "visible").fadeIn("slow");
                                }, 200);
                                $("#txt_join").animate({
                                    width: "0px"
                                }, "slow").fadeOut("fast", "swing", function () {
                                    $(this).css("display", "inline").css("visibility", "hidden");
                                    $("#btn_join").one("click", showJoin).animate({
                                        marginLeft: "20px"
                                    }, "fast", "swing", function () {
                                        $("#txt_join").val("");
                                    });
                                });
                            }, 200);
                        });
                    });
                    this.disabled = true;
                    setTimeout(function () {
                        $("#txt_join").animate({
                            width: "150px"
                        }, "slow", "swing", function () {
                            this.focus();
                        }).keydown(function () {
                            $("#btn_join").attr("disabled", false);
                        });
                    }, 200);
                    $("#btn_join").css("margin-left", "10px").on("click", function () {
                        var val = $("#txt_join").val();
                        if (!val) {
                            $("#txt_join")[0].focus();
                            return;
                        }
                        $("#btn_join").val("Loading...");
                        $.get("join.jsp?" + val, function (data) {
                            if (parseInt(data) == 1) {
                                location.href = "game.jsp?" + val;
                            } else {
                                $("#txt_join").css("border-bottom", "1px red solid").animate({
                                    "margin-right": "+5px",
                                    "margin-left": "-5px"
                                }, 50).animate({
                                    "margin-right": "-5px",
                                    "margin-left": "+5px"
                                }, 50).animate({
                                    "margin-right": "+5px",
                                    "margin-left": "-5px"
                                }, 50).animate({
                                    "margin-right": "-5px",
                                    "margin-left": "+5px"
                                }, 50).animate({
                                    "margin-right": "+5px",
                                    "margin-left": "-5px"
                                }, 50).animate({
                                    "margin-right": "-5px",
                                    "margin-left": "+5px"
                                }, 50).animate({
                                    "margin-right": "-5px",
                                    "margin-left": "+5px"
                                }, 50, "swing", function () {
                                    setTimeout(function () {
                                        $("#txt_join").css("border-bottom", "1px rgba(177,175,175,0.50) solid")[0].focus();
                                    }, 400);
                                });
                            }
                        });
                    });
                }
                var socket = new Socket();
                function initStatusSocket() {
                    var list = $("#list ul");
                    socket.connect(location.href.replace(/#\w*$/, "").replace(/^\w+:/, "ws:").replace(/\/index.jsp|\/$/, "/status"), function () {}, function (evt) {
                        list.empty();
                        if (evt.data == "{}") {
                            list.parent(":visible").slideUp("slow");
                            return;
                        }
                        $.each($.parseJSON(evt.data), function (id, v) {
                            list.append("<li>Room - <span class='id'>" + id + "</span>&nbsp;&nbsp;<span class='" + (v.playing ? "playing" : "waiting") + "'>" +
                                    (v.playing ? "Playing" : "Waiting") + "</span>&nbsp;\n\
                                    Rounds: " + v.rounds + "&nbsp;&nbsp;\n\
                                    Steps: " + v.steps + "&nbsp;&nbsp;\n\
                                    Watchers: " + v.watchers + "&nbsp;&nbsp;&nbsp;&nbsp;<a href='game.jsp?" + id + "'>[" + (v.playing ? "Watch" : "Join") + "]</a></li>");
                        });
                        $(".id").click(function () {
                            showJoin();
                            $("#txt_join").val(this.innerHTML);
                        });
                        list.parent(":hidden").slideDown("slow");
                    }, function () {}, function () {});
                }
                //-->
            </script>
            <div id="title" style="background-image: url('image/bgs/0.svg');">
                Gobang
            </div>
            <div id="content">
                <span class="button" id="btn_create">Create Room</span>
                <input type="text" id="txt_join" style="visibility: hidden;width:0px;margin-right:0px;margin-left:0px;" placeholder="Room ID" autocomplete="off" autofocus="on"/>
                <span class="button" style="background-color: #00bcd4;margin-left: 20px;" id="btn_join">Join Room</span>
                <span class="button" style="background-color: #00D437;display: none;margin-left: 10px;" id="btn_back">Back</span>
                <div id="list" style="display:none;">
                    <ul>
                    </ul>
                </div>
            </div>
        </div>
    </body>
</html>
