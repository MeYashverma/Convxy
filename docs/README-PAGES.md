# GitHub Pages for the Convxy website

`docs/index.html` is the project website (terminal-style one-pager with live
GitHub data). It is fully static and self-contained — no build step.

## One-time enable (repo owner)

GitHub Pages must be switched on once in the repo settings:

1. Open **Settings → Pages** on `MeYashverma/Convxy`.
2. Under **Build and deployment → Source**, choose **Deploy from a branch**.
3. Branch: `main`, folder: `/docs`, then **Save**.
4. Wait ~1 minute; the site goes live at:

```
https://meyashverma.github.io/Convxy/
```

That's it — every push to `main` that touches `docs/` redeploys automatically.

## What the site pulls live

- `GET api.github.com/repos/MeYashverma/Convxy` → star count
- `GET .../releases/latest` → release tag + APK download button + download counts
- `GET .../commits?per_page=6` → commit feed
- `https://github.com/MeYashverma.png` → developer avatar (always current)

No secrets, no build, no maintenance: tagging a release or pushing a commit
updates the site on the next visit.

## Local preview

```bash
cd docs && python3 -m http.server 8000
# open http://localhost:8000
```
