<%@page import="net.andylizi.gobang.UserManager"%>
<%@page import="java.io.File"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    String username = (String) session.getAttribute("username");
    if (username == null) {
        username = "Anonymous";
    }
    String action = request.getParameter("action");
    boolean loginout = "loginout".equalsIgnoreCase(request.getParameter("action"));
    if (loginout || "info".equalsIgnoreCase(action)) {
        if (loginout) {
            session.removeAttribute("username");
            username = "Anonymous";
        }
%>
<span id="username"><%=username%></span>&nbsp;&nbsp;<a id="btn_login">[<%=(username.equals("Anonymous") ? "Login" : "Logout")%>]</a>
<%
        return;
    } else if ("login".equalsIgnoreCase(action)) {
        response.getWriter().write(UserManager.login(session, request.getParameter("username"), request.getParameter("password")));
        return;
    }
%>
<script src="js/lib/jquery.md5.js"></script>
<form id="login_form" action="login.jsp" method="POST">
    <input type="hidden" name="action" value="login"/>
    <input type="text" autofocus id="l_username" name="username" placeholder="Username" size="28" autocomplete="off"/>
    <br/>
    <input type="password" id="l_password" name="password" placeholder="Password" style="margin-bottom: 20px;" size="28"/>
    <br/>
    <span id="l_btn_login" class="button disabled" style="background: #323a45; margin: 0 auto; margin-bottom: 5px;display: block;width: 254px;">Login</span>
</form>
<div id="l_btn_register" class="flat_button" style="color: #00BCD4;margin-top: 10px;">Register</div>
<script reload="1">
    $("#actions").slideUp("slow");
    $("#l_username,#l_password").keyup(function (e) {
        if ($("#l_username").val() && $("#l_password").val())
            $("#l_btn_login").removeClass("disabled");
        else {
            $("#l_btn_login").addClass("disabled");
            return;
        }
        if (e.keyCode == 13)
            $("#l_btn_login").click();
    }).keypress(function (e) {
        var p = $("#l_password").val();
        if (p.length > 24) {
            $("#l_password").val(p.substring(0, 24));
        }
        var u = $("#l_username").val();
        if (u.length > 16) {
            $("#l_username").val(u.substring(0, 16));
        }
    }).on("click", function () {
        if (this.value.length == 32) {
            this.value = "";
        }
    });
    $("#l_btn_register").click(function () {
        $("#l_loading").fadeIn("slow");
        $("#login").slideUp("slow",function(){
            $("#l_loading").fadeOut("slow");
            $(this).load("ajax/register.jsp",{},function(){
                $(this).slideDown("slow");
            });
        });
    });
    $("#l_btn_login").click(function () {
        if (!$("#l_username").val() || !$("#l_password").val()) {
            if (!$("#l_username").val()) {
                vibrate($("#l_username"));
            }
            if (!$("#l_password").val()) {
                vibrate($("#l_password"));
            }
            return;
        }
        $("#l_btn_login").addClass("disabled").html("Loading...");
        if ($("#l_password").val().length != 32)
            $("#l_password").val($.md5($("#l_password").val()));
        $.post("ajax/login.jsp", $("#login_form").serializeArray(), function (data) {
            $("#l_btn_login").html("Login");
            $("#l_password").keyup();
            data = data.trim();
            if (data == "un_notfound") {
                vibrate($("#l_username"));
            } else if (data == "pw_wrong") {
                vibrate($("#l_password"), function (e) {
                    e.val("");
                });
            } else if (data == "ok") {
                $("#login").slideUp("slow");
                $("#actions").slideDown("slow");
                $("#username_arena").load("ajax/login.jsp", {action: "info"}, function () {
                    $("#btn_login").one("click", login);
                });
            } else {
                alert(data);
            }
        });
    });
</script>