import puppeteer from 'puppeteer';
(async () => {
  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();
  await page.goto('http://localhost:5181');
  await new Promise(r => setTimeout(r, 2000));
  await page.screenshot({ path: 'screenshot2.png' });
  await browser.close();
  process.exit(0);
})();
