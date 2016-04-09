<%@page import="net.andylizi.gobang.GameStorge"%><%@page contentType="text/html" pageEncoding="UTF-8"%><%
    if(request.getQueryString() == null){
        response.sendRedirect("index.jsp");
        return;
    }
    if(GameStorge.rooms.containsKey(request.getQueryString().toLowerCase())){
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