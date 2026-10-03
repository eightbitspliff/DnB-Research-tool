#!/usr/bin/env sh
# Erzeugt index.html (komprimiert, für Browser und Android-App) aus game.html (lesbarer Quelltext).
# Benötigt Node.js; html-minifier-terser wird bei Bedarf über npx geladen.
set -e
cd "$(dirname "$0")"
{
  printf '<!doctype html>\n<html lang="de">\n<meta charset="utf-8">\n'
  printf '<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover,user-scalable=no">\n'
  cat game.html
} > index.src.html
npx --yes html-minifier-terser@7 --collapse-whitespace --remove-comments --minify-css true \
  --minify-js '{"compress":{"passes":2},"mangle":true}' -o index.html index.src.html
rm index.src.html
