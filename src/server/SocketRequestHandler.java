package server;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

class SocketRequestHandler implements Runnable {

    private static final String CURRENT_DIRECTORY = System.getProperty("user.dir");

    private static final String CRLF = "" + (char) 0x0D + (char) 0x0A;

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private static final int SIZE = 2048;

    private static final int TIMEOUT_TIME = 10000;

    private static final String METHOD_GET = "GET";

    private static final String METHOD_HEAD = "HEAD";

    private static final int CODE_OK = 200;

    private static final int CODE_METHOD_NOT_ALLOWED = 405;

    private static final int CODE_NOT_FOUND = 404;

    private static final int CODE_FORBIDDEN = 403;

    private final Socket socket;

    private OutputStream outputStream;

    private InputStream inputStream;

    SocketRequestHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            socket.setSoTimeout(TIMEOUT_TIME);
        } catch (SocketException e) {
            System.out.println("Cant set timeout.");
            e.printStackTrace();
        }

        try (InputStream inputStreamTmp = socket.getInputStream()) {
            this.inputStream = inputStreamTmp;
            outputStream = socket.getOutputStream();

            String query = readRequest();

            processRequest(query);
        } catch (Exception e) {
            System.out.println("Cant handle request.");
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                } else {
                    socket.close();
                }
            } catch (IOException e) {
                System.out.println("Cant close outputStream.");
                e.printStackTrace();
            }
        }
    }

    private String readRequest() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder builder = new StringBuilder();

        while (true) {
            String line = reader.readLine();
            if (line == null || line.isEmpty()) {
                break;
            }

            builder.append(line).append(LINE_SEPARATOR);
        }

        return builder.toString();
    }

    private static String parseRequestMethod(String query) throws IllegalArgumentException {
        int to = query.indexOf(" ");
        if (to == -1) {
            throw new IllegalArgumentException("Cant parse method");
        }

        String method = query.substring(0, to);
        List<String> correctMethods = new ArrayList<>(
                Arrays.asList(METHOD_GET, "POST", "PUT", "DELETE", METHOD_HEAD, "OPTIONS")
        );

        if (!correctMethods.contains(method)) {
            throw new IllegalArgumentException("Cant parse method");
        }

        return method;
    }

    private void processRequest(String query) {
        String method = parseRequestMethod(query);
        String url = parseUrl(query);

        switch (method) {
            case METHOD_GET, METHOD_HEAD -> {
                sendFile(url, method);
            }
            default -> sendHeader(CODE_METHOD_NOT_ALLOWED, "", 0);
        }
    }

    private void sendHeader(int code, String mimeType, int size) {
        String header = createHeader(code, mimeType, size);
        PrintStream printStream = new PrintStream(outputStream, true, StandardCharsets.UTF_8);
        printStream.print(header);
    }

    private String parseUrl(String query) {
        int from = query.indexOf(" ") + 1; // Skip method type
        if (from == 0) {
            throw new IllegalArgumentException("Incorrect query. Cant skip method type");
        }
        int to = query.indexOf(" ", from);
        if (to == -1) {
            throw new IllegalArgumentException("Incorrect query. Cant parse url");
        }

        String url = java.net.URLDecoder.decode(query.substring(from, to), StandardCharsets.UTF_8);

        if (url.lastIndexOf("/") == url.length() - 1) {
            if (!url.contains(".")) {
                return CURRENT_DIRECTORY + url + "index.html";
            } else {
                sendHeader(CODE_NOT_FOUND, "", 0);
            }
        }

        int paramIndex = url.indexOf("?");
        if (paramIndex != -1) {
            url = url.substring(0, paramIndex);
        }

        if (processDoubleDotsInUrl(url)) {
            return null;
        }

        return CURRENT_DIRECTORY + url;
    }


    private void sendFile(String url, String methodType) {
        if (url == null) {
            sendHeader(CODE_FORBIDDEN, "", 0);
            return;
        }

        try {
            File file = new File(url);
            FileInputStream fileInputStream = new FileInputStream(file);

            String mimeType = getContentType(file);

            long size = file.length();

            sendHeader(CODE_OK, mimeType, (int) size);

            if (!methodType.equals(METHOD_HEAD)) {
                sendBody(fileInputStream);
                fileInputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            sendHeader(url.contains("/index.html") ? CODE_FORBIDDEN : CODE_NOT_FOUND, "", 0);
        }
    }

    private void sendBody(FileInputStream fileInputStream) throws IOException {
        int count;
        byte[] buffer = new byte[SIZE];

        while ((count = fileInputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, count);
        }
    }

    private static String createHeader(int code, String contentType, int contentLength) {
        StringBuilder buffer = new StringBuilder();

        buffer.append("HTTP/1.1 ").append(code).append(" ").append(getCodeDescription(code)).append(CRLF);
        buffer.append("Server: MyWebServer Java" + CRLF);
        buffer.append("Connection: close" + CRLF);
        buffer.append("Date: ").append(new Date()).append(CRLF);
        buffer.append("Accept-Ranges: none " + CRLF);

        if (code == CODE_OK) {
            if (contentType != null) {
                buffer.append("Content-Type: ").append(contentType).append(CRLF);
            }

            if (contentLength != 0) {
                buffer.append("Content-Length: ").append(contentLength).append(CRLF);
            }
        }

        buffer.append(CRLF);
        return buffer.toString();
    }

    private static String getContentType(File file) throws IOException {
        int startIndex = file.getPath().lastIndexOf('.');

        if (startIndex > 0) {
            String needToCheckMimeType = file.getPath().substring(startIndex + 1);
            if (needToCheckMimeType.equals("swf")) {
                return "application/x-shockwave-flash";
            } else if (needToCheckMimeType.equals("css")) {
                return "text/css";
            } else if (needToCheckMimeType.equals("js")) {
                return "text/javascript";
            }
        }

        return Files.probeContentType(file.toPath());
    }

    private static Boolean processDoubleDotsInUrl(String url) {
        int doubleDotsCount = replaceDoubleDotsInUrl(url, "/..");

        if (doubleDotsCount > 0) {
            return replaceDoubleDotsInUrl(url, "/") - 2 * doubleDotsCount < 0;
        }

        return false;
    }

    private static int replaceDoubleDotsInUrl(String url, String doubleDots) {
        int doubleDotsCount = 0;
        while (url.contains(doubleDots)) {
            url = url.replaceFirst(doubleDots, "");
            doubleDotsCount++;
        }

        return doubleDotsCount;
    }

    private static String getCodeDescription(int code) {
        switch (code) {
            case CODE_OK:
                return "OK";
            case CODE_METHOD_NOT_ALLOWED:
                return "Method not allowed";
            case CODE_NOT_FOUND:
                return "Not Found";
            case CODE_FORBIDDEN:
                return "Forbidden";
            default:
                return "Internal Server Error";
        }
    }
}
