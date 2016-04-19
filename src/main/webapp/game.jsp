<%@page import="net.andylizi.gobang.Room"%>
<%@page import="net.andylizi.gobang.GameStorage"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    boolean create = false;
    String roomId = null;

    if ("create".equalsIgnoreCase(request.getQueryString())) {
        create = true;
    } else if ("closed".equalsIgnoreCase(request.getQueryString())) {
        response.sendRedirect("index.jsp");
    } else if (request.getQueryString() != null) {
        Room room = GameStorage.rooms.get(request.getQueryString().toString().toLowerCase());
        if (room == null) {
            response.getWriter().write("<script>alert(\"Can not found room id \\\"" + request.getQueryString() + "\\\"!\");location.href=\"index.jsp\";</script>");
            return;
        }
        roomId = room.getRoomId();
        create = false;
    } else {
        create = true;
    }
    String username = session.getAttribute("username") != null ? (String) session.getAttribute("username") : "Anonymous";
%>
<!DOCTYPE html>
<html>
    <head>
        <title>Gobang</title>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="shortcut icon" href="image/favicon.ico" type="image/x-icon" />
        <link href="style/normalize.css" rel="stylesheet" type="text/css">
        <link href="style/common.css" rel="stylesheet" type="text/css">
        <link href="style/messages.css" rel="stylesheet" type="text/css">
        <link href="style/board.css" rel="stylesheet" type="text/css">
        <script src="http://libs.baidu.com/jquery/2.0.3/jquery.min.js" onerror="document.write('<script src=js/lib/jquery.js>&lt;/script&gt;');"></script>
        <script src="js/lib/class.js"></script>
        <script src="js/lib/dialog.js"></script>
        <script src="js/socket.js"></script>
        <script>var create = <%=create%>;var roomId = "<%=(roomId == null ? "null" : roomId)%>";var username = "<%=username%>";</script>
        <script src="js/game.js"></script>
    </head>
    <body>
        <span id="gear"></span>
        <table id="board" style="display:none;">
            <tbody>
                <tr class="border-top" id="row_0">
                    <td class="border-left" id="row_0_0"><span class="put"></span></td>
                        <% for (int col = 1; col < 14; col++) { %>
                    <td id="row_0_<%=col%>"><span class="put"></span></td>
                        <% } %>
                    <td class="border-right" id="row_0_14"><span class="put"></span></td>
                </tr>

                <% for (int row = 1; row < 14; row++) { %>
                <tr id="row_<%=row%>">
                    <td class="border-left" id="row_<%=row%>_0"><span class="put"></span></td>
                        <% for (int col = 1; col < 14; col++) { %>
                    <td id="row_<%=row%>_<%=col%>"><span class="put"></span></td>
                        <% } %>
                    <td class="border-right" id="row_<%=row%>_14"><span class="put"></span></td>
                </tr>
                <% } %>

                <tr class="border-bottom" id="row_14">
                    <td class="border-left" id="row_14_0"><span class="put"></span></td>
                        <% for (int col = 1; col < 14; col++) { %>
                    <td id="row_14_<%=col%>"><span class="put"></span></td>
                        <% } %>
                    <td class="border-right" id="row_14_14"><span class="put"></span></td>
                </tr>
            </tbody>
        </table>
        <button id="fbtn_close" class="floating_button" style="display: none;" alt="Close">
            <svg style="fill:#ffffff;color:#ffffff;width:32px;" viewBox="0 0 24 24">
                <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"></path>
            </svg>
            <span class="button_mask" alt="Close"></span>
        </button>
        <button id="fbtn_undo" class="floating_button" alt="Undo" disabled>
            <img src="image/undo.png"/>
            <span class="button_mask" alt="Undo"></span>
        </button>
        <div id="bar">
            <div class="" id="bar_white">
                <div class="title">
                    <img src="image/white.png"/><span class="title_content">White<span id="white_name"></span></span>
                </div>
                <div class="content">Waiting for join...</div>
            </div>
            <div class="" id="bar_black">
                <div class="title">
                    <img src="image/black.png"/><span class="title_content">Black<span id="black_name"></span></span>
                </div>
                <div class="content">Waiting for join...</div>
            </div>
            <div id="bar_chat" class="can_select_text">
                <div id="bar_chat_title">Chat</div>
                <input type="text" id="txt_chat" placeholder="What do you want to say?"/>
                <div id="bar_chat_content_wrapper">
                    <textarena id="bar_chat_content" class="can_select_text"></textarena>
                </div>
            </div>
        </div>
    </body>
</html>
