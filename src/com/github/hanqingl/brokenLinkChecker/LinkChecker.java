package com.github.hanqingl.brokenLinkChecker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class LinkChecker {
    private final ExecutorService pool;
    private static final int DEFAULT_POOL_SIZE = 50;

    public LinkChecker() {
        this(DEFAULT_POOL_SIZE);
    }

    public LinkChecker(int poolSize) {
        pool = Executors.newFixedThreadPool(poolSize);
    }

    public void checkLinks(String filePath) {
        pool.execute(new LinkCheckerHandler(filePath));
    }

    private class LinkCheckerHandler implements Runnable {
        String filePath;

        LinkCheckerHandler(String filePath) {
            this.filePath = filePath;
        }

        @Override
        public void run() {
            try {
                String content = getFileContent(filePath);
                // System.out.println("File " + filePath + " checked");

                List<String> linkURLs = extractLinkURL(content);

                int index = filePath.lastIndexOf('/');
                String parentDir = filePath.substring(0, index + 1);

                for (String url : linkURLs) {
                    pool.execute(new LinkCheckerHandler(parentDir + url));
                }
            } catch (IOException e) {
                System.err.println("Broken Link: " + filePath);
                // throw new BrokenLinkException("File " + filePath + " not found");
            }
        }

    }

    private String getFileContent(String fileName) throws IOException {
        File file = new File(fileName);
        FileInputStream fis = new FileInputStream(file);
        byte[] b = new byte[(int) file.length()];
        fis.read(b);
        fis.close();
        return new String(b);
    }

    private List<String> extractLinkURL(String content) {
        List<String> list = new ArrayList<String>();
        Document doc = Jsoup.parse(content);
        Elements categories = doc.select("a");
        for (Element category : categories) {
            String link = category.attr("href");
            if (link.length() > 0 && !link.startsWith("#")) {
                list.add(category.attr("href"));
            }
        }
        return list;
    }

}
