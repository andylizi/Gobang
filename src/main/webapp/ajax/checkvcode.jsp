<%@page contentType="text/plain" pageEncoding="UTF-8"%><%
    if(!((String)session.getAttribute("vcode")).equalsIgnoreCase(request.getParameter("vcode"))){
        response.getWriter().write("0");
    }else{
        response.getWriter().write("1");
    }
%>