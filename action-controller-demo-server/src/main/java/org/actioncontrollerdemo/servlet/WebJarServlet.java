package org.actioncontrollerdemo.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class WebJarServlet extends HttpServlet {
    private final String resourcePrefix;

    public WebJarServlet(String webJarName) {
        String prefix = "/META-INF/resources/webjars/" + webJarName;
        Properties properties = new Properties();
        try (InputStream pomProperties = getClass().getResourceAsStream("/META-INF/maven/org.webjars/" + webJarName + "/pom.properties")) {
            properties.load(pomProperties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.resourcePrefix = prefix + "/" + properties.get("version");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        URL resource = getClass().getResource(resourcePrefix + req.getPathInfo());
        if (resource == null) {
            resp.sendError(404, req.getPathInfo());
            return;
        }

        if (resource.getFile().endsWith("/")) {
            resource = new URL(resource, "index.html");
        }

        resp.setContentType(getServletContext().getMimeType(req.getPathInfo()));
        try (InputStream inputStream = resource.openStream()) {
            inputStream.transferTo(resp.getOutputStream());
        }
    }
}