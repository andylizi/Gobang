<%@page import="net.andylizi.gobang.GameStorage"%><%@page import="net.andylizi.gobang.Room"%><%@page contentType="application/json" pageEncoding="UTF-8"%>{<% 
    Room[] rooms = GameStorage.rooms.values().toArray(new Room[0]);
    for(int i = 0;i < rooms.length;i++){
        Room room = rooms[i];
%>
    "<%=room.getRoomId()%>":{
        "playing": <%=room.isPlaying()%>,
        "steps": <%=room.getSteps()%>,
        "rounds": <%=room.getRounds()%>,
        "watchers": <%=room.getSpectators().size()%>
    }<%=(i == rooms.length - 1 ? "" : ",")%><% } %>
}
