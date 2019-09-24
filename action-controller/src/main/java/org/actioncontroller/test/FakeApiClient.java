package org.actioncontroller.test;

import org.actioncontroller.client.ApiClient;
import org.actioncontroller.client.ApiClientExchange;
import org.actioncontroller.client.HttpClientException;
import org.fakeservlet.FakeHttpSession;
import org.fakeservlet.FakeServletRequest;
import org.fakeservlet.FakeServletResponse;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class FakeApiClient implements ApiClient {
    public static final Charset CHARSET = StandardCharsets.ISO_8859_1;
    private final URL contextRoot;
    private final String servletPath;
    private Servlet servlet;
    private FakeHttpSession session;
    private Map<String, Cookie> clientCookies = new HashMap<>();

    public FakeApiClient(URL contextRoot, String servletPath, Servlet servlet) {
        this.contextRoot = contextRoot;
        this.servletPath = servletPath;
        this.servlet = servlet;
    }

    public ApiClientExchange createExchange() {
        return new FakeApiClientExchange(contextRoot, servletPath);
    }

    private static boolean isUnexpired(Cookie c) {
        return c.getMaxAge() == -1 || c.getMaxAge() > 0;
    }

    private class FakeApiClientExchange implements ApiClientExchange {
        private FakeServletRequest request;

        private FakeServletResponse response = new FakeServletResponse();

        private FakeApiClientExchange(URL contextRoot, String servletPath) {
            List<Cookie> requestCookies = clientCookies.values().stream()
                    .filter(FakeApiClient::isUnexpired)
                    .collect(Collectors.toList());

            request = new FakeServletRequest("GET", contextRoot, servletPath, "/");
            request.setSession(session);
            request.setCookies(requestCookies);
        }

        @Override
        public void setTarget(String method, String pathInfo) {
            request.setMethod(method);
            int questionPos = pathInfo.indexOf('?');
            request.setPathInfo(questionPos == -1 ? pathInfo : pathInfo.substring(0, questionPos));
        }

        @Override
        public String getRequestMethod() {
            return request.getMethod();
        }

        @Override
        public String getPathInfo() {
            return request.getPathInfo();
        }

        @Override
        public void setPathInfo(String pathInfo) {
            request.setPathInfo(pathInfo);
        }

        @Override
        public URL getRequestURL() {
            try {
                return new URL(request.getRequestURL().toString());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void setRequestParameter(String name, Object value) {
            possiblyOptionalToString(value, s -> request.setParameter(name, s));
        }

        @Override
        public void addRequestCookie(String name, Object value) {
            possiblyOptionalToString(value, s -> request.setCookie(name, s));
        }

        @Override
        public void setHeader(String name, Object value) {
            possiblyOptionalToString(value, s -> request.setHeader(name, s));
        }

        private void possiblyOptionalToString(Object value, Consumer<String> consumer) {
            if (value instanceof Optional) {
                ((Optional)value).ifPresent(v -> consumer.accept(String.valueOf(v)));
            } else {
                consumer.accept(String.valueOf(value));
            }
        }

        @Override
        public void executeRequest() throws IOException {
            try {
                servlet.service(request, response);
            } catch (ServletException e) {
                throw new RuntimeException(e);
            }
            session = request.getSession(false);
            response.getCookies().forEach(c -> clientCookies.put(c.getName(), c));
        }

        @Override
        public int getResponseCode() {
            return response.getStatus();
        }

        @Override
        public void checkForError() throws HttpClientException {
            if (getResponseCode() >= 400) {
                throw new HttpClientException(getResponseCode(), response.getStatusMessage(), getResponseBody(), getRequestURL());
            }
        }

        @Override
        public String getResponseHeader(String name) {
            return response.getHeader(name);
        }

        @Override
        public String getResponseCookie(String name) {
            return URLDecoder.decode(response.getCookie(name), CHARSET);
        }

        @Override
        public String getResponseBody() {
            return response.getBody();
        }
    }
}
