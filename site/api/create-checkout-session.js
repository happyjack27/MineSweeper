/* ============================================================
   Optional: real card payments via Stripe Checkout.
   Only needed if you set checkoutMode: "stripe" in config.js.

   Works as-is on Vercel (put this file at /api/) or Netlify
   (see the note at the bottom). It never trusts prices sent by
   the browser — the cart only sends ids and quantities, and the
   price is looked up here on the server.

   Setup:
     1. npm install stripe
     2. Set the env var STRIPE_SECRET_KEY on your host
     3. Deploy. That's it.
   ============================================================ */

const Stripe = require('stripe');
const stripe = new Stripe(process.env.STRIPE_SECRET_KEY);

/* Prices in cents. These must match site/assets/js/config.js —
   the browser copy is for display, this copy is what you charge. */
const CATALOG = {
  original: { name: 'Original Sweet & Smoke',  cents: 1100 },
  gold:     { name: 'Carolina Gold Mustard',   cents: 1100 },
  ember:    { name: 'Ember Habanero',          cents: 1250 },
  vinegar:  { name: 'Eastern Vinegar Mop',     cents: 1000 },
  coffee:   { name: 'Black Coffee Bourbon',    cents: 1350 },
  sampler:  { name: 'The Whole Shelf (5 × 8 oz)', cents: 4800 }
};

const FLAT_SHIPPING_CENTS = 700;
const FREE_SHIPPING_OVER_CENTS = 4500;

module.exports = async function handler(req, res) {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).json({ error: 'POST only' });
  }

  try {
    const items = Array.isArray(req.body?.items) ? req.body.items : [];
    if (!items.length) return res.status(400).json({ error: 'Empty cart' });

    let subtotal = 0;
    const line_items = [];

    for (const item of items) {
      const product = CATALOG[item.id];
      const qty = Math.min(Math.max(parseInt(item.qty, 10) || 0, 1), 20);
      if (!product) return res.status(400).json({ error: `Unknown item: ${item.id}` });

      subtotal += product.cents * qty;
      line_items.push({
        quantity: qty,
        price_data: {
          currency: 'usd',
          unit_amount: product.cents,
          product_data: { name: product.name }
        }
      });
    }

    const shipping = subtotal >= FREE_SHIPPING_OVER_CENTS ? 0 : FLAT_SHIPPING_CENTS;
    const origin = req.headers.origin || `https://${req.headers.host}`;

    const session = await stripe.checkout.sessions.create({
      mode: 'payment',
      line_items,
      shipping_address_collection: { allowed_countries: ['US', 'CA'] },
      shipping_options: [{
        shipping_rate_data: {
          type: 'fixed_amount',
          display_name: shipping === 0 ? 'Free shipping' : 'Flat-rate shipping',
          fixed_amount: { amount: shipping, currency: 'usd' },
          delivery_estimate: {
            minimum: { unit: 'business_day', value: 3 },
            maximum: { unit: 'business_day', value: 7 }
          }
        }
      }],
      success_url: `${origin}/?paid=1`,
      cancel_url: `${origin}/#shop`
    });

    return res.status(200).json({ url: session.url });
  } catch (err) {
    console.error('checkout failed:', err);
    return res.status(500).json({ error: 'Could not start checkout' });
  }
};

/* Netlify note: rename to netlify/functions/create-checkout-session.js,
   export `exports.handler = async (event) => …`, parse JSON.parse(event.body),
   and return { statusCode, body: JSON.stringify({ url }) }. Then set
   checkoutEndpoint to "/.netlify/functions/create-checkout-session". */
