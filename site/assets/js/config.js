/* ============================================================
   EDIT THIS FILE — everything about the shop lives here.
   You do not need to touch any other file to change the
   brand name, the sauces, the prices, or the contact details.
   ============================================================ */

window.SHOP = {

  /* ---- 1. The basics ------------------------------------ */
  brand: "Hollow Ridge",
  brandSuffix: "BBQ Co.",
  tagline: "Small-batch barbecue sauce, cooked one pot at a time.",
  email: "orders@example.com",
  phone: "(555) 013-2288",
  town: "Asheville, North Carolina",
  instagram: "",            // e.g. "hollowridgebbq" — leave "" to hide the link

  /* ---- 2. Shipping -------------------------------------- */
  currency: "$",
  shippingFlat: 7.00,       // flat rate per order
  freeShippingOver: 45.00,  // set to null to always charge shipping

  /* ---- 3. How checkout works ---------------------------- *
     "inquiry" — no payment processor needed. The cart builds an
                 order summary and opens the customer's email app
                 addressed to you. Works today, zero setup.
     "stripe"  — posts the cart to `checkoutEndpoint` which creates
                 a real Stripe Checkout session. See site/README.md
                 for the 10-minute setup.
   * ------------------------------------------------------- */
  checkoutMode: "inquiry",
  checkoutEndpoint: "/api/create-checkout-session",

  /* ---- 4. The sauces ------------------------------------ *
     id            unique, lowercase, no spaces
     heat          0-4, drawn as little peppers
     label         the bottle-label color on the illustration
     photo         optional: "assets/img/original.jpg" to use a real
                   photo instead of the drawn bottle
     stock         set to 0 to show "Sold out" and block adding
   * ------------------------------------------------------- */
  products: [
    {
      id: "original",
      name: "Original Sweet & Smoke",
      price: 11.00,
      size: "16 oz",
      heat: 1,
      label: "#c0392b",
      stock: 24,
      photo: "",
      blurb: "Molasses, cider vinegar, and a long slow smoke. The one that started it — good on everything, argued about by nobody.",
      notes: ["Molasses", "Cider vinegar", "Hickory smoke"]
    },
    {
      id: "gold",
      name: "Carolina Gold Mustard",
      price: 11.00,
      size: "16 oz",
      heat: 1,
      label: "#d9a021",
      stock: 18,
      photo: "",
      blurb: "Yellow mustard, brown sugar, a knuckle of black pepper. Tangy enough to cut through pulled pork and keep going.",
      notes: ["Yellow mustard", "Brown sugar", "Cracked pepper"]
    },
    {
      id: "ember",
      name: "Ember Habanero",
      price: 12.50,
      size: "16 oz",
      heat: 3,
      label: "#e05a1c",
      stock: 12,
      photo: "",
      blurb: "Charred habanero and roasted garlic. Hot, but it lets you taste the meat first — the heat arrives about a second later.",
      notes: ["Charred habanero", "Roasted garlic", "Lime"]
    },
    {
      id: "vinegar",
      name: "Eastern Vinegar Mop",
      price: 10.00,
      size: "16 oz",
      heat: 2,
      label: "#8e6b4a",
      stock: 20,
      photo: "",
      blurb: "Thin, sharp, and unapologetic. Traditional whole-hog mop sauce — vinegar, red pepper flake, and not much else.",
      notes: ["Cider vinegar", "Red pepper flake", "Salt"]
    },
    {
      id: "coffee",
      name: "Black Coffee Bourbon",
      price: 13.50,
      size: "16 oz",
      heat: 2,
      label: "#4a3728",
      stock: 9,
      photo: "",
      blurb: "Cold-brew coffee and a splash of bourbon cooked down dark. Deep and a little bitter in the right way. Brisket's best friend.",
      notes: ["Cold-brew coffee", "Bourbon", "Dark chili"]
    },
    {
      id: "sampler",
      name: "The Whole Shelf",
      price: 48.00,
      size: "5 × 8 oz",
      heat: 2,
      label: "#5b7553",
      stock: 6,
      photo: "",
      blurb: "One of each in half-size bottles, packed in a box with straw. The correct gift for anybody who owns a smoker.",
      notes: ["All five sauces", "Gift boxed", "Ships free"],
      featured: true
    }
  ],

  /* ---- 5. Front page copy ------------------------------- */
  story: [
    "It started with a kettle grill, a borrowed canning pot, and a recipe that took four summers to stop tinkering with.",
    "Every bottle is still cooked in batches of about forty, labeled by hand, and shipped from the same kitchen it was made in. If your order takes an extra day, it's because the sauce was still on the stove."
  ],

  faq: [
    ["How long does shipping take?",
     "Orders go out within two business days. Most arrive in three to five days after that. Everything ships flat-rate, and orders over $45 ship free."],
    ["Does it need to be refrigerated?",
     "Not until you open it. Sealed bottles keep about a year in the pantry. Once opened, refrigerate and use within three months."],
    ["What's in it?",
     "No high-fructose corn syrup, no artificial colors, and nothing you can't pronounce. Full ingredient lists are printed on every label."],
    ["Do you do wholesale or farmers markets?",
     "Yes to both, and yes to custom labels for weddings and events. Send an email and let's talk."]
  ]
};
