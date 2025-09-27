package com.mhduiy.androidtoolsserver;

/**
 * 极简测试服务器
 */
public class TestServer {
    public static void main(String[] args) {
        System.out.println("Hello from TestServer");
        System.out.flush();

        // 只做最基本的操作，不使用任何可能有问题的API
        try {
            for (int i = 0; i < 5; i++) {
                System.out.println("Count: " + i);
                System.out.flush();
                Thread.sleep(1000);
            }
            System.out.println("Test completed successfully");
        } catch (Exception e) {
            System.err.println("Error: " + e);
            e.printStackTrace();
        }
    }
}
