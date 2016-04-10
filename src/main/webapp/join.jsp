<%@page import="net.andylizi.gobang.GameStorage"%><%@page contentType="text/html" pageEncoding="UTF-8"%><%
    if(request.getQueryString() == null){
        response.sendRedirect("index.jsp");
        return;
    }
    if(GameStorage.rooms.containsKey(request.getQueryString().toLowerCase())){
        session.setAttribute("playing",request.getQueryString().toLowerCase());
 %>
1
<%
    }else{
 %>
notfound
<%
    }
%>