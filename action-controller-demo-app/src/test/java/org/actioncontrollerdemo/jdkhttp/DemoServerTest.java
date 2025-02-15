package org.actioncontrollerdemo.jdkhttp;

import org.actioncontroller.client.ApiClientClassProxy;
import org.actioncontroller.client.HttpClientException;
import org.actioncontroller.client.HttpURLConnectionApiClient;
import org.actioncontrollerdemo.TestController;
import org.actioncontrollerdemo.UserController;
import org.jsonbuddy.JsonObject;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DemoServerTest {

    private DemoServer server;
    private HttpURLConnectionApiClient client;

    @Before
    public void setUp() throws Exception {
        server = new DemoServer();
        server.setServerPort(0);
        client = new HttpURLConnectionApiClient(server.getURL() + "/demo/api");
    }

    @Test
    public void shouldShowFrontPage() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(server.getURL()).openConnection();
        assertThat(connection.getResponseCode()).isEqualTo(200);
        StringWriter writer = new StringWriter();
        new BufferedReader(new InputStreamReader(connection.getInputStream())).transferTo(writer);
        assertThat(writer.toString())
                .contains("<h1>Hello World</h1>");
    }

    @Test
    public void shouldShowJson() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(server.getURL() + "/demo/openapi.yaml").openConnection();
        assertThat(connection.getResponseCode()).isEqualTo(200);
        StringWriter writer = new StringWriter();
        new BufferedReader(new InputStreamReader(connection.getInputStream())).transferTo(writer);
        assertThat(writer.toString())
                .contains("Swagger Petstore");
    }

    @Test
    public void shouldCallTestController() {
        TestController testController = ApiClientClassProxy.create(TestController.class, client);
        assertThat(testController.sayHello(Optional.of("Test")))
                .isEqualTo("Hello Test");
        assertThat(testController.getJson())
                .isEqualTo(new JsonObject().put("product", "Blåbærsyltetøy"));
    }

    @Test
    public void shouldCallUpdater() {
        AtomicBoolean called = new AtomicBoolean(false);
        server.setUpdater(() -> called.set(true));

        TestController testController = ApiClientClassProxy.create(TestController.class, client);
        testController.update();
        assertThat(called).isTrue();
    }

    @Test
    public void shouldPermitOptionalUser() {
        UserController userApi = ApiClientClassProxy.create(UserController.class, client);
        assertThat(userApi.getUsername(Optional.empty())).isEqualTo("Hello, stranger");
    }

    @Test
    public void shouldAuthenticateUser() {
        UserController userApi = ApiClientClassProxy.create(UserController.class, client);
        userApi.postLogin("john doe", Optional.empty(), new AtomicReference<String>()::set);
        assertThat(userApi.getRealUsername(null)).isEqualTo("Hello - required, john doe");
    }

    @Test
    public void shouldRejectNonAdminUser() {
        UserController userApi = ApiClientClassProxy.create(UserController.class, client);
        userApi.postLogin("john doe", Optional.empty(), new AtomicReference<String>()::set);
        assertThatThrownBy(() -> userApi.getAdminPage(null))
                .isInstanceOf(HttpClientException.class)
                .hasMessageContaining("Forbidden");
    }

    @Test
    public void shouldShowLoginForm() {
        UserController userApi = ApiClientClassProxy.create(UserController.class, client);
        String response = userApi.getLogin(Optional.of("http://example.com"));
        assertThat(response)
                .contains("<input type='hidden' name='redirectAfterLogin' value='http://example.com' />");
    }

    @Test
    public void shouldAuthenticateAdminUser() {
        UserController userApi = ApiClientClassProxy.create(UserController.class, client);
        userApi.postLogin("admin", Optional.empty(), new AtomicReference<String>()::set);
        assertThat(userApi.getAdminPage(null)).isEqualTo("Hello - boss, admin");
    }

    @Test
    public void shouldChangePort() throws IOException {
        String initialUrl = server.getURL();
        server.setServerPort(0);
        assertThat(initialUrl).isNotEqualTo(server.getURL());
        HttpURLConnection connection = (HttpURLConnection) new URL(server.getURL()).openConnection();
        assertThat(connection.getResponseCode()).isEqualTo(200);
    }


}
