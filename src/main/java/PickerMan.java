import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PickerMan {
    /**
     * The waiting time after each operation,
     * increase it if the result does not match to the UI list
     */
    public static final long SHORT_WAITING = 800L;

    /**
     * The waiting time between 2 dropdown list
     */
    public static final long LONG_WAITING = 2000L;

    public static void main(String[] args) throws Exception {
        ChromeOptions options = new ChromeOptions();

        // 如果不想看到浏览器窗口，则需要加上这个浏览器启动参数
        // options.addArguments("--headless");

        RemoteWebDriver driver = new ChromeDriver(options);

        WebDriverWait wait = new WebDriverWait(driver, 30);

        try {
            // 从被控浏览器加载指定页面
            // 注意：如果页面实现发生变化，下列所有代码则需要调整
            driver.get("https://exchange.pancakeswap.finance/?_gl=1*powthj*_ga*NzM1NzUxNzExLjE2MTM0MzM0OTg.*_ga_334KNG3DMQ*MTYxMzcwNDM4OS44LjAuMTYxMzcwNDM4OS4w#/swap");

            // 等待 from 和 to 按钮被渲染
            By buttonSelector = By.cssSelector("button.open-currency-select-button");
            wait.until(drv -> driver.findElements(buttonSelector).size() >= 2);

            // 等待 from 和 to 按钮被浏览器显示出来
            List<WebElement> buttons = driver.findElements(buttonSelector);
            wait.until(ExpectedConditions.visibilityOfAllElements(buttons));

            WebElement buttonFrom = buttons.get(0);
            WebElement buttonTo = buttons.get(1);

            if (args.length > 0 && "from".equalsIgnoreCase(args[0])) {
                Set<String> tokensOfFrom = pickDropdownList(driver, wait, buttonFrom);
                printTokens(tokensOfFrom, null);
            } else if (args.length > 0 && "to".equalsIgnoreCase(args[0])) {
                Set<String> tokensOfTo = pickDropdownList(driver, wait, buttonTo);
                printTokens(tokensOfTo, null);
            } else {
                Set<String> tokensOfFrom = pickDropdownList(driver, wait, buttonFrom);
                printTokens(tokensOfFrom, "---from---");

                Thread.sleep(2000);

                Set<String> tokensOfTo = pickDropdownList(driver, wait, buttonTo);
                printTokens(tokensOfTo, "---to---");
            }
        } finally {
            driver.quit();
        }
    }

    public static void printTokens(Set<String> tokens, String header) {
        if (header != null) {
            System.out.println(header + tokens.size());
        }

        for (String t : tokens) {
            System.out.println(t);
        }
    }

    public static Set<String> pickDropdownList(
            RemoteWebDriver driver,
            WebDriverWait wait,
            WebElement dropdownButton) throws Exception {
        Thread.sleep(SHORT_WAITING);

        // 模拟点击下拉按钮，以打开下拉列表
        dropdownButton.click();
        Thread.sleep(SHORT_WAITING);

        // 等待下拉列表中的输入框显示出来
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("token-search-input")));

        // 获取列表中被渲染的的所有项
        By listItemSelector = By.cssSelector("img[src*=\"coins\"] + div div:first-child");
        List<WebElement> labels = driver.findElements(listItemSelector);

        // 获取第一个列表项
        WebElement firstLabel = labels.get(0);

        // 找到所在的可滚动容器
        WebElement scrollPanelOfFrom = firstLabel.findElement(By.xpath("../../../.."));

        // 用来保存下拉列表中的所有项
        Set<String> allTokens = new LinkedHashSet<>();

        long lastScrollTopOfFrom = 0;
        while (true) {
            Set<String> tokens = new LinkedHashSet<>();

            // 重新获取所有被渲染出来的下拉框中的列表项
            List<WebElement> tokenLabels = driver.findElements(listItemSelector);
            for (WebElement element : tokenLabels) {
                tokens.add(element.getText());
            }

            // 去掉以前已经读取的列表项
            tokens.removeAll(allTokens);
            // System.out.println("*** Found new items: " + tokens.size());
            allTokens.addAll(tokens);

            // 每次向下滚动200个像素
            driver.executeScript("arguments[0].scrollBy(0, arguments[1]);", scrollPanelOfFrom, 200);

            // 获取当前的滚动位置
            Thread.sleep(SHORT_WAITING);

            // 如果已经滚动到底
            Number scrollTop = (Number) driver.executeScript("return arguments[0].scrollTop;", scrollPanelOfFrom);
            // System.out.println("*** New scroll top: " + scrollTop);
            if (lastScrollTopOfFrom == scrollTop.longValue()) {
                break;
            }

            lastScrollTopOfFrom = scrollTop.longValue();
        }

        // 最后需要关闭弹出的下拉列表，模拟按下 Esc 键
        WebElement searchInput = driver.findElement(By.id("token-search-input"));
        searchInput.sendKeys(Keys.ESCAPE);

        // 等待下拉列表中的输入框消失掉
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("token-search-input")));

        return allTokens;
    }
}
