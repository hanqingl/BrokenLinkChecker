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
    private static final int DEFAULT_POOL_SIZE = 50;
    private final ExecutorService pool;
    private int totalTask = 0;
    private int finishedTask = 0;

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
        increaseTotalTask();
        pool.execute(new LinkCheckerHandler(filePath));

        synchronized (this) {
            try {
                // wait until a worker thread notify
                // the main thread that all tasks are done
                wait();
            } catch (InterruptedException e) {
                System.err.println("main thread interrupted");
            }
        }

        System.out.println("Total link checked: " + totalTask);
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
                if (index == -1) {
                    System.err.println("Bad file path (no '/' found)");
                    return;
                }

                String parentDir = filePath.substring(0, index + 1);

                for (String url : linkURLs) {
                    increaseTotalTask();
                    pool.execute(new LinkCheckerHandler(parentDir + url));
                }
            } catch (IOException e) {
                System.err.println("Broken Link: " + filePath);
                // throw new BrokenLinkException("File " + filePath + " not found");
            }

            increaseFinishedTask();
            notifyIfDone();
        }
    }

    private synchronized void increaseTotalTask() {
        totalTask++;
    }

    private synchronized void increaseFinishedTask() {
        finishedTask++;
    }

    /**
     * Notify the main thread which is waiting that all tasks are done
     */
    private synchronized void notifyIfDone() {
        if (totalTask == finishedTask) {
            notify();
        }
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
            if (fis != null) {
                fis.close();
            }
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
