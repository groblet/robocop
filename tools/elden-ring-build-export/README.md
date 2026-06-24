# Elden Ring Build Export

Exports a build page from `er-inventory.nyasu.business` (or any similar
client-rendered build-planner site) to Markdown.

The build planner renders its content client-side, so a plain HTTP fetch
returns an empty shell. This script uses a real headless browser (Playwright)
to render the page first, then converts the rendered content to Markdown.

## Setup

```bash
cd tools/elden-ring-build-export
npm install
npx playwright install chromium
```

## Usage

```bash
node extract-build.mjs "https://er-inventory.nyasu.business/?b=60df7e105f430b" --out ./output
```

Pass multiple URLs to export several builds/pages in one run:

```bash
node extract-build.mjs \
  "https://er-inventory.nyasu.business/loadsave" \
  "https://er-inventory.nyasu.business/?b=60df7e105f430b" \
  --out ./output
```

Output files are named after the build id (`build-<id>.md`) when the URL has
a `?b=` query param, or a slug of the page title otherwise, and are written
to `--out` (default `./output`).

## Notes / limitations

- This is a generic "render then convert to Markdown" approach rather than a
  hand-tuned scraper, since the site's exact DOM structure wasn't available
  to inspect when this was written. The output will include whatever text is
  visible on the rendered page (stats, equipment, talismans, spells, etc.),
  but formatting fidelity (e.g. table layout for stats) depends on how the
  site structures its HTML.
- If the site shows a cookie-consent banner, the script makes a best-effort
  attempt to dismiss it before extracting content.
- If output looks incomplete, try increasing the `waitForTimeout` in
  `extract-build.mjs` to give the page more time to finish rendering.
