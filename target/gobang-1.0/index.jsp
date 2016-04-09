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
                    $(function(){
                        $("#main").fadeIn("slow");
                        $("#btn_create").click(function(){
                            $("#btn_join").fadeOut("fast");
                            var e = $(this);
                            e.html("Loading...");
                            location.href = "game.jsp?create";
                        });
                        $("#btn_join").one("click",function(){
                            $("#btn_create").fadeOut("fast","swing",function(){
                                $(this).remove();
                            });
                            this.disabled = true;
                            setTimeout(function(){
                                $("#txt_join").show().animate({
                                    width: "150px"
                                },"slow","swing",function(){
                                    this.focus();
                                }).keydown(function(){
                                    $("#btn_join").attr("disabled",false);
                                });
                            },200);
                            $(this).click(function(){
                                var val = $("#txt_join").val();
                                if(!val){
                                    $("#txt_join")[0].focus();
                                    return;
                                }
                                $.get("join.jsp?"+val,function(data){
                                    if(parseInt(data) == 1){
                                        location.href = "game.jsp?"+val;
                                    }else{
                                        alert("Room Id Not Found");
                                    }
                                })
                            });
                        });
                    });
                //-->
            </script>
            <div id="title" style="background-image: url('image/bgs/<%=(int)Math.ceil(Math.random() * 6)%>.svg');">
                Gobang
            </div>
            <div id="content">
                <span class="button" id="btn_create" style="margin-right: 30px;">Create</span>
                <input type="text" id="txt_join" style="display: none;width:0px;" placeholder="Room ID"/>
                <span class="button" style="background-color: #00bcd4;"  id="btn_join">Join</span>
            </div>
        </div>
    </body>
</html>
