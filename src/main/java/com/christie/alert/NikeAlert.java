package com.christie.alert;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


public class NikeAlert {

    private static final String URL =
            "https://www.nike.com.hk/product/HQ4307-101/detail.htm";

    // 你而家最常用到會 trigger 嘅版本（已刪走售罄）
    private static final String XPATH =
            "//span[@onclick and (contains (text(),'加入購物車'))]|//*[@class='if-you-like-sold-out iyl-button' and @style != 'display:none']//span[contains (text(),'售罄') ]| //*[@class='if-you-like-qs iyl-button' and @style != 'display:none']//span[contains (text(),'結算')]";

    // TODO: 改成你自己嘅 Telegram Bot Token
    //private static final String BOT_TOKEN = "8602045465:AAGft6yHBJWjDdZXqyevq1kzXz5ItwOr5RU";
    private static final String BOT_TOKEN = System.getenv("BOT_TOKEN");

    // TODO: 改成你自己嘅 Telegram Chat ID
    //private static final String CHAT_ID = "33130039";
    private static final String CHAT_ID = System.getenv("CHAT_ID");

    // 防止同一狀態不停發通知
    //private static final Path STATE_FILE = Path.of("nike_alert_sent.txt");

    public static void main(String[] args) throws Exception {
        boolean found = elementExists(URL, XPATH);

        if (found) {
            String time = ZonedDateTime.now(ZoneId.of("Asia/Hong_Kong"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss '(UTC+8)'"));

            String msg = "🟢 Nike Alert\n"
                    + "🕒 " + time + "\n"
                    + "👟 HQ4307-101\n"
                    + "✅ 已偵測到目標 element\n"
                    + "🔗 " + URL;

            sendTelegram(msg);
            System.out.println("Element found -> Telegram sent.");
        } else {
            System.out.println("Element not found.");
        }
    }

    private static boolean elementExists(String url, String xpath) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(true)
                            .setSlowMo(300)
            );

            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            page.setDefaultNavigationTimeout(60000);
            page.setDefaultTimeout(30000);

            page.navigate(url, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            page.waitForTimeout(5000);

            //String xpathCart = "//span[@onclick and (contains (text(),'加入購物車'))]";
            String xpathCheckout = "//*[@class='if-you-like-qs iyl-button' and @style != 'display:none']//span[contains (text(),'結算')]";
            String xpathSoldOut = "//*[@class='if-you-like-sold-out iyl-button' and @style != 'display:none']//span[contains (text(),'售罄') ]";

            //int cartCount = page.locator("xpath=" + xpathCart).count();
            int checkoutCount = page.locator("xpath=" + xpathCheckout).count();
            int soldOutCount = page.locator("xpath=" + xpathSoldOut).count();

            //System.out.println("加入購物車 count = " + cartCount);
            System.out.println("結算 count = " + checkoutCount);
            System.out.println("售罄 count = " + soldOutCount);

            //boolean found = cartCount > 0;
            boolean found = soldOutCount > 0;

            browser.close();
            return found;
        }
    }

    private static void sendTelegram(String text) throws IOException, InterruptedException {
        String endpoint = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage";

        String body = "chat_id=" + encode(CHAT_ID)
                + "&text=" + encode(text)
                + "&disable_web_page_preview=true";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Telegram response: " + response.body());
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
