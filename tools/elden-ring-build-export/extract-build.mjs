#!/usr/bin/env node
// Render an Elden Ring build-planner page (e.g. er-inventory.nyasu.business) with a
// real browser, since the build data is rendered client-side after page load, then
// convert the rendered content to Markdown.
//
// Usage:
//   node extract-build.mjs <url> [<url> ...] [--out <dir>]
//
// Example:
//   node extract-build.mjs "https://er-inventory.nyasu.business/?b=60df7e105f430b" --out ./output

import { chromium } from "playwright";
import TurndownService from "turndown";
import { gfm } from "turndown-plugin-gfm";
import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";

const NOISE_SELECTORS = [
  "script",
  "style",
  "noscript",
  "svg",
  "nav",
  "footer",
  "header[role='banner']",
  "[aria-hidden='true']",
];

// Common content roots for modern SPA frameworks, tried in order.
const ROOT_SELECTORS = ["main", "#__next", "#root", "#app", "body"];

function parseArgs(argv) {
  const urls = [];
  let outDir = "./output";
  for (let i = 0; i < argv.length; i++) {
    if (argv[i] === "--out") {
      outDir = argv[++i];
    } else {
      urls.push(argv[i]);
    }
  }
  return { urls, outDir };
}

function slugify(text, fallback) {
  const slug = (text || "")
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
  return slug || fallback;
}

async function dismissConsentBanners(page) {
  const labels = ["accept", "agree", "got it", "ok", "i understand", "close"];
  for (const label of labels) {
    try {
      const button = page.getByRole("button", { name: new RegExp(label, "i") }).first();
      if (await button.isVisible({ timeout: 1000 })) {
        await button.click({ timeout: 1000 });
        await page.waitForTimeout(300);
      }
    } catch {
      // No matching button — fine, keep going.
    }
  }
}

async function extractMarkdown(page, url) {
  await page.goto(url, { waitUntil: "networkidle", timeout: 60000 });
  await dismissConsentBanners(page);
  // Let client-side rendering / hydration settle.
  await page.waitForTimeout(1500);

  const { html, title } = await page.evaluate(
    ({ noiseSelectors, rootSelectors }) => {
      const clone = document.cloneNode(true);
      for (const sel of noiseSelectors) {
        clone.querySelectorAll(sel).forEach((el) => el.remove());
      }
      let root = null;
      for (const sel of rootSelectors) {
        root = clone.querySelector(sel);
        if (root && root.textContent.trim().length > 50) break;
      }
      root = root || clone.body;
      return { html: root.innerHTML, title: document.title };
    },
    { noiseSelectors: NOISE_SELECTORS, rootSelectors: ROOT_SELECTORS }
  );

  const turndown = new TurndownService({
    headingStyle: "atx",
    bulletListMarker: "-",
    codeBlockStyle: "fenced",
  });
  turndown.use(gfm);

  let markdown = turndown.turndown(html);
  // Collapse 3+ blank lines into 1.
  markdown = markdown.replace(/\n{3,}/g, "\n\n").trim();

  const header = `# ${title || "Elden Ring Build"}\n\nSource: ${url}\n\n`;
  return { markdown: header + markdown + "\n", title };
}

async function main() {
  const { urls, outDir } = parseArgs(process.argv.slice(2));
  if (urls.length === 0) {
    console.error("Usage: node extract-build.mjs <url> [<url> ...] [--out <dir>]");
    process.exit(1);
  }

  await mkdir(outDir, { recursive: true });
  const browser = await chromium.launch();
  try {
    const page = await browser.newPage();
    for (const url of urls) {
      console.log(`Fetching ${url} ...`);
      const { markdown, title } = await extractMarkdown(page, url);

      const buildIdMatch = url.match(/[?&]b=([^&]+)/);
      const slug = buildIdMatch
        ? `build-${buildIdMatch[1]}`
        : slugify(title, "page");
      const outPath = path.join(outDir, `${slug}.md`);

      await writeFile(outPath, markdown, "utf8");
      console.log(`  -> wrote ${outPath}`);
    }
  } finally {
    await browser.close();
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
