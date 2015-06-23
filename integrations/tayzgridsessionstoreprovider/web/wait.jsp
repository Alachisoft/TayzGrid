Waiting ....<br><br>
<%
	try
	{
		Thread.sleep(5 * 1000);
	}
	catch(Exception exp)
	{
		out.println("Exception: " + exp.toString());
	}
%>
<br><br>Completed ....