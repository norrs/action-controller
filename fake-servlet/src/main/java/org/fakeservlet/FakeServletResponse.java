package org.fakeservlet;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * DANGER! Unfinished class! Implement methods as you go!
 */
public class FakeServletResponse implements HttpServletResponse {
    public static final Charset CHARSET = StandardCharsets.ISO_8859_1;
    private final FakeServletRequest request;
    private int statusCode = 200;
    private String statusMessage;
    private String contentType;
    private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final List<Cookie> cookies = new ArrayList<>();
    private String characterEncoding;

    public FakeServletResponse(FakeServletRequest request) {
        this.request = request;
    }

    @Override
    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    public List<String> getCookies(String name) {
        return cookies.stream()
                .filter(c -> c.getName().equals(name))
                .map(Cookie::getValue)
                .map(c -> URLDecoder.decode(c, CHARSET))
                .collect(Collectors.toList());
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    // TODO
    @Override
    public boolean containsHeader(String s) {
        throw unimplemented();
    }

    // TODO
    @Override
    public String encodeURL(String s) {
        return encodeUrl(s);
    }

    // TODO
    @Override
    public String encodeRedirectURL(String s) {
        return encodeRedirectUrl(s);
    }

    // TODO
    @Override
    public String encodeUrl(String s) {
        // TODO: Should include some session id stuff if needed
        return s;
    }

    // TODO
    @Override
    public String encodeRedirectUrl(String s) {
        return encodeURL(s);
    }

    @Override
    public void sendError(int sc, String msg) {
        setStatus(sc, msg);
    }

    @Override
    public void sendError(int sc) {
        sendError(sc, "Server Error");
    }

    @Override
    public void sendRedirect(String location) {
        statusCode = 302;

        // if location contains scheme, just return it
        // if location starts with / use request root url, append encodeRedirectUrl(location)
        // otherwise use request.getRequestURI() backtrack to "/" if needed and encodeRedirectUrl(location)
        if (location.startsWith("/")) {
            location = request.getAuthority() + request.getContextPath() + location;
        }

        setHeader("Location", encodeRedirectURL(location));
    }

    @Override
    public void setDateHeader(String name, long epochMillis) {
        setHeader(name, DateTimeFormatter.RFC_1123_DATE_TIME.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())));
    }

    @Override
    public void addDateHeader(String s, long l) {
        throw unimplemented();
    }

    @Override
    public void setHeader(String name, String value) {
        headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
    }

    // TODO
    @Override
    public void addHeader(String s, String s1) {
        throw unimplemented();
    }

    @Override
    public void setIntHeader(String name, int value) {
        setHeader(name, String.valueOf(value));
    }

    @Override
    public void addIntHeader(String s, int i) {
        throw unimplemented();
    }

    @Override
    public void setStatus(int sc) {
        setStatus(sc, null);
    }

    @Override
    public void setStatus(int sc, String reason) {
        this.statusCode = sc;
        this.statusMessage = reason;
    }

    @Override
    public int getStatus() {
        return statusCode;
    }

    @Override
    public String getHeader(String name) {
        return headers.containsKey(name) ? headers.get(name).get(0) : null;
    }

    // TODO
    @Override
    public List<String> getHeaders(String s) {
        return headers.getOrDefault(s, new ArrayList<>());
    }

    @Override
    public Collection<String> getHeaderNames() {
        throw unimplemented();
    }

    // TODO
    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final ServletOutputStream servletOutputStream = new ServletOutputStream() {
        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {

        }

        @Override
        public void write(int b) {
            outputStream.write(b);
        }
    };

    @Override
    public ServletOutputStream getOutputStream() {
        return servletOutputStream;
    }

    private final PrintWriter writer = new PrintWriter(servletOutputStream);

    @Override
    public PrintWriter getWriter() {
        return writer;
    }

    @Override
    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    private AssertionError unimplemented() {
        return new AssertionError("called unexpected method");
    }

    @Override
    public void setContentLength(int i) {
        setContentLengthLong(i);
    }

    @Override
    public void setContentLengthLong(long l) {
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public void setBufferSize(int i) {
        throw unimplemented();
    }

    @Override
    public int getBufferSize() {
        throw unimplemented();
    }

    // TODO
    @Override
    public void flushBuffer() {
        writer.flush();
    }

    @Override
    public void resetBuffer() {
        throw unimplemented();
    }

    @Override
    public boolean isCommitted() {
        throw unimplemented();
    }

    @Override
    public void reset() {
        throw unimplemented();
    }

    @Override
    public void setLocale(Locale locale) {
        throw unimplemented();
    }

    @Override
    public Locale getLocale() {
        throw unimplemented();
    }

    public void assertNoError() {
        if (statusCode >= 400) {
            throw new AssertionError("Unexpected error " + statusCode + " " + statusMessage);
        }
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public byte[] getBody() {
        flushBuffer();
        return outputStream.toByteArray();
    }

    public String getBodyString() {
        return new String(getBody());
    }

}
