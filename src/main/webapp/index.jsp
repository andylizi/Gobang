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
                    $("#main").fadeIn("slow");
                    $("#btn_create").click(function () {
                        $("#btn_join").fadeOut("fast");
                        var e = $(this);
                        e.html("Loading...");
                        location.href = "game.jsp?create";
                    });
                    $("#btn_join").one("click", showJoin);
                    if(location.hash == "#join"){
                        showJoin();
                    }
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
                                    },"fast");;
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
                    $("#btn_join").css("margin-left","10px").on("click",function () {
                        var val = $("#txt_join").val();
                        if (!val) {
                            $("#txt_join")[0].focus();
                            return;
                        }
                        $.get("join.jsp?" + val, function (data) {
                            if (parseInt(data) == 1) {
                                location.href = "game.jsp?" + val;
                            } else {
                                alert("Room Id Not Found");
                            }
                        });
                    });
                }
                //-->
            </script>
            <div id="title" style="background-image: url('image/bgs/<%=(int) Math.ceil(Math.random() * 6)%>.svg');">
                Gobang
            </div>
            <div id="content">
                <span class="button" id="btn_create">Create</span>
                <input type="text" id="txt_join" style="visibility: hidden;width:0px;" placeholder="Room ID"/>
                <span class="button" style="background-color: #00bcd4;margin-left: 20px;" id="btn_join">Join</span>
                <span class="button" style="background-color: #00D437;display: none;margin-left: 10px;" id="btn_back">Back</span>
            </div>
        </div>
    </body>
</html>
