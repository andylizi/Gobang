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
                    refushList(false);
                    setInterval(function(){
                        refushList(true);
                    },3000);
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
                                    }, "fast");
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
                        $.get("join.jsp?" + val, function (data) {
                            if (parseInt(data) == 1) {
                                location.href = "game.jsp?" + val;
                            } else {
                                alert("RoomId Not Found");
                                $("#txt_join").val("")[0].focus();
                            }
                        });
                    });
                }
                function refushList(cache){
                    $.ajax("roomlist.jsp", {
                        dataType: "json",
                        cache: cache,
                        success: function (data) {
                            var list = $("#list ul").empty();
                            var changed = false;
                            $.each(data, function (id, v) {
                                changed = true;
                                list.append("<li>Room - <span class='id'>" + id + "</span>&nbsp;&nbsp;<span class='"+(v.playing ? "playing" : "waiting")+"'>"+
                                        (v.playing ? "Playing" : "Waiting")+"</span>&nbsp;\n\
                                    Rounds: " + v.rounds + "&nbsp;&nbsp;\n\
                                    Steps: " + v.steps + "&nbsp;&nbsp;\n\
                                    Watchers: " + v.watchers + "&nbsp;&nbsp;&nbsp;&nbsp;<a href='game.jsp?"+id+"'>["+(v.playing ? "Watch" : "Join")+"]</a></li>");
                            });
                            if(!changed){
                                list.append("<li>None</li>");
                            }
                            list.parent().slideDown("slow");
                        }
                    });
                }
                //-->
            </script>
            <div id="title" style="background-image: url('image/bgs/0.svg');">
                Gobang
            </div>
            <div id="content">
                <span class="button" id="btn_create">Create Room</span>
                <input type="text" id="txt_join" style="visibility: hidden;width:0px;" placeholder="Room ID"/>
                <span class="button" style="background-color: #00bcd4;margin-left: 20px;" id="btn_join">Join Joom</span>
                <span class="button" style="background-color: #00D437;display: none;margin-left: 10px;" id="btn_back">Back</span>
                <div id="list" style="display:none;">
                    <ul>
                    </ul>
                </div>
            </div>
        </div>
    </body>
</html>
