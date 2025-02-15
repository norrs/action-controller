package org.actioncontroller;

import org.actioncontroller.client.ApiClient;
import org.actioncontroller.client.ApiClientClassProxy;
import org.actioncontroller.client.HttpClientException;
import org.actioncontroller.client.HttpURLConnectionApiClient;
import org.actioncontroller.servlet.ApiServlet;
import org.actioncontroller.socket.SocketHttpClient;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.Test;
import org.slf4j.event.Level;

import javax.servlet.ServletContextEvent;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ApiClientProxyServletTest extends AbstractApiClientProxyTest {

    private final ApiServlet servlet = new ApiServlet(new TestController());

    @Override
    protected ApiClient createClient(TestController controller) throws Exception {
        Server server = new Server();
        ServletContextHandler handler = new ServletContextHandler();
        handler.setSessionHandler(new SessionHandler());
        handler.addEventListener(new javax.servlet.ServletContextListener() {
            @Override
            public void contextInitialized(ServletContextEvent event) {
                event.getServletContext().addServlet("testApi", servlet).addMapping("/api/*");
            }
        });
        handler.setContextPath("/test");
        server.setHandler(handler);
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(0);
        connector.setHost("localhost");
        server.addConnector(connector);
        server.start();

        return new HttpURLConnectionApiClient(server.getURI().toString() + "/api");
    }

    @Test
    public void gives404OnUnmappedController() {
        expectedLogEvents.expectMatch(m -> m
                .logger(ApiControllerRouteMap.class)
                .level(Level.INFO)
                .pattern("No route for {}. Routes {}"));
        UnmappedController unmappedController = ApiClientClassProxy.create(UnmappedController.class, apiClient);
        assertThatThrownBy(unmappedController::notHere)
                .isInstanceOf(HttpClientException.class)
                .satisfies(e -> assertThat(((HttpClientException)e).getStatusCode()).isEqualTo(404));
    }

    @Test
    public void shouldCalculateUrlWithoutHost() throws IOException {
        URL url = new URL(apiClient.getBaseUrl() + "/api/loginSession/endsession");

        Socket socket = new Socket("localhost", url.getPort());
        socket.getOutputStream().write(("GET /test/api/loginSession/endsession HTTP/1.0\r\n" +
                "Connection: close\r\n" +
                "\r\n").getBytes());

        String responseLine = SocketHttpClient.readLine(socket.getInputStream());
        assertThat(responseLine).startsWith("HTTP/1.1 302 ");
        assertThat(SocketHttpClient.readHttpHeaders(socket.getInputStream()))
                .containsEntry("location", singletonList("http://127.0.0.1:" + url.getPort() + "/test/frontPage"));
    }

    @Test
    public void shouldCalculateUrlWithDifferentHost() throws IOException {
        URL url = new URL(apiClient.getBaseUrl() + "/api/loginSession/endsession");

        Socket socket = new Socket("localhost", url.getPort());
        socket.getOutputStream().write(("GET /test/api/loginSession/endsession HTTP/1.0\r\n" +
                "Connection: close\r\n" +
                "Host: www.example.com:8080\r\n" +
                "\r\n").getBytes());

        String responseLine = SocketHttpClient.readLine(socket.getInputStream());
        assertThat(responseLine).startsWith("HTTP/1.1 302 ");
        assertThat(SocketHttpClient.readHttpHeaders(socket.getInputStream()))
                .containsEntry("location", singletonList("http://www.example.com:8080/test/frontPage"));
    }

    @Test
    public void shouldCalculateUrlWithProxyHost() throws IOException {
        URL url = new URL(apiClient.getBaseUrl() + "/api/loginSession/endsession");

        Socket socket = new Socket("localhost", url.getPort());
        socket.getOutputStream().write(("GET /test/api/loginSession/endsession HTTP/1.0\r\n" +
                "Connection: close\r\n" +
                "X-Forwarded-Proto: https\r\n" +
                "X-Forwarded-Host: www.example.com:8443\r\n" +
                "Host: app.example.com:8080\r\n" +
                "\r\n").getBytes());

        String responseLine = SocketHttpClient.readLine(socket.getInputStream());
        assertThat(responseLine).startsWith("HTTP/1.1 302 ");
        assertThat(SocketHttpClient.readHttpHeaders(socket.getInputStream()))
                .containsEntry("location", singletonList("https://www.example.com:8443/test/frontPage"));
    }

    @Override
    @Test
    public void shouldRethrowRuntimeExceptions() {
        expectedLogEvents.expect(
                HttpChannel.class,
                Level.WARN,
                "/test/api/someNiceMath",
                new ArithmeticException("/ by zero")
        );
        assertThatThrownBy(() -> controllerClient.divide(10, 0, false))
                .isInstanceOf(HttpClientException.class)
                .hasMessageContaining("Server Error");
    }

    private long count = 0;
    @Test
    public void shouldCountExecutions() {
        servlet.setTimerRegistry(name -> duration -> count++);
        controllerClient.first();
        assertThat(count).isEqualTo(1);
    }
}
