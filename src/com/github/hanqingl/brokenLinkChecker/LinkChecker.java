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
    private static final long HEART_BEET_RATE = 500;
    private static final int DEFAULT_POOL_SIZE = 50;
    private final ExecutorService pool;
    private boolean isActive;

    /**
     * Creates a LinkChecker with default number of threads (50)
     */
    public LinkChecker() {
        this(DEFAULT_POOL_SIZE);
    }

    /**
     * Creates a LinkChecker with at most nThreads
     * 
     * @param nThreads
     *            the number of threads in the pool
     * @throws IllegalArgumentException
     *             if {@code nThreads <= 0}
     */
    public LinkChecker(int nThreads) {
        pool = Executors.newFixedThreadPool(nThreads);
    }

    /**
     * Start checking links in a file. This method will also check
     * all links in this file's linked files
     * 
     * @param filePath
     *            root file path
     */
    public void checkLinks(String filePath) {
        setIsActive(true);
        pool.execute(new LinkCheckerHandler(filePath));

        while (isActive()) {
            setIsActive(false);
            try {
                Thread.sleep(HEART_BEET_RATE);
            } catch (InterruptedException e) {
                System.err.println("main thread interrupted");
                pool.shutdown();
                return;
            }
        }

        pool.shutdown();
    }

    private class LinkCheckerHandler implements Runnable {
        String filePath;

        LinkCheckerHandler(String filePath) {
            this.filePath = filePath;
        }

        @Override
        public void run() {
            try {
                // System.out.println("Checking file " + filePath);
                String content = getFileContent(filePath);

                List<String> linkURLs = extractLinkURL(content);

                int index = filePath.lastIndexOf('/');
                String parentDir = filePath.substring(0, index + 1);

                for (String url : linkURLs) {
                    setIsActive(true);
                    pool.execute(new LinkCheckerHandler(parentDir + url));
                }
            } catch (IOException e) {
                System.err.println("Broken Link: " + filePath);
                // throw new BrokenLinkException("File " + filePath + " not found");
                return;
            }
        }

    }

    private synchronized boolean isActive() {
        return isActive;
    }

    private synchronized void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    private String getFileContent(String fileName) throws IOException {
        File file = new File(fileName);

        long len = file.length();
        if (len > Integer.MAX_VALUE) {
            System.err.println("File " + fileName + " is too large, skip");
            return "";
        }

        FileInputStream fis = new FileInputStream(file);
        byte[] b = new byte[(int) len];
        try {
            fis.read(b);
        }
        finally {
            if (fis != null)
                fis.close();
        }
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
