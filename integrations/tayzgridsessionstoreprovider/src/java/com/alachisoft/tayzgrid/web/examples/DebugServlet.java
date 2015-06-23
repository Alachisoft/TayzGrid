/*
* Copyright (c) 2015, Alachisoft. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.alachisoft.tayzgrid.web.examples;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class DebugServlet extends HttpServlet
{
    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String op = request.getParameter("operation");
        if (op == null)
        {
            op = "";
        }
        HttpSession session = null;
        String text = "<b>Welcome - Session Information </b><br>";
        if (op.equalsIgnoreCase("invalidate"))
        {
            session = request.getSession(false);
            if (session != null)
            {
                session.invalidate();
                text += "Session Invalidated " + new Date();
            }
        }
        else if (op.equalsIgnoreCase("create"))
        {
            session = request.getSession();
            text += "Session Created " + new Date();
        }
        text += "<br>";
        if (!op.equalsIgnoreCase("norequest"))
        {
            session = request.getSession(false);
        }
        if (op.equalsIgnoreCase("timeout") && session != null)
        {
            try
            {
                int val = Integer.parseInt(request.getParameter("value"));
                session.setMaxInactiveInterval(val);
            }
            catch (Exception ex)
            {
            }
        }
        text += "Now is " + new Date() + "<br>";
        if (session == null)
        {
            text += "No valid session associated with request";
        }
        else
        {
            text += "<p>";
            text += "Session ID:" + session.getId() + "<br>";
            text += "Creation Time:" + new Date(session.getCreationTime()) + "<br>";
            text += "Last Accessed:" + new Date(session.getLastAccessedTime()) + "<br>";
            text += "Is New :&nbsp;" + session.isNew() + "<br>";
            text += "Maximum Inactive Interval :&nbsp;" + session.getMaxInactiveInterval() + "<br>";
            text += "</p>";
        }
        try
        {
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet DebugServlet</title>");
            out.println("</head>");
            out.println("<body>");
            out.println(text);
            out.println("</body>");
            out.println("</html>");
        }
        finally
        {
            out.close();
        }
    }

    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    public String getServletInfo()
    {
        return "Short description";
    }
}
