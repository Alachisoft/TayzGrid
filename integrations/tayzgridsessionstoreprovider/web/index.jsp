<%@page contentType="text/html" pageEncoding="UTF-8"%>
<HTML>
	</HEAD>
	<body>
		<form name="Form1" method="post" action="<%=response.encodeURL("index.jsp")%>" id="Form1">
			<span id="Label1" style="background-color:Wheat;font-family:Tahoma;font-size:Smaller;font-weight:bold;height:24px;width:454px;Z-INDEX: 100; LEFT: 22px; POSITION: absolute; TOP: 58px">The computer has selected a random number between 1 and 50 (inclusive). In this game you have to try to guess the number.</span>
			<input name="txtGuess" type="text" maxlength="3" id="txtGuess" style="border-width:1px;border-style:Solid;width:48px;Z-INDEX: 101; LEFT: 121px; POSITION: absolute; TOP: 130px" />
			<span id="Label2" style="font-family:Tahoma;font-size:Smaller;Z-INDEX: 102; LEFT: 22px; POSITION: absolute; TOP: 133px">Enter a number:</span>
			<span id="lblHint" style="background-color:Transparent;font-family:Tahoma;font-size:X-Small;font-style:italic;Z-INDEX: 103; LEFT: 24px; POSITION: absolute; TOP: 199px">Hint: The number is between 0 and 50.</span>
			<input type="submit" name="cmdGuess" value="Guess!" id="cmdGuess" style="border-width:1px;border-style:Solid;font-family:Tahoma;font-size:Smaller;width:134px;Z-INDEX: 107; LEFT: 188px; POSITION: absolute; TOP: 129px" />
			<span id="Label3" style="width:454px;text-decoration:none;height:23px;font-weight:bold;font-size:X-Small;font-family:Tahoma;color:White;border-style:Outset;background-color:LightSlateGray;z-index: auto; left:22px; position: absolute; top: 24px">Using JvCache's Session Store Provider Implementation</span>
		</form>
		<%
		if(session.getAttribute("page") == null && session.getAttribute("gamestarted") != "true")
		{
			//out.print("<div style='text-decoration:none;left:22px; position: absolute; top: 400px'>Welcome to Guess Game</div>");
			// startGame();
			java.util.Random rand=new java.util.Random();
			session.setAttribute("gamestarted", "true");
			session.setAttribute("secretNumber", new Integer(rand.nextInt(50)));
		}
		{
			//StartNewGame();
			session.setAttribute("page", "gamestarted");
			if(request.getParameter("txtGuess")!= null)
			{
				if(!request.getParameter("txtGuess").equals(session.getAttribute("secretNumber").toString()))
				{
					String guess = (String)session.getAttribute("guess");
					if(guess == null)
						guess = "";
					session.setAttribute("guess",  guess + " " + request.getParameter("txtGuess"));
					out.print("<div style='text-decoration:none;left:22px; position: absolute; top: 200px' >" + session.getAttribute("guess") + "</div>");
				}
				else
				{
					out.print("<div style='text-decoration:none;left:22px; position: absolute; top: 400px' >Congratulations!...you have guessed the number :)</div>");
				}
			}
		}
		%>
	</body>
</HTML>


