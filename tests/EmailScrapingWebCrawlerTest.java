import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class EmailScrapingWebCrawlerTest {
    private static HttpServer server;
    private static int port;
    private static Path dir;
    private static String page1Url, page2Url;

    @BeforeAll
    static void setupFilesAndServer() throws Exception {
        // 1) Create two temp HTML files
        dir = Files.createTempDirectory("crawlerTest");
        String p1 = """
            <html><body>
              <a href="mailto:foo@example.com">foo</a>
              <a href="page2.html">next</a>
            </body></html>
            """;
        String p2 = """
            <html><body>
              <a href="mailto:bar@example.com">bar</a>
            </body></html>
            """;
        Files.writeString(dir.resolve("page1.html"), p1);
        Files.writeString(dir.resolve("page2.html"), p2);

        // 2) Spin up a HttpServer on an ephemeral port
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        // a single handler that serves any file under our temp dir
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String path = exchange.getRequestURI().getPath().substring(1); // drop leading "/"
                Path file = dir.resolve(path);
                byte[] bytes = Files.readAllBytes(file);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();

        // 3) Build our test URLs
        page1Url = "http://localhost:" + port + "/page1.html";
        page2Url = "http://localhost:" + port + "/page2.html";
    }

    @AfterAll
    static void tearDownServer() {
        server.stop(0);
    }

    @Test
    void crawlsTwoPagesAndFindsBothEmails() throws InterruptedException {
        // give the crawler our starting page1
        EmailScrapingWebCrawler crawler = new EmailScrapingWebCrawler(page1Url);
        crawler.breadthFirstWebCrawler();

        // now verify that it saw both foo@ and bar@
        var emails = crawler.getUniqueEmailSet(); // assume you have a getter
        assertTrue(emails.contains("foo@example.com"));
        assertTrue(emails.contains("bar@example.com"));
    }
}