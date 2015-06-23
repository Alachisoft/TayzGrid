<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <title></title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  </head>
  <body>
	Is requested session from cookie?<%=request.isRequestedSessionIdFromCookie()%><br>
    <a href="<%=response.encodeURL("welcome.jsp")%>" target="right"> Show Session Information </a><br>
    <a href="<%=response.encodeURL("synch.jsp")%>" target="right"> Synchronization </a><br>
	<a href="<%=response.encodeURL("debug.jspx")%>" target="right"> Debug </a><br>
    <a href="<%=response.encodeURL("debug.jspx?operation=invalidate")%>" target="right"> Debug - invalidate </a><br>
    <a href="<%=response.encodeURL("debug.jspx?operation=create")%>" target="right"> Debug - create </a><br>
    <a href="<%=response.encodeURL("load.html")%>" target="right"> Load Test </a><br>
  </body>
</html>
