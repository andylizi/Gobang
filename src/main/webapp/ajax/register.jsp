<%@page import="net.andylizi.gobang.UserManager"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    String action = request.getParameter("action");
    if("register".equalsIgnoreCase(action)){
        if(!((String)session.getAttribute("vcode")).equalsIgnoreCase(request.getParameter("vcode"))){
            response.getWriter().write("vcode_wrong");
            return;
        }
        response.getWriter().write(UserManager.register(session, request.getParameter("username"), request.getParameter("password")));
        return;
    }
%>
<script src="js/lib/jquery.md5.js"></script>
<form id="register_form" action="register.jsp" method="POST" style="text-align: left;width: 254px; margin: 0 auto;">
    <input type="hidden" name="action" value="register"/>
    <input type="text" id="r_username" name="username" placeholder="Username" size="28" autofocus autocomplete="off" maxlength="24"/>
    <br/>
    <input type="password" id="r_password" name="password" placeholder="Password" size="28" autocomplete="off" maxlength="24"/>
    <br/>
    <input type="text" id="r_vcode" name="vcode" placeholder="Validate Code" size="13"  style="margin: 10px 0;" autocomplete="off" maxlength="4"/><img src="validatecode.sl" id="vcode" style="cursor:pointer;" onclick="this.src='validatecode.sl?'+Math.random();"/>
    <button id="r_btn_register" class="bluegrey" disabled style="margin: 0 auto;margin-bottom: 5px;display: block;width: 254px;padding: 4px 0;">Sign up</button>
</form>
<button id="r_btn_login" class="flat text_blue non-uppercase" style="margin-top: 10px;">Sign in</button>
<script reload="1">
    $("#r_username,#r_password,#r_vcode").keyup(function (e) {
        if ($("#r_username").val() && $("#r_password").val() && $("#r_vcode").val().length == 4)
            $("#r_btn_register")[0].disabled = false;
        else {
            $("#r_btn_register")[0].disabled = true;
            return;
        }
        if (e.keyCode == 13)
            $("#r_btn_register").click();
    }).on("click", function () {
        if (this.value.length == 32) {
            this.value = "";
        }
    });
    $("#r_btn_login").click(function () {
        $("#l_loading").fadeIn("slow");
        $("#login").slideUp("slow",function(){
            $(this).load("ajax/login.jsp",{},function(){
                $("#l_loading").fadeOut("slow");
                $(this).slideDown("slow");
            });
        });
    });
    $("#r_btn_register").click(function () {
        if (!$("#r_username").val() || !$("#r_password").val() || !$("#r_vcode").val()) {
            if (!$("#r_username").val()) {
                vibrate($("#r_username"));
            }
            if (!$("#r_password").val()) {
                vibrate($("#r_password"));
            }
            if(!$("#r_vcode").val()){
                vibrate($("#r_vcode"));
            }
            return;
        }
        $("#r_btn_register").html("Signing up...")[0].disabled = true;
        if ($("#r_password").val().length != 32)
            $("#r_password").val($.md5($("#r_password").val()));
        $.post("ajax/register.jsp", $("#register_form").serializeArray(), function (data) {
            $("#r_password").keyup();
            data = data.trim();
            if (data == "exists") {
                vibrate($("#r_username"));
            }else if(data == "invalid_un" ){
                vibrate($("#r_username"));
                alert("Username must match [\\u4e00-\\u9fa5A-za-z0-9]+");
            } else if(data == "vcode_wrong"){
                vibrate($("#r_vcode"),function(){
                    $("#vcode")[0].onclick();
                    $("#r_vcode").val("");
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
            $("#r_btn_register").html("Sign up");
        });
    });
</script>