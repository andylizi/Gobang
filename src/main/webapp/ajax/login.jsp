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
<span id="username"><%=username%></span>&nbsp;&nbsp;<a id="btn_login">[<%=(username.equals("Anonymous") ? "Sign in" : "Sign out")%>]</a>
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
    <input type="text" autofocus id="l_username" name="username" placeholder="Username" size="28" autocomplete="off" maxlength="24"/>
    <br/>
    <input type="password" id="l_password" name="password" placeholder="Password" style="margin-bottom: 20px;" size="28" maxlength="24"/>
    <br/>
    <button id="l_btn_login" class="bluegrey" disabled style="margin: 0 auto;margin-bottom: 5px;display: block;width: 254px;padding: 4px 0;">Sign in</button>
</form>
<button id="l_btn_register" class="flat text_blue non-uppercase" style="text-transform: none;margin-top: 10px;">Sign up</button>
<script reload="1">
    $("#actions").slideUp("slow");
    $("#l_username,#l_password").keyup(function (e) {
        if ($("#l_username").val() && $("#l_password").val())
            $("#l_btn_login")[0].disabled = false;
        else {
            $("#l_btn_login")[0].disabled = true;
            return;
        }
        if (e.keyCode == 13)
            $("#l_btn_login").click();
    }).on("focus", function () {
        if (this.value.length == 32) {
            this.value = "";
        }
    });
    $("#l_btn_register").click(function () {
        $("#l_loading").fadeIn("slow");
        $("#login").slideUp("slow",function(){
            $(this).load("ajax/register.jsp",{},function(){
                $(this).slideDown("slow");
                $("#l_loading").fadeOut("slow");
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
        $("#l_btn_login").html("Signing in...")[0].disabled = true;
        if ($("#l_password").val().length != 32)
            $("#l_password").val($.md5($("#l_password").val()));
        $.post("ajax/login.jsp", $("#login_form").serializeArray(), function (data) {
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
                return;
            } else {
                alert("Unknown response: "+data);
            }
            $("#l_btn_login").html("Sign in");
        });
    });
</script>