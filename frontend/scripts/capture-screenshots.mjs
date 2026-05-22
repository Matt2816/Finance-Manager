import { chromium, devices } from "playwright";
import { mkdir } from "node:fs/promises";
import path from "node:path";

const BASE_URL = process.env.SCREENSHOT_BASE_URL ?? "http://127.0.0.1:3000";
const OUT_DIR =
  process.env.SCREENSHOT_OUT_DIR ?? "/opt/cursor/artifacts/screenshots";

const shots = [
  {
    name: "01-dashboard-desktop",
    viewport: { width: 1280, height: 900 },
    fullPage: true,
  },
  {
    name: "02-insights-desktop",
    viewport: { width: 1280, height: 900 },
    fullPage: false,
    clipSelector: "[data-screenshot='insights']",
    scrollY: 0,
  },
  {
    name: "03-heatmap-desktop",
    viewport: { width: 1280, height: 900 },
    fullPage: false,
    scrollTo: "[data-screenshot='heatmap']",
  },
  {
    name: "04-table-filters-desktop",
    viewport: { width: 1280, height: 900 },
    fullPage: false,
    scrollTo: "[data-screenshot='transactions']",
  },
  {
    name: "05-dashboard-mobile",
    device: "iPhone 13",
    fullPage: true,
  },
  {
    name: "06-mobile-cards",
    device: "iPhone 13",
    fullPage: false,
    scrollTo: "[data-screenshot='transactions']",
  },
  {
    name: "07-mobile-filters-dialog",
    device: "iPhone 13",
    action: async (page) => {
      await page.getByRole("button", { name: /filters/i }).click();
      await page.waitForTimeout(400);
    },
  },
];

async function waitForDashboard(page) {
  await page.goto(BASE_URL, { waitUntil: "networkidle" });
  await page.getByText("Spending insights").waitFor({ timeout: 60000 });
  await page.waitForTimeout(800);
  const heatmap = page.locator("[data-screenshot='heatmap'] circle").first();
  try {
    await heatmap.waitFor({ timeout: 45000 });
  } catch {
    // Heatmap may fail geocoding offline; continue with other shots
  }
  await page.waitForTimeout(500);
}

async function scrollToSelector(page, selector) {
  const el = page.locator(selector).first();
  await el.scrollIntoViewIfNeeded();
  await page.waitForTimeout(500);
}

async function main() {
  await mkdir(OUT_DIR, { recursive: true });
  const browser = await chromium.launch({ headless: true });

  for (const shot of shots) {
    const context = shot.device
      ? await browser.newContext({ ...devices[shot.device] })
      : await browser.newContext({ viewport: shot.viewport });
    const page = await context.newPage();

    try {
      await waitForDashboard(page);

      if (shot.action) {
        await shot.action(page);
      }
      if (shot.scrollTo) {
        await scrollToSelector(page, shot.scrollTo);
      }

      const filePath = path.join(OUT_DIR, `${shot.name}.png`);
      if (shot.clipSelector) {
        const el = page.locator(shot.clipSelector).first();
        await el.screenshot({ path: filePath });
      } else {
        await page.screenshot({
          path: filePath,
          fullPage: shot.fullPage ?? false,
        });
      }
      console.log(`Saved ${filePath}`);
    } finally {
      await context.close();
    }
  }

  await browser.close();
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
