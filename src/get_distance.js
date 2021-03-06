const
    args = process.argv.slice(2),
    from = args[0], to = args[1];
if (args.length < 2) {
    console.error("Must provide a start and end location.");
    return;
}
const
    webdriver = require('C:/Users/sina-_000/AppData/Roaming/npm/node_modules/selenium-webdriver'),
    browser = new webdriver.Builder().usingServer().forBrowser('internet explorer').build(),
    Key = webdriver.Key, By = webdriver.By,
    until = webdriver.until;
var by;

function waitForElement(elementCondition = by, timeout = 10000, client = browser) {
    return client.wait(until.elementLocated(elementCondition), timeout);
}

browser.get('http://bing.com/maps');
browser.sleep(3000);

by = By.className('directionsIcon realign');
waitForElement().then(icon => icon.click());

by = By.xpath("//input[@placeholder='From']");
waitForElement().then(box => {browser.sleep(500); box.sendKeys(from);});

by = By.xpath("//input[@placeholder='To']");
waitForElement().then(box => {browser.sleep(500); box.sendKeys(to, Key.ENTER);});

by = By.xpath("//td[@data-tag='descriptionDistance']");
waitForElement().then(promise => promise.getText().then(t => console.log(t)));
