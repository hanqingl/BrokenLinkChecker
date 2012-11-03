package com.github.hanqingl.brokenLinkChecker;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class LinkChecker {
    private interface OnLinkCheckCompletedListener {
        void linkCheckCompleted(String[] subLinks);
    }

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

                System.out.println("File " + filePath + " checked");

                String[] linkURLs = extractLinkURL(content);

                int index = filePath.lastIndexOf('/');
                String parentDir = filePath.substring(0, index + 1);

                for (String url : linkURLs) {
                    pool.execute(new LinkCheckerHandler(parentDir + url));
                }
            } catch (FileNotFoundException e) {
                System.err.println("checking " + filePath);
                // throw new BrokenLinkException("File " + filePath + " not found");
            }
        }

    }

    private String getFileContent(String fileName) throws FileNotFoundException {
        StringBuilder sb = new StringBuilder();
        Reader reader = null;
        reader = new FileReader(fileName);

        BufferedReader br = new BufferedReader(reader);

        int c;
        try {
            while ((c = br.read()) != -1) {
                sb.append((char) c);
            }
            br.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return sb.toString();
    }

    private String[] extractLinkURL(String content) {
        List<String> list = new ArrayList<String>();
        Document doc = Jsoup.parse(content);
        Elements categories = doc.select("a");
        for (Element category : categories) {
            list.add(category.attr("href"));
        }
        return list.toArray(new String[list.size()]);
    }

}
