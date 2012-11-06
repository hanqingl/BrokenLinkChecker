package com.github.hanqingl.brokenLinkChecker.test;

import com.github.hanqingl.brokenLinkChecker.LinkChecker;

public class Main {
    public static final String filePath =
            "/Users/hanqingliu/Documents/Code/2012Fall/" +
                    "18649/cmu_649/portfolio_template/portfolio.html";

    public static void main(String[] args) {
        new LinkChecker().checkLinks(filePath);
    }
}
