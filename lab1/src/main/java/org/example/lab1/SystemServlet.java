package org.example.lab1;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "systemServlet", value = "/system")
public class SystemServlet extends HttpServlet {

    public void init() {
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");

        Runtime rt = Runtime.getRuntime();
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<p>CPU cores: " + rt.availableProcessors() + "</p>");
        out.println("<p>Total RAM: " + rt.totalMemory() / 1024 / 1024 + " MB</p>");
        out.println("<p>Free RAM: " + rt.freeMemory() / 1024 / 1024 + " MB</p>");
        out.println("<p>OS: " + System.getProperty("os.name") + "</p>");
        out.println("<p>Architecture: " + System.getProperty("os.arch") + "</p>");
        out.println("</body></html>");
    }

    public void destroy() {
    }
}
