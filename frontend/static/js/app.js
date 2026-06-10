/* =========================================================================
   Magazin Online — page logic
   Drives the header, storefront catalogue (filters + search), auth forms,
   and the role-aware account dashboard. All data comes from the REST API
   via api.js.
   ========================================================================= */

document.addEventListener("DOMContentLoaded", () => {
    initHeader();
    const page = document.body.dataset.page;
    if (page === "index") initCatalogue();
    if (page === "auth") initAuth();
    if (page === "dashboard") initDashboard();
});

/* ---------------- Header ---------------- */
function initHeader() {
    const slot = document.getElementById("navAuth");
    if (!slot) return;
    const s = Session.get();
    if (s) {
        slot.innerHTML = `
            <span class="whoami">${escapeHtml(s.email)} (${s.role.toLowerCase()})</span>
            <a href="/dashboard">My account</a>
            <button class="btn" id="signout">Sign out</button>`;
        slot.querySelector("#signout").onclick = () => { Session.clear(); location.href = "/"; };
    } else {
        slot.innerHTML = `<a href="/">Shop</a><a class="btn" href="/auth">Sign in</a>`;
    }
}

/* ---------------- Catalogue (index) ---------------- */
const catalogue = { all: [], filter: "all", query: "" };

async function initCatalogue() {
    const grid = document.getElementById("catalogue");
    grid.innerHTML = `<li class="count">Loading...</li>`;

    // Filter buttons
    document.querySelectorAll("#filters .chip").forEach(chip => {
        chip.onclick = () => {
            document.querySelectorAll("#filters .chip").forEach(c => c.classList.remove("active"));
            chip.classList.add("active");
            catalogue.filter = chip.dataset.filter;
            renderCatalogue();
        };
    });

    // Search box (lives in the top bar)
    const search = document.getElementById("searchInput");
    if (search) search.addEventListener("input", () => { catalogue.query = search.value.trim().toLowerCase(); renderCatalogue(); });

    try {
        catalogue.all = await Api.products();
        renderCatalogue();
    } catch (e) {
        grid.innerHTML = emptyState("Couldn't load the store", e.message);
    }
}

function renderCatalogue() {
    const grid = document.getElementById("catalogue");
    const count = document.getElementById("catCount");

    let items = catalogue.all;
    if (catalogue.filter !== "all") items = items.filter(p => p.saleType === catalogue.filter);
    if (catalogue.query) items = items.filter(p =>
        (p.name || "").toLowerCase().includes(catalogue.query) ||
        (p.description || "").toLowerCase().includes(catalogue.query));

    if (count) count.textContent = `${items.length} ${items.length === 1 ? "product" : "products"}`;

    if (!items.length) {
        grid.innerHTML = catalogue.all.length
            ? emptyState("No matches", "Try a different search or filter.")
            : emptyState("The store is empty", "No products are listed right now.");
        return;
    }
    grid.innerHTML = "";
    items.forEach(p => grid.appendChild(productCard(p)));
}

function productCard(p) {
    const isBuyer = Session.role === "BUYER";
    const negotiable = p.saleType === "NEGOTIABLE";
    const card = document.createElement("li");
    card.className = "product";

    let actions = "";
    if (isBuyer && negotiable) actions = `<button class="btn" data-act="offer">Make an offer</button>`;
    else if (isBuyer && !negotiable) actions = `<button class="btn" data-act="buy">Buy now</button>`;
    else if (!Session.get()) actions = `<a class="btn" href="/auth">Sign in to ${negotiable ? "offer" : "buy"}</a>`;

    card.innerHTML = `
        <span class="product__seller">Sold by ${escapeHtml(p.sellerEmail)}</span>
        <h3 class="product__name">${escapeHtml(p.name)}</h3>
        <p class="product__desc">${escapeHtml(p.description || "No description provided.")}</p>
        <div class="product__foot">
            <span>
                <span class="price">${money(p.price)}</span>
                <span class="tag">${negotiable ? "Negotiable" : "Fixed price"}</span>
            </span>
            ${actions}
        </div>`;

    const offerBtn = card.querySelector('[data-act="offer"]');
    const buyBtn = card.querySelector('[data-act="buy"]');
    if (offerBtn) offerBtn.onclick = () => makeOffer(p);
    if (buyBtn) buyBtn.onclick = () => buyNow(p);
    return card;
}

async function makeOffer(p) {
    const price = await priceModal("Make an offer", `On "${p.name}". Asking price ${Number(p.price).toFixed(2)} RON. Offers below the seller's private minimum are discarded.`, "Your offer (RON)");
    if (price == null) return;
    try {
        const r = await Api.submitOffer(p.reference, price);
        if (r.reference === null && r.status === "REJECTED") {
            toast("err", "Below minimum", "Your offer was below the seller's minimum and was not recorded.");
        } else {
            toast("ok", "Offer submitted", "Pending the seller's review. Track it in your account.");
        }
    } catch (e) { toast("err", "Couldn't submit", e.message); }
}

async function buyNow(p) {
    const ok = await confirmModal("Confirm purchase", `Buy "${p.name}" for ${Number(p.price).toFixed(2)} RON?`, "Buy now");
    if (!ok) return;
    try {
        await Api.purchase(p.reference);
        toast("ok", "Purchased", `"${p.name}" is yours. Your receipt is in your account.`);
        initCatalogue();
    } catch (e) { toast("err", "Purchase failed", e.message); }
}

/* ---------------- Auth ---------------- */
function initAuth() {
    const tabs = document.querySelectorAll(".tabs button");
    const loginForm = document.getElementById("loginForm");
    const regForm = document.getElementById("regForm");
    tabs.forEach(t => t.onclick = () => {
        tabs.forEach(x => x.classList.remove("active"));
        t.classList.add("active");
        const login = t.dataset.tab === "login";
        loginForm.style.display = login ? "" : "none";
        regForm.style.display = login ? "none" : "";
    });

    loginForm.onsubmit = async (e) => {
        e.preventDefault();
        const email = loginForm.email.value.trim(), password = loginForm.password.value;
        try {
            const s = await Api.login(email, password);
            Session.set(s);
            toast("ok", "Welcome back", `Signed in as ${s.role.toLowerCase()}.`);
            setTimeout(() => location.href = "/dashboard", 500);
        } catch (e) { toast("err", "Sign-in failed", e.message); }
    };

    regForm.onsubmit = async (e) => {
        e.preventDefault();
        const email = regForm.email.value.trim(), password = regForm.password.value;
        const role = regForm.querySelector('input[name="role"]:checked').value;
        try {
            if (role === "buyer") {
                await Api.registerBuyer(email, password);
                const s = await Api.login(email, password);
                Session.set(s);
                toast("ok", "Account created", "You're signed in. Happy shopping.");
                setTimeout(() => location.href = "/dashboard", 600);
            } else {
                await Api.registerSeller(email, password);
                toast("ok", "Request received", "An administrator must approve your seller account before you can list. You can sign in meanwhile.");
                document.querySelector('.tabs button[data-tab="login"]').click();
                loginForm.email.value = email;
            }
        } catch (e) { toast("err", "Couldn't register", e.message); }
    };
}

/* ---------------- Dashboard ---------------- */
function initDashboard() {
    const s = Session.get();
    if (!s) { location.href = "/auth"; return; }

    document.getElementById("dashWho").textContent = s.email;
    document.getElementById("dashRole").textContent = s.role.toLowerCase();

    const tabsEl = document.getElementById("dashTabs");
    const panel = document.getElementById("dashPanel");

    const views = {
        BUYER: [
            { id: "offers", label: "My offers", render: renderBuyerOffers },
            { id: "purchases", label: "My orders", render: renderBuyerPurchases },
        ],
        SELLER: [
            { id: "listings", label: "My listings", render: renderSellerListings },
            { id: "received", label: "Offers received", render: renderSellerOffers },
        ],
        ADMIN: [
            { id: "sellers", label: "Sellers", render: renderAdminSellers },
        ],
    }[s.role] || [];

    tabsEl.innerHTML = "";
    views.forEach((v, i) => {
        const b = document.createElement("button");
        b.textContent = v.label;
        if (i === 0) b.classList.add("active");
        b.onclick = () => {
            tabsEl.querySelectorAll("button").forEach(x => x.classList.remove("active"));
            b.classList.add("active");
            run(v);
        };
        tabsEl.appendChild(b);
    });
    if (views[0]) run(views[0]);

    async function run(v) {
        panel.innerHTML = `<p class="count">Loading...</p>`;
        try { await v.render(panel); }
        catch (e) {
            if (e.status === 403) panel.innerHTML = noticeBanner(e.message);
            else panel.innerHTML = emptyState("Something went wrong", e.message);
        }
    }
}

/* ----- Buyer panels ----- */
async function renderBuyerOffers(panel) {
    const offers = await Api.myOffers();
    if (!offers.length) return void (panel.innerHTML = emptyState("No offers yet", "Browse the store and make an offer on a negotiable product."));
    panel.innerHTML = `<ul class="list"></ul>`;
    const list = panel.querySelector(".list");
    offers.forEach(o => {
        const row = document.createElement("li");
        row.className = "row";
        const canBuy = o.status === "APPROVED";
        row.innerHTML = `
            <div>
                <span class="row__title">Offer · ${money(o.proposedPrice)}</span>
                <span class="row__meta">product ${shortRef(o.productReference)} · offer ${shortRef(o.reference)}</span>
            </div>
            <div class="row__actions">
                ${stamp(o.status)}
                ${canBuy ? `<button class="btn" data-buy="${o.productReference}">Complete purchase</button>` : ""}
            </div>`;
        const buy = row.querySelector("[data-buy]");
        if (buy) buy.onclick = async () => {
            try { await Api.purchase(o.productReference); toast("ok", "Purchased", "Your negotiated product is yours."); renderBuyerOffers(panel); }
            catch (e) { toast("err", "Purchase failed", e.message); }
        };
        list.appendChild(row);
    });
}

async function renderBuyerPurchases(panel) {
    const sales = await Api.myPurchases();
    if (!sales.length) return void (panel.innerHTML = emptyState("No orders yet", "Your receipts will appear here."));
    panel.innerHTML = `<ul class="list"></ul>`;
    const list = panel.querySelector(".list");
    sales.forEach(x => {
        const row = document.createElement("li");
        row.className = "row";
        row.innerHTML = `
            <div>
                <span class="row__title">${escapeHtml(x.productName)}</span>
                <span class="row__meta">${escapeHtml(x.productDescription || "")} · sold by ${escapeHtml(x.sellerEmail)} · ${fmtDate(x.soldAt)}</span>
            </div>
            <div class="row__actions"><span class="price">${money(x.finalPrice)}</span></div>`;
        list.appendChild(row);
    });
}

/* ----- Seller panels ----- */
async function renderSellerListings(panel) {
    const products = await Api.sellerProducts();
    panel.innerHTML = `
        <div class="toolbar">
            <span class="count">${products.length} active ${products.length === 1 ? "listing" : "listings"}</span>
            <button class="btn" id="newListing">+ New listing</button>
        </div>
        <ul class="list"></ul>`;
    panel.querySelector("#newListing").onclick = () => newListingModal(() => renderSellerListings(panel));
    const list = panel.querySelector(".list");
    if (!products.length) { list.innerHTML = emptyState("Nothing listed", "Add your first product to the store."); return; }
    products.forEach(p => {
        const negotiable = p.saleType === "NEGOTIABLE";
        const row = document.createElement("li");
        row.className = "row";
        row.innerHTML = `
            <div>
                <span class="row__title">${escapeHtml(p.name)}</span>
                <span class="row__meta">${shortRef(p.reference)} · ${escapeHtml(p.description || "no description")}</span>
            </div>
            <div class="row__actions">
                <span class="tag">${negotiable ? "negotiable" : "fixed"}</span>
                <span class="price">${money(p.price)}</span>
                <button class="btn btn--danger" data-cancel="${p.reference}">Remove</button>
            </div>`;
        row.querySelector("[data-cancel]").onclick = async () => {
            if (!await confirmModal("Remove listing", `Remove "${p.name}" from the store? Offers on it are discarded.`, "Remove listing")) return;
            try { await Api.cancelListing(p.reference); toast("ok", "Listing removed", `"${p.name}" is no longer for sale.`); renderSellerListings(panel); }
            catch (e) { toast("err", "Couldn't remove", e.message); }
        };
        list.appendChild(row);
    });
}

async function renderSellerOffers(panel) {
    const offers = await Api.sellerOffers();
    if (!offers.length) return void (panel.innerHTML = emptyState("No offers yet", "Offers on your negotiable products show up here."));
    panel.innerHTML = `<ul class="list"></ul>`;
    const list = panel.querySelector(".list");
    offers.forEach(o => {
        const pending = o.status === "PENDING";
        const row = document.createElement("li");
        row.className = "row";
        row.innerHTML = `
            <div>
                <span class="row__title">${money(o.proposedPrice)}</span>
                <span class="row__meta">from ${escapeHtml(o.buyerEmail)} · product ${shortRef(o.productReference)}</span>
            </div>
            <div class="row__actions">
                ${pending ? `
                    <button class="btn" data-approve="${o.reference}">Approve</button>
                    <button class="btn btn--danger" data-reject="${o.reference}">Reject</button>
                ` : stamp(o.status)}
            </div>`;
        const ap = row.querySelector("[data-approve]"), rj = row.querySelector("[data-reject]");
        if (ap) ap.onclick = () => decide(() => Api.approveOffer(o.reference), "Offer approved", panel);
        if (rj) rj.onclick = () => decide(() => Api.rejectOffer(o.reference), "Offer rejected", panel);
        list.appendChild(row);
    });
    async function decide(fn, msg, panel) {
        try { await fn(); toast("ok", msg, "The buyer can see the update."); renderSellerOffers(panel); }
        catch (e) { toast("err", "Action failed", e.message); }
    }
}

/* ----- Admin panel ----- */
async function renderAdminSellers(panel) {
    const sellers = await Api.sellers();
    if (!sellers.length) return void (panel.innerHTML = emptyState("No sellers yet", "Seller account requests will appear here."));
    panel.innerHTML = `<ul class="list"></ul>`;
    const list = panel.querySelector(".list");
    sellers.forEach(u => {
        const row = document.createElement("li");
        row.className = "row";
        const state = !u.active ? `<span class="stamp stamp--rejected">deactivated</span>`
            : u.approved ? `<span class="stamp stamp--approved">approved</span>`
            : `<span class="stamp stamp--pending">awaiting</span>`;
        row.innerHTML = `
            <div>
                <span class="row__title">${escapeHtml(u.email)}</span>
                <span class="row__meta">role ${u.role.toLowerCase()}</span>
            </div>
            <div class="row__actions">
                ${state}
                ${!u.approved && u.active ? `<button class="btn" data-approve>Approve</button>` : ""}
                ${u.active ? `<button class="btn btn--danger" data-deact>Deactivate</button>` : ""}
            </div>`;
        const ap = row.querySelector("[data-approve]"), de = row.querySelector("[data-deact]");
        if (ap) ap.onclick = () => adminDo(() => Api.approveSeller(u.email), "Seller approved", panel);
        if (de) de.onclick = async () => {
            if (!await confirmModal("Deactivate seller", `Block ${u.email} from signing in? Their record is kept.`, "Deactivate")) return;
            adminDo(() => Api.deactivateSeller(u.email), "Seller deactivated", panel);
        };
        list.appendChild(row);
    });
    async function adminDo(fn, msg, panel) {
        try { await fn(); toast("ok", msg, ""); renderAdminSellers(panel); }
        catch (e) { toast("err", "Action failed", e.message); }
    }
}

/* ---------------- Small view helpers ---------------- */
function stamp(status) {
    const cls = status === "APPROVED" ? "approved" : status === "REJECTED" ? "rejected" : "pending";
    return `<span class="stamp stamp--${cls}">${status.toLowerCase()}</span>`;
}
function emptyState(title, sub) { return `<div class="empty"><span class="empty__title">${escapeHtml(title)}</span>${escapeHtml(sub || "")}</div>`; }
function noticeBanner(msg) {
    return `<div class="notice"><b>Awaiting approval</b><p style="margin:.5rem 0 0">${escapeHtml(msg)}</p></div>`;
}
function fmtDate(s) { try { return new Date(s).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" }); } catch { return s; } }

/* ---------------- Modals ---------------- */
function showModal(inner, setup) {
    const back = document.createElement("div");
    back.className = "modal-back";
    back.innerHTML = `<div class="modal">${inner}</div>`;
    document.body.appendChild(back);
    const close = () => back.remove();
    back.addEventListener("click", e => { if (e.target === back) close(); });
    document.addEventListener("keydown", function esc(e) { if (e.key === "Escape") { close(); document.removeEventListener("keydown", esc); } });
    setup(back.querySelector(".modal"), close);
}

function priceModal(title, subtitle, label) {
    return new Promise(resolve => {
        showModal(`
            <h3>${escapeHtml(title)}</h3><p>${escapeHtml(subtitle)}</p>
            <form><div class="field"><label>${escapeHtml(label)}</label>
            <input name="price" type="number" min="0.01" step="0.01" required autofocus></div>
            <div class="role-toggle" style="margin-top:.5rem">
                <button type="button" class="btn" data-x style="flex:1">Cancel</button>
                <button type="submit" class="btn" style="flex:1">Submit</button>
            </div></form>`, (m, close) => {
            m.querySelector("[data-x]").onclick = () => { close(); resolve(null); };
            m.querySelector("form").onsubmit = e => { e.preventDefault(); const v = parseFloat(m.querySelector('[name="price"]').value); close(); resolve(isNaN(v) ? null : v); };
        });
    });
}

function confirmModal(title, subtitle, confirmLabel) {
    return new Promise(resolve => {
        showModal(`
            <h3>${escapeHtml(title)}</h3><p>${escapeHtml(subtitle)}</p>
            <div class="role-toggle">
                <button class="btn" data-x style="flex:1">Cancel</button>
                <button class="btn" data-ok style="flex:1">${escapeHtml(confirmLabel)}</button>
            </div>`, (m, close) => {
            m.querySelector("[data-x]").onclick = () => { close(); resolve(false); };
            m.querySelector("[data-ok]").onclick = () => { close(); resolve(true); };
        });
    });
}

function newListingModal(onDone) {
    showModal(`
        <h3>New listing</h3><p>Add a product to the store.</p>
        <form id="lf">
            <div class="field"><label>Name</label><input name="name" required></div>
            <div class="field"><label>Description</label><input name="description"></div>
            <div class="field"><label>Asking price (RON)</label><input name="price" type="number" min="0.01" step="0.01" required></div>
            <div class="field"><label>Sale type</label>
                <select name="saleType"><option value="FIXED_PRICE">Fixed price</option><option value="NEGOTIABLE">Negotiable</option></select></div>
            <div class="field" id="minWrap" style="display:none"><label>Minimum price (private)</label><input name="minimumPrice" type="number" min="0.01" step="0.01"></div>
            <div class="role-toggle" style="margin-top:.3rem">
                <button type="button" class="btn" data-x style="flex:1">Cancel</button>
                <button type="submit" class="btn" style="flex:1">List it</button>
            </div>
        </form>`, (m, close) => {
        const f = m.querySelector("#lf"), minWrap = m.querySelector("#minWrap");
        f.saleType.onchange = () => { minWrap.style.display = f.saleType.value === "NEGOTIABLE" ? "" : "none"; };
        m.querySelector("[data-x]").onclick = close;
        f.onsubmit = async e => {
            e.preventDefault();
            const body = {
                name: f.name.value.trim(),
                description: f.description.value.trim() || null,
                price: parseFloat(f.price.value),
                saleType: f.saleType.value,
                minimumPrice: f.saleType.value === "NEGOTIABLE" ? parseFloat(f.minimumPrice.value) : null,
            };
            try { await Api.createProduct(body); close(); toast("ok", "Listed", `"${body.name}" is now in the store.`); onDone && onDone(); }
            catch (err) { toast("err", "Couldn't list", err.message); }
        };
    });
}
