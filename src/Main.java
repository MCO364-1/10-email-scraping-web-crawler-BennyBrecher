import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    /*
    public static void main(String[] args) {

        // Target: Extract email addresses from HTML <a href="mailto:..."> tags
        // Basic pattern: <a\s+href="mailto:(\w+@\w+\.(com|edu))">.*</a>

        // Improved pattern for subdomains:
        // <a\s+href="mailto:(\w+@(\w+\.)+?(com|edu))">.*</a>

        // Group 0: entire match
        // Group 1: email address (support@cloudresearch.com)
        // Group 3: TLD (com or edu)




        // 1. Define the regex pattern
        String regex = "<a\\s+href=\"mailto:(\\w+@\\w+\\.(com|edu))\">.*</a>";

        // 2. Compile the pattern (Factory Pattern)
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE); //Pattern.compile() uses the Factory Pattern - a static method that creates an instance of the class, rather than using a constructor directly with new.


        // 3. Create a matcher with the input string
        String input = "<a href=\"mailto:support@example.com\">Contact Support</a>";
        Matcher matcher = pattern.matcher(input);

        // 4. Find matches and process them
        while (matcher.find()) {
            // Group 0 is the entire match
            System.out.println("Full match: " + matcher.group(0));

            // Process all capturing groups
            for (int i = 1; i <= matcher.groupCount(); i++) {
                System.out.println("Group " + i + ": " + matcher.group(i));
            }
        }
        //(?i)href\\s*=\\s*\"mailto:([\\w.+\\-]+@[\\w\\-]+\\.[A-Za-z]{2,})\"
        }
 */

    public static void main(String[] args) {
        /*
        // Create a thread pool with an appropriate number of threads
        ExecutorService executor = Executors.newFixedThreadPool(100);

        // Queue up many web scraping tasks
        for (String url : urlsToScrape) {
            executor.submit(() -> {
                // IO-bound: Fetch the web page (thread will wait for network)
                String content = fetchWebPage(url);

                // CPU-bound: Process the content to extract data
                List extractedData = processContent(content);

                // IO-bound: Store results to database
                saveToDatabase(extractedData);

                return null;
            });
        }

        executor.shutdown();
         */
    }
}










/*
    Graph Traversal Strategies
The web can be viewed as a graph where pages are nodes and links are edges. There are two main strategies for traversing this graph:
    notes BFS/Dfs in their files
     */


    /* TODO Threadpool later after we have MVP: Minmal Viable Product:
    Thread Pool Management in Web Crawlers
Common Issues in Web Crawler Implementation
Creating too many threads or tasks can lead to OutOfMemoryError exceptions in web crawlers.
Identified Problem: Unbounded Thread Creation
The code was creating a new task for every link found without limiting the number of tasks queued in the executor service.
// Problematic code - creates unlimited tasks
while (emails.size() < numEmails) {
    executor.execute(new ThreadTask(queue.poll()));
    // This creates a new task for every link without any limit
}
Solution: Limiting Task Creation
// Better approach - limit the number of queued tasks
ExecutorService executor = Executors.newFixedThreadPool(10);
int maxQueuedTasks = 1000; // Limit pending tasks

while (emails.size() < numEmails) {
    // Only create new tasks when below the limit
    if (((ThreadPoolExecutor) executor).getQueue().size() < maxQueuedTasks) {
        String link = queue.poll();
        if (link != null) {
            executor.execute(new ThreadTask(link));
        }
    } else {
        // Wait a bit when the queue is full
        Thread.sleep(100);
    }
}
Debugging Tips
Importance of Logging
Always implement proper logging to gain visibility into your program's execution:
Use a logging framework like Log4J (recommended)
At minimum, use console logging redirected to a file
Log important state changes and error conditions
// Example of using Log4J for logging
import org.apache.log4j.Logger;

public class WebCrawler {
    private static final Logger logger = Logger.getLogger(WebCrawler.class);

    public void crawl(String url) {
        try {
            logger.info("Starting to crawl: " + url);
            // Crawling code
            logger.debug("Queue size: " + queue.size());
        } catch (Exception e) {
            logger.error("Error while crawling " + url, e);
        }
    }
}

     */
