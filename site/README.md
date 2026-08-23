# Barbecue sauce storefront

A small, self-contained shop site for a one-person sauce operation. Plain
HTML, CSS, and JavaScript — no build step, no npm install, no framework.
Open `index.html` in a browser and it works.

```
site/
├── index.html                     the page
├── assets/
│   ├── css/styles.css             all styling (colors at the top)
│   ├── js/config.js               ← the only file you normally edit
│   ├── js/app.js                  cart + rendering
│   └── img/                       drop product photos here
└── api/create-checkout-session.js optional Stripe function
```

## Changing things

Everything your friend will want to change is in **`assets/js/config.js`**:
brand name, tagline, town, email, phone, Instagram, shipping rates, and the
list of sauces (name, price, size, heat level, description, stock count).

Set a sauce's `stock` to `0` and the card shows "Sold out" and can't be added
to the cart. Ten or fewer shows an "Only N left" badge.

Site colors are the `:root` block at the top of `styles.css`.

## Product photos

The bottles on the page are drawn in code, so the site looks finished before
any photos exist. To use real ones, put the file in `assets/img/` and set the
product's `photo` field:

```js
photo: "assets/img/original.jpg",
```

Square-ish images on a plain background, around 800×800, look best.

## Taking money

Two modes, set by `checkoutMode` in `config.js`.

### `"inquiry"` — the default, zero setup

The cart totals the order and opens the customer's email app with an itemized
order addressed to your inbox, including a blank shipping address block. You
reply with a payment request (Venmo, PayPal, Zelle, an invoice — whatever you
already use) and ship it.

This is genuinely how a lot of small sauce makers operate for the first year.
It costs nothing, there's no processor account, and no card data ever touches
the site.

### `"stripe"` — real card checkout

1. Make a [Stripe](https://stripe.com) account.
2. Deploy the site somewhere that runs serverless functions — Vercel and
   Netlify both have free tiers that cover this.
3. `npm install stripe` and set the `STRIPE_SECRET_KEY` environment variable
   on the host.
4. Set `checkoutMode: "stripe"` in `config.js`.

`api/create-checkout-session.js` handles the rest. It keeps its own copy of
the prices, so a customer editing the page in their browser can't change what
they're charged — **if you change a price in `config.js`, change it in that
file too.**

Stripe collects the shipping address and emails a receipt. Orders show up in
the Stripe dashboard.

## Putting it online

The site is static files, so almost anything hosts it:

- **Netlify / Vercel** — drag the `site` folder onto their dashboard. Free,
  gives you HTTPS, and supports the Stripe function above.
- **GitHub Pages** — free, but static only, so `inquiry` mode only.
- **Any web host** — upload the contents of `site/` via FTP.

A domain (~$12/year from Namecheap, Porkbun, or Cloudflare) points at any of
these in a couple of minutes.

## Before it goes live

- [ ] Real brand name, email, phone, and town in `config.js`
- [ ] Real sauce names, prices, and descriptions
- [ ] Product photos, if you have them
- [ ] Decide on a checkout mode and test one order end to end
- [ ] Check it on a phone — most people will buy sauce on a phone

## Notes

Works in current Chrome, Safari, Firefox, and Edge, and on mobile. The cart
saves to the browser's local storage, so it survives a refresh. Keyboard
navigation and screen readers are handled; motion is reduced for anyone who
asks their system for that.
