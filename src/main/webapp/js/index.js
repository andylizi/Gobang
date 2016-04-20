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
(function () {
    function login() {
        var finished = false;
        if ($("#username").html() == "Anonymous") {
            setTimeout(function () {
                if (!finished) {
                    $("#btn_login").html("Loading...");
                }
            }, 200);
            if ($("#login_form"))
                $("#login").load("ajax/login.jsp", {}, show);
            else
                show();
            function show() {
                finished = true;
                $("#btn_login").html("[Cancel]").one("click", function () {
                    $("#login").slideUp("slow");
                    $("#btn_login").html("[Login]").one("click", login);
                    $("#actions").slideDown("slow");
                });
                $("#login").slideDown("slow");
            }
        } else {
            setTimeout(function () {
                if (!finished) {
                    $("#btn_login").html("Loading...");
                }
            }, 200);
            $("#username_arena").load("ajax/login.jsp", {action: "loginout"}, function () {
                finished = true;
                $("#login").slideUp("slow");
                $("#btn_login").one("click", login);
            });
        }
    }
    function showJoin() {
        if($("#btn_back:visible").length > 0) return;
        location.hash = "#join";
        $("#btn_create").animate({
            "margin-right": "10px"
        }, "slow");
        $("#txt_join").css("visibility", "visible");
        $("#btn_back").fadeIn("slow").one("click", function () {
            location.hash = "";
            $(this).fadeOut("normal", function () {
                setTimeout(function () {
                    $("#btn_join").prop("disabled",false);
                    setTimeout(function () {
                        $("#btn_create").css("visibility", "visible").fadeIn("slow");
                    }, 200);
                    $("#txt_join").animate({
                        width: "0px"
                    }, "slow").fadeOut("fast", "swing", function () {
                        $(this).css("display", "inline").css("visibility", "hidden")[0].focus();
                        $("#btn_join").one("click", showJoin).animate({
                            marginLeft: "20px"
                        }, "fast", "swing", function () {
                            $("#txt_join").val("");
                        });
                    });
                }, 100);
            });
        });
        $("#btn_join").prop("disabled",!$("#txt_join").val());
        setTimeout(function () {
            $("#txt_join").animate({
                width: "150px"
            }, "slow", "swing", function () {
                this.focus();
            }).keyup(function (e) {
                $("#btn_join")[0].disabled = !this.value;
                if (!this.value) {
                    return;
                }
                if (e.keyCode == 13) {
                    $("#btn_join").click();
                }
            });
        }, 200);
        $("#btn_join").css("margin-left", "10px").on("click", function () {
            var val = $("#txt_join").val();
            if (!val) {
                $("#txt_join")[0].focus();
                return;
            }
            $("#btn_join").val("Loading...");
            $.get("ajax/join.jsp?" + val, function (data) {
                if (parseInt(data) == 1) {
                    location.href = "game.jsp?" + val;
                } else {
                    vibrate($("#txt_join"));
                }
            });
        });
    }
    function vibrate(e, callback) {
        var ir = e.css("margin-right");
        var il = e.css("margin-left");
        e.css("border-bottom", "1px red solid").animate({
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
            e.css({"margin-right": ir, "margin-left": il});
            e.one("keyup", function () {
                e.css("border-bottom", "1px rgba(177,175,175,0.50) solid")[0].focus();
            });
            if (callback) {
                callback(e);
            }
        });
    }
    var socket = new Socket();
    function initStatusSocket(connectCount) {
        if (!connectCount) {
            connectCount = 1;
        }
        var list = $("#list ul");
        socket.connect(location.href.replace(/#\w*$/, "").replace(/^\w+:/, "ws:").replace(/\/index.jsp|\/$/, "/status"), function () {}, function (evt) {
            list.empty();
            if (evt.data == "{}") {
                list.parent(":visible").slideUp("slow");
                return;
            }
            $.each($.parseJSON(evt.data), function (id, v) {
                list.append("<li>Room <span class='id'>#" + id + "</span>&nbsp;" + v.owner + "&nbsp;&nbsp;<span class='" + (v.playing ? "playing" : "waiting") + "'>[" +
                        (v.playing ? "Playing" : "Waiting") + "]</span>&nbsp;\n\
                                    Rounds: " + v.rounds + "&nbsp;&nbsp;\n\
                                    Steps: " + v.steps + "&nbsp;&nbsp;\n\
                                    Watchers: " + v.watchers + "&nbsp;&nbsp;&nbsp;&nbsp;<a href='game.jsp?" + id + "'>[" + (v.playing ? "Watch" : "Join") + "]</a></li>");
            });
            $(".id").click(function () {
                $("#txt_join").val(this.innerHTML);
                showJoin();
            });
            list.parent(":hidden").slideDown("slow");
        }, function () {}, function () {
            initStatusSocket(++connectCount);
        });
    }

    $(function () {
        $.get("ajax/init.html");
        $("#title").css("background-image", "url('image/bgs/" + parseInt(Math.random() * 6) + ".svg')");
        if ($("#main").width() < 890) {
            $("#gear").hide();
        }
        $("#main").css({
            display: "none",
            visibility: "visible"
        }).fadeIn("slow");
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
        $("#username_arena").load("ajax/login.jsp", {action: "info"}, function () {
            $("#btn_login").one("click", login);
        });
        if (location.hash == "#join") {
            showJoin();
        }
        initStatusSocket();
    });
})(window.jQuery);
