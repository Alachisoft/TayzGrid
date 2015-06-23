<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    </head>
    <body>
        <h1>Synch 3</h1>
        <%
            session.setAttribute("s3-att1","altaf4");
            printAttributes(out, session);
        %>
    </body>
</html>

<%!
	void printAttributes(JspWriter out, HttpSession session) throws java.io.IOException
	{
		boolean hasAtt = false;
		java.util.Enumeration  names = session.getAttributeNames();
		while (names.hasMoreElements())
		{
			hasAtt = true;
			String name = (String)names.nextElement();
			String val = (String)session.getAttribute(name);
			out.println(name + "&nbsp;:&nbsp;" + val + "<br>");
		}
		if (!hasAtt)
		{
			out.println("No attributes in session");
		}
	}
%>
