import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailScrapingWebCrawler {
    //TODO blacklist touros shop and add multithreading

    private static final Queue<String> linksToVisitQueueBFS = new LinkedList<>();
    private static final Set<String> sitesVisited    = new HashSet<>();
    private static final Set<String> uniqueEmailSet    = Collections.synchronizedSet(new HashSet<>());
    private static final Map<String,String> emailSourceMap = Collections.synchronizedMap(new HashMap<>());

    private static final int PAGE_SUMMARY_INTERVAL = 50;
    private static final int SQL_INSERT_BATCH_SIZE = 100;
    private static final int TOTAL_EMAILS_REQUIRED = 10_000;

    private static final String INSERT_SQL = "INSERT INTO Emails (EmailAddress, Source, TimeStamp) VALUES (?, ?, ?)";

    private static final Pattern EMAIL_REGEX_PATTERN = Pattern.compile("(?U)\\b[\\p{L}0-9._%'+\\-]+@[\\p{L}0-9.\\-]+\\.[\\p{L}]{2,}\\b");

    private static final Logger logger = Logger.getLogger(EmailScrapingWebCrawler.class.getName());

    private int regexMatcherCounter = 0;
    private int mailToCount = 0;
    private int pagesCrawled = 0;


    public EmailScrapingWebCrawler(String startingPageURL){
        linksToVisitQueueBFS.add(startingPageURL);
        logger.info(() -> "Began crawler from: " + startingPageURL);
    }



    public static void main(String[] args) throws InterruptedException {
        Logger root = Logger.getLogger("");
        root.setLevel(Level.INFO);
        for (Handler h : root.getHandlers()) h.setLevel(Level.INFO);

        EmailScrapingWebCrawler scraper = new EmailScrapingWebCrawler("https://touro.edu");
        scraper.breadthFirstWebCrawler();

    }



    public void breadthFirstWebCrawler() throws InterruptedException {
        final int BATCH_INTERVAL = 50;
        while(!linksToVisitQueueBFS.isEmpty() && uniqueEmailSet.size() < TOTAL_EMAILS_REQUIRED){
            String url = linksToVisitQueueBFS.remove();
            sitesVisited.add(url);
            pagesCrawled++;
            logger.info(()-> "Crawling: " + url + " | Emails found so far: " + uniqueEmailSet.size());
            try{
                Document page = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36").timeout(10_000).get();

                Elements allAnchorLinkTags = page.getElementsByTag("a"); //<a> in html holds links in hrefs
                //extract alll links in one pass, emails and urls
                for(Element a: allAnchorLinkTags) { //each link is like a neighbor node in BFS algorithm
                    String href = a.absUrl("href"); //using a.absUrl() instead of a.attr() protects from relative links like /about_us etc.
                    if (href.startsWith("mailto:")) {
                        if(!href.isBlank()){
                            String email = href.substring("mailto:".length()).toLowerCase(Locale.ROOT); //canonical form is lower for us and uses locale for diff language emails
                            if(uniqueEmailSet.add(email)) {
                                mailToCount++;
                                emailSourceMap.put(email, url);
                                logger.fine(() -> "New Email Found: " + email + "that makes " + mailToCount + "scraped email catches");
                            }
                        }
                    } else if (href.startsWith("http")) {
                        if(sitesVisited.add(href)){// we dont wanna cycle the same sites
                            linksToVisitQueueBFS.add(href);
                            logger.fine(()-> "Link Added to Queue: "+href);
                        }
                    }
                }

                //second go over of page with plain regex (probably won't catch anything after 1st pass above)
                Matcher m = EMAIL_REGEX_PATTERN.matcher(page.text());
                while (m.find()){
                    String email = m.group().toLowerCase(Locale.ROOT);
                    if(uniqueEmailSet.add(email)) { //is it worth adding an if guard to add when ! contains or is set build to withstand that
                        regexMatcherCounter++;
                        emailSourceMap.put(email, url);
                        logger.fine(() -> "  Regex email caught: " + email + "that makes " + regexMatcherCounter + "regex email catches");
                    }
                }
            }catch (IOException e) {;
                logger.warning(() -> "\"Couldn't Connect to " + url + " due to: " + e.getMessage());
                Thread.sleep(200);
                continue; //if site cant connect notify user but keep scraping
            }
            if (pagesCrawled % PAGE_SUMMARY_INTERVAL == 0) {
                logger.info(() -> String.format(
                        "Processed %,d pages: mailto emails scraped =%d, regex matches scraped =%d, total unique emails=%d",
                        pagesCrawled, mailToCount, regexMatcherCounter, uniqueEmailSet.size()
                ));
                bulkInsertEmailsWithSQL();
            }
            Thread.sleep(200);
        }
        //TODO: Bulk writes: once you hit, say, every 100 emails, push them in batch to your SQL Server.
        logger.info(() -> "Crawl complete. Total unique emails: " + uniqueEmailSet.size());
//        logger.info(() -> "=== Final Email List ===");
//        uniqueEmailSet.forEach(e -> logger.info(() -> "  " + e));
    }


    //todo added for testing, maybe delete or make safer later(not our focus till after MVP)
    public Set<String> getUniqueEmailSet() {
        return Collections.unmodifiableSet(uniqueEmailSet);
    }
    public Set<String> getEmails() {
        return Collections.unmodifiableSet(uniqueEmailSet);
    }





    private static final String createTableIfMissing = """
            IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='Emails')
            CREATE TABLE Emails (
                     EmailID       INT IDENTITY(1,1)  PRIMARY KEY,
                     EmailAddress  NVARCHAR(255)       NOT NULL,
                     Source        NVARCHAR(512)       NOT NULL,
                     TimeStamp     DATETIME2           NOT NULL
                   );
            """;

    public void bulkInsertEmailsWithSQL(){
        // Retrieve sensitive data from environment variables
        Map<String, String> env = System.getenv();
        String endpoint = env.get("db_connection");
        String username = env.get("db_username");
        String password = env.get("db_password");

        String connectionUrl =
                "jdbc:sqlserver://" + endpoint + ";"
                        + "database=BENNY_BRECHER;"
                        + "user=" + username + ";"
                        + "password=" + password + ";"
                        + "encrypt=true;"
                        + "trustServerCertificate=true;"
                        + "loginTimeout=30;";

        try (Connection conn = DriverManager.getConnection(connectionUrl)) {
            conn.setAutoCommit(false);

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createTableIfMissing);
            }

            try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
                int count = 0;

                for (String email : uniqueEmailSet) {
                    ps.setString(1, email);
                    ps.setString(2, emailSourceMap.get(email));
                    ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                    ps.addBatch();

                    if (++count % SQL_INSERT_BATCH_SIZE == 0) {
                        ps.executeBatch();
                        conn.commit();
                    }
                }

                ps.executeBatch();
                conn.commit();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Could not open database connection", e);
            throw new RuntimeException("Database connection failure", e);
        }
    }
}
