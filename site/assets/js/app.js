/* ============================================================
   Storefront logic: renders the page from config.js, runs the
   cart, and hands off to checkout. No build step, no framework.
   ============================================================ */
(function () {
  'use strict';

  var S = window.SHOP;
  var BY_ID = {};
  S.products.forEach(function (p) { BY_ID[p.id] = p; });

  var money = function (n) { return S.currency + n.toFixed(2); };
  var el = function (id) { return document.getElementById(id); };

  /* ---------- Bottle illustration ------------------------ *
     Drawn, not photographed, so the site looks finished before
     any product photos exist. Set `photo` on a product in
     config.js and the photo is used instead.
   * ------------------------------------------------------- */
  function bottleSVG(color, w) {
    var h = Math.round(w * 2.6);
    return '<svg xmlns="http://www.w3.org/2000/svg" width="' + w + '" height="' + h + '" viewBox="0 0 80 208">' +
      '<defs>' +
        '<linearGradient id="g" x1="0" x2="1"><stop offset="0" stop-color="#000" stop-opacity=".28"/>' +
        '<stop offset=".28" stop-color="#fff" stop-opacity=".22"/><stop offset=".62" stop-color="#000" stop-opacity="0"/>' +
        '<stop offset="1" stop-color="#000" stop-opacity=".3"/></linearGradient>' +
      '</defs>' +
      /* glass body */
      '<path d="M27 30h26v14c0 6 11 14 11 30v118a10 10 0 0 1-10 10H26a10 10 0 0 1-10-10V74c0-16 11-24 11-30z" fill="#3b2b22"/>' +
      /* sauce */
      '<path d="M27 30h26v14c0 6 11 14 11 30v118a10 10 0 0 1-10 10H26a10 10 0 0 1-10-10V74c0-16 11-24 11-30z" fill="' + color + '"/>' +
      '<path d="M27 30h26v14c0 6 11 14 11 30v118a10 10 0 0 1-10 10H26a10 10 0 0 1-10-10V74c0-16 11-24 11-30z" fill="url(#g)"/>' +
      /* neck + cap */
      '<rect x="26" y="14" width="28" height="18" rx="3" fill="#2a211d"/>' +
      '<rect x="23" y="4" width="34" height="15" rx="4" fill="#1c1614"/>' +
      '<rect x="23" y="9" width="34" height="2" fill="#000" opacity=".35"/>' +
      /* label */
      '<rect x="13" y="96" width="54" height="66" rx="4" fill="#faf4ea"/>' +
      '<rect x="13" y="96" width="54" height="66" rx="4" fill="none" stroke="#000" stroke-opacity=".12"/>' +
      '<rect x="21" y="108" width="38" height="3" rx="1.5" fill="' + color + '"/>' +
      '<rect x="25" y="120" width="30" height="4" rx="2" fill="#33261f" opacity=".82"/>' +
      '<rect x="29" y="130" width="22" height="3" rx="1.5" fill="#33261f" opacity=".38"/>' +
      '<rect x="27" y="139" width="26" height="3" rx="1.5" fill="#33261f" opacity=".38"/>' +
      '<rect x="21" y="151" width="38" height="3" rx="1.5" fill="' + color + '"/>' +
      /* highlight */
      '<rect x="22" y="80" width="5" height="96" rx="2.5" fill="#fff" opacity=".16"/>' +
      '</svg>';
  }

  function bottleURI(color, w) {
    return 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(bottleSVG(color, w));
  }

  function pepper(on) {
    return '<svg class="pepper' + (on ? ' on' : '') + '" viewBox="0 0 24 24" fill="#c9481d" aria-hidden="true">' +
      '<path d="M13 3c0 2 1 3 3 3 3 0 4 3 4 6 0 5-5 10-10 10-4 0-7-2-7-5 0-1 1-2 2-2 4 0 8-4 8-9 0-1 0-2-1-3z"/></svg>';
  }

  /* ---------- Fill in the copy from config --------------- */
  function hydrate() {
    document.title = S.brand + ' ' + S.brandSuffix + ' — Small-batch barbecue sauce';
    [].forEach.call(document.querySelectorAll('[data-brand]'), function (n) { n.textContent = S.brand; });
    [].forEach.call(document.querySelectorAll('[data-brand-suffix]'), function (n) { n.textContent = S.brandSuffix; });
    [].forEach.call(document.querySelectorAll('[data-town]'), function (n) { n.textContent = S.town; });
    [].forEach.call(document.querySelectorAll('[data-tagline]'), function (n) { n.textContent = S.tagline; });
    [].forEach.call(document.querySelectorAll('[data-free-ship]'), function (n) {
      n.textContent = S.freeShippingOver ? money(S.freeShippingOver) : '—';
    });
    el('year').textContent = new Date().getFullYear();

    var mail = el('contactEmail');
    mail.href = 'mailto:' + S.email;
    mail.textContent = S.email;
    el('contactPhone').textContent = S.phone;
    if (S.instagram) {
      var ig = el('contactInsta');
      ig.href = 'https://instagram.com/' + S.instagram;
      ig.textContent = '@' + S.instagram;
      ig.hidden = false;
    }

    el('storyText').innerHTML = S.story.map(function (p) { return '<p>' + p + '</p>'; }).join('');

    el('faqList').innerHTML = S.faq.map(function (q, i) {
      return '<details' + (i === 0 ? ' open' : '') + '><summary>' + q[0] + '</summary><p>' + q[1] + '</p></details>';
    }).join('');

    // three bottles leaning on each other in the hero
    el('top').querySelector('.bottle-stack').innerHTML =
      [S.products[1], S.products[0], S.products[2]]
        .filter(Boolean)
        .map(function (p, i) { return bottleSVG(p.label, i === 1 ? 168 : 132); })
        .join('');
  }

  /* ---------- Product cards ------------------------------ */
  function renderProducts() {
    el('productGrid').innerHTML = S.products.map(function (p) {
      var out = p.stock === 0;
      var tag = out ? '<span class="tag">Sold out</span>'
        : p.featured ? '<span class="tag gold">Best value</span>'
        : p.stock <= 10 ? '<span class="tag low">Only ' + p.stock + ' left</span>' : '';

      var heat = '<div class="heat"><b>Heat</b>' +
        [0, 1, 2, 3].map(function (i) { return pepper(i < p.heat); }).join('') + '</div>';

      return '<article class="card' + (p.featured ? ' featured' : '') + '">' +
        '<div class="card-art">' + tag +
          '<img src="' + (p.photo || bottleURI(p.label, 70)) + '" alt="' + p.name + ' bottle" loading="lazy">' +
        '</div>' +
        '<div class="card-body">' +
          '<div class="card-title"><h3>' + p.name + '</h3><span class="price">' + money(p.price) + '</span></div>' +
          '<span class="size">' + p.size + '</span>' +
          heat +
          '<p class="blurb">' + p.blurb + '</p>' +
          '<ul class="notes">' + p.notes.map(function (n) { return '<li>' + n + '</li>'; }).join('') + '</ul>' +
          '<div class="card-cta">' +
            '<button class="add" data-add="' + p.id + '"' + (out ? ' disabled' : '') + '>' +
              (out ? 'Sold out' : 'Add to cart') + '</button>' +
          '</div>' +
        '</div></article>';
    }).join('');
  }

  /* ---------- Cart --------------------------------------- */
  var KEY = 'bbq-cart-v1';
  var cart = load();

  function load() {
    try {
      var raw = JSON.parse(localStorage.getItem(KEY) || '[]');
      // drop anything that is no longer a real product
      return raw.filter(function (l) { return BY_ID[l.id]; });
    } catch (e) { return []; }
  }
  function save() {
    try { localStorage.setItem(KEY, JSON.stringify(cart)); } catch (e) {}
  }

  function add(id) {
    var p = BY_ID[id];
    if (!p || p.stock === 0) return;
    var line = cart.filter(function (l) { return l.id === id; })[0];
    if (line) {
      if (line.qty >= p.stock) { toast('That’s all we have of ' + p.name + ' right now.'); return; }
      line.qty++;
    } else {
      cart.push({ id: id, qty: 1 });
    }
    save(); paint(); toast(p.name + ' added to cart');
  }

  function bump(id, delta) {
    var line = cart.filter(function (l) { return l.id === id; })[0];
    if (!line) return;
    var max = BY_ID[id].stock;
    line.qty += delta;
    if (line.qty > max) { line.qty = max; toast('Only ' + max + ' in stock'); }
    if (line.qty < 1) return remove(id);
    save(); paint();
  }

  function remove(id) {
    cart = cart.filter(function (l) { return l.id !== id; });
    save(); paint();
  }

  function totals() {
    var sub = cart.reduce(function (n, l) { return n + BY_ID[l.id].price * l.qty; }, 0);
    var free = S.freeShippingOver !== null && sub >= S.freeShippingOver;
    var ship = cart.length === 0 || free ? 0 : S.shippingFlat;
    return { sub: sub, ship: ship, free: free, total: sub + ship };
  }

  function paint() {
    var count = cart.reduce(function (n, l) { return n + l.qty; }, 0);
    var badge = el('cartCount');
    badge.textContent = count;
    badge.hidden = count === 0;

    var body = el('cartBody'), foot = el('cartFoot');

    if (!cart.length) {
      body.innerHTML = '<div class="cart-empty">' +
        '<svg width="46" height="46" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">' +
        '<path d="M6 7h12l-1 12H7L6 7zm3 0a3 3 0 0 1 6 0"/></svg>' +
        '<p>Nothing in here yet.</p></div>';
      foot.innerHTML = '';
      return;
    }

    body.innerHTML = cart.map(function (l) {
      var p = BY_ID[l.id];
      return '<div class="line">' +
        '<div class="line-art"><img src="' + (p.photo || bottleURI(p.label, 24)) + '" alt=""></div>' +
        '<div><h4>' + p.name + '</h4><small>' + p.size + ' · ' + money(p.price) + ' each</small>' +
          '<div class="qty">' +
            '<button data-dec="' + p.id + '" aria-label="One fewer ' + p.name + '">&minus;</button>' +
            '<span>' + l.qty + '</span>' +
            '<button data-inc="' + p.id + '" aria-label="One more ' + p.name + '">+</button>' +
          '</div></div>' +
        '<div class="line-right"><b>' + money(p.price * l.qty) + '</b>' +
          '<button class="rm" data-rm="' + p.id + '">Remove</button></div>' +
        '</div>';
    }).join('');

    var t = totals();
    var away = S.freeShippingOver !== null && !t.free ? S.freeShippingOver - t.sub : 0;

    foot.innerHTML =
      (away > 0 ? '<p class="ship-note">' + money(away) + ' more for free shipping.</p>' : '') +
      '<div class="totals">' +
        '<div><span>Subtotal</span><span>' + money(t.sub) + '</span></div>' +
        '<div><span>Shipping</span><span>' + (t.free ? 'Free' : money(t.ship)) + '</span></div>' +
        '<div class="grand"><span>Total</span><span>' + money(t.total) + '</span></div>' +
      '</div>' +
      '<button class="checkout" id="checkoutBtn">' +
        (S.checkoutMode === 'stripe' ? 'Checkout' : 'Place your order') + '</button>' +
      '<p class="fine">' + (S.checkoutMode === 'stripe'
        ? 'Secure payment by card. Taxes calculated at checkout.'
        : 'Opens an email with your order. We’ll reply with payment and shipping.') + '</p>';
  }

  /* ---------- Checkout ----------------------------------- */
  function checkout() {
    if (!cart.length) return;
    var t = totals();
    var lines = cart.map(function (l) {
      var p = BY_ID[l.id];
      return { id: p.id, name: p.name, size: p.size, price: p.price, qty: l.qty };
    });

    if (S.checkoutMode === 'stripe') return stripeCheckout(lines);

    /* inquiry mode — build an order email, no processor needed */
    var body = 'Hi ' + S.brand + ',\n\nI’d like to order:\n\n' +
      lines.map(function (l) {
        return '  ' + l.qty + ' × ' + l.name + ' (' + l.size + ') — ' + money(l.price * l.qty);
      }).join('\n') +
      '\n\n  Subtotal: ' + money(t.sub) +
      '\n  Shipping: ' + (t.free ? 'Free' : money(t.ship)) +
      '\n  Total:    ' + money(t.total) +
      '\n\nShip to:\n  Name:\n  Address:\n  City / State / ZIP:\n  Phone:\n\nThanks!';

    window.location.href = 'mailto:' + encodeURIComponent(S.email) +
      '?subject=' + encodeURIComponent('Sauce order — ' + money(t.total)) +
      '&body=' + encodeURIComponent(body);

    toast('Opening your email app with the order…');
  }

  function stripeCheckout(lines) {
    var btn = el('checkoutBtn');
    btn.disabled = true;
    btn.textContent = 'Taking you to checkout…';

    fetch(S.checkoutEndpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ items: lines.map(function (l) { return { id: l.id, qty: l.qty }; }) })
    })
      .then(function (r) {
        if (!r.ok) throw new Error('checkout ' + r.status);
        return r.json();
      })
      .then(function (data) {
        if (!data.url) throw new Error('no session url');
        window.location.href = data.url;
      })
      .catch(function (err) {
        console.error(err);
        btn.disabled = false;
        btn.textContent = 'Checkout';
        toast('Couldn’t reach checkout. Email ' + S.email + ' and we’ll sort it out.');
      });
  }

  /* ---------- Drawer + events ---------------------------- */
  var lastFocus = null;

  function openCart() {
    lastFocus = document.activeElement;
    el('cart').hidden = false;
    el('cartScrim').hidden = false;
    document.body.style.overflow = 'hidden';
    el('cartClose').focus();
  }
  function closeCart() {
    el('cart').hidden = true;
    el('cartScrim').hidden = true;
    document.body.style.overflow = '';
    if (lastFocus) lastFocus.focus();
  }

  var toastTimer;
  function toast(msg) {
    var t = el('toast');
    t.textContent = msg;
    t.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { t.classList.remove('show'); }, 2600);
  }

  document.addEventListener('click', function (e) {
    var t = e.target.closest('[data-add],[data-inc],[data-dec],[data-rm],#checkoutBtn');
    if (!t) return;
    if (t.id === 'checkoutBtn') return checkout();
    if (t.dataset.add) { add(t.dataset.add); return openCart(); }
    if (t.dataset.inc) return bump(t.dataset.inc, 1);
    if (t.dataset.dec) return bump(t.dataset.dec, -1);
    if (t.dataset.rm) return remove(t.dataset.rm);
  });

  el('cartBtn').addEventListener('click', openCart);
  el('cartClose').addEventListener('click', closeCart);
  el('cartScrim').addEventListener('click', closeCart);
  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && !el('cart').hidden) closeCart();
  });

  hydrate();
  renderProducts();
  paint();
})();
