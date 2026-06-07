/* =========================================================================
   MERIDIAN — page logic
   Drives the header, public catalogue, auth forms, and the role-aware
   dashboard. All data comes from the REST API via api.js.
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
            <span class="whoami"><span class="dot"></span>${escapeHtml(s.email)} · ${s.role.toLowerCase()}</span>
            <a class="link" href="/dashboard">Dashboard</a>
            <button class="btn btn--ghost btn--sm" id="signout">Sign out</button>`;
        slot.querySelector("#signout").onclick = () => { Session.clear(); location.href = "/"; };
    } else {
        slot.innerHTML = `<a class="link" href="/">Catalogue</a><a class="btn btn--sm" href="/auth">Sign in</a>`;
    }
}

/* ---------------- Catalogue (index) ---------------- */
async function initCatalogue() {
    const grid = document.getElementById("catalogue");
    const chip = document.getElementById("catCount");
    grid.innerHTML = skeletons(6);
    try {
        const products = await Api.products();
        chip && (chip.textContent = `${products.length} ${products.length === 1 ? "lot" : "lots"} on the floor`);
        if (!products.length) { grid.innerHTML = emptyState("The floor is quiet", "No products are listed right now."); return; }
        grid.innerHTML = "";
        products.forEach((p, i) => grid.appendChild(productCard(p, i)));
    } catch (e) {
        grid.innerHTML = emptyState("Couldn’t load the catalogue", e.message);
    }
}

function productCard(p, i) {
    const isBuyer = Session.role === "BUYER";
    const negotiable = p.saleType === "NEGOTIABLE";
    const card = document.createElement("article");
    card.className = "card reveal";
    card.style.animationDelay = (i * 60) + "ms";

    let actions = "";
    if (isBuyer && negotiable) actions = `<button class="btn btn--accent btn--sm" data-act="offer">Make an offer</button>`;
    else if (isBuyer && !negotiable) actions = `<button class="btn btn--accent btn--sm" data-act="buy">Buy now</button>`;
    else if (!Session.get()) actions = `<a class="btn btn--ghost btn--sm" href="/auth">Sign in to ${negotiable ? "offer" : "buy"}</a>`;

    card.innerHTML = `
        <div class="card__top">
            <h3 class="card__name">${escapeHtml(p.name)}</h3>
            <span class="tag ${negotiable ? "tag--negotiable" : "tag--fixed"}">${negotiable ? "open to offers" : "fixed price"}</span>
        </div>
        <p class="card__desc">${escapeHtml(p.description || "")}</p>
        <span class="card__seller">listed by ${escapeHtml(p.sellerEmail)}</span>
        <div class="card__foot">
            <span class="price">${money(p.price)}</span>
        </div>
        <div class="card__actions">${actions}</div>`;

    const offerBtn = card.querySelector('[data-act="offer"]');
    const buyBtn = card.querySelector('[data-act="buy"]');
    if (offerBtn) offerBtn.onclick = () => makeOffer(p);
    if (buyBtn) buyBtn.onclick = () => buyNow(p);
    return card;
}

async function makeOffer(p) {
    const price = await priceModal("Make an offer", `On “${p.name}”. Asking price ${Number(p.price).toFixed(2)} RON. Offers below the seller’s private minimum are discarded.`, "Your offer (RON)");
    if (price == null) return;
    try {
        const r = await Api.submitOffer(p.reference, price);
        if (r.reference === null && r.status === "REJECTED") {
            toast("err", "Below minimum", "Your offer was below the seller’s minimum and was not recorded.");
        } else {
            toast("ok", "Offer submitted", "Pending the seller’s review. Track it in your dashboard.");
        }
    } catch (e) { toast("err", "Couldn’t submit", e.message); }
}

async function buyNow(p) {
    const ok = await confirmModal("Confirm purchase", `Buy “${p.name}” for ${Number(p.price).toFixed(2)} RON?`, "Buy now");
    if (!ok) return;
    try {
        await Api.purchase(p.reference);
        toast("ok", "Purchased", `“${p.name}” is yours. A receipt is in your dashboard.`);
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
                toast("ok", "Account created", "You’re signed in. Happy browsing.");
                setTimeout(() => location.href = "/dashboard", 600);
            } else {
                await Api.registerSeller(email, password);
                toast("ok", "Request received", "An administrator must approve your seller account before you can list. You can sign in meanwhile.");
                document.querySelector('.tabs button[data-tab="login"]').click();
                loginForm.email.value = email;
            }
        } catch (e) { toast("err", "Couldn’t register", e.message); }
    };
}

/* ---------------- Dashboard ---------------- */
function initDashboard() {
    const s = Session.get();
    if (!s) { location.href = "/auth"; return; }

    document.getElementById("dashWho").textContent = s.email;
    document.getElementById("dashRole").textContent = s.role;

    const tabsEl = document.getElementById("dashTabs");
    const panel = document.getElementById("dashPanel");

    const views = {
        BUYER: [
            { id: "offers", label: "My offers", render: renderBuyerOffers },
            { id: "purchases", label: "My purchases", render: renderBuyerPurchases },
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
        panel.innerHTML = `<div class="list">${skeletonRows(3)}</div>`;
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
    if (!offers.length) return void (panel.innerHTML = emptyState("No offers yet", "Browse the catalogue and make an offer on a negotiable lot."));
    panel.innerHTML = `<div class="list"></div>`;
    const list = panel.querySelector(".list");
    offers.forEach(o => {
        const row = document.createElement("div");
        row.className = "row reveal";
        const canBuy = o.status === "APPROVED";
        row.innerHTML = `
            <div class="row__main">
                <span class="row__title">Offer · ${money(o.proposedPrice)}</span>
                <span class="row__meta">product ${shortRef(o.productReference)} · offer ${shortRef(o.reference)}</span>
            </div>
            <div class="row__actions">
                ${stamp(o.status)}
                ${canBuy ? `<button class="btn btn--accent btn--sm" data-buy="${o.productReference}">Complete purchase</button>` : ""}
            </div>`;
        const buy = row.querySelector("[data-buy]");
        if (buy) buy.onclick = async () => {
            try { await Api.purchase(o.productReference); toast("ok", "Purchased", "Your negotiated lot is yours."); renderBuyerOffers(panel); }
            catch (e) { toast("err", "Purchase failed", e.message); }
        };
        list.appendChild(row);
    });
}

async function renderBuyerPurchases(panel) {
    const sales = await Api.myPurchases();
    if (!sales.length) return void (panel.innerHTML = emptyState("No purchases yet", "Your receipts will appear here."));
    panel.innerHTML = `<div class="list"></div>`;
    const list = panel.querySelector(".list");
    sales.forEach(x => {
        const row = document.createElement("div");
        row.className = "row reveal";
        row.innerHTML = `
            <div class="row__main">
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
        <div class="section-head" style="margin-bottom:1.2rem">
            <span class="count-chip">${products.length} active ${products.length === 1 ? "listing" : "listings"}</span>
            <button class="btn btn--accent btn--sm" id="newListing">+ New listing</button>
        </div>
        <div class="list"></div>`;
    panel.querySelector("#newListing").onclick = () => newListingModal(() => renderSellerListings(panel));
    const list = panel.querySelector(".list");
    if (!products.length) { list.innerHTML = emptyState("Nothing listed", "Put your first product on the floor."); return; }
    products.forEach(p => {
        const negotiable = p.saleType === "NEGOTIABLE";
        const row = document.createElement("div");
        row.className = "row reveal";
        row.innerHTML = `
            <div class="row__main">
                <span class="row__title">${escapeHtml(p.name)}</span>
                <span class="row__meta">${shortRef(p.reference)} · ${escapeHtml(p.description || "no description")}</span>
            </div>
            <div class="row__actions">
                <span class="tag ${negotiable ? "tag--negotiable" : "tag--fixed"}">${negotiable ? "negotiable" : "fixed"}</span>
                <span class="price">${money(p.price)}</span>
                <button class="btn btn--danger btn--sm" data-cancel="${p.reference}">Cancel</button>
            </div>`;
        row.querySelector("[data-cancel]").onclick = async () => {
            if (!await confirmModal("Cancel listing", `Remove “${p.name}” from sale? Offers on it are discarded.`, "Cancel listing")) return;
            try { await Api.cancelListing(p.reference); toast("ok", "Listing removed", `“${p.name}” is off the floor.`); renderSellerListings(panel); }
            catch (e) { toast("err", "Couldn’t cancel", e.message); }
        };
        list.appendChild(row);
    });
}

async function renderSellerOffers(panel) {
    const offers = await Api.sellerOffers();
    if (!offers.length) return void (panel.innerHTML = emptyState("No offers yet", "Offers on your negotiable lots show up here."));
    panel.innerHTML = `<div class="list"></div>`;
    const list = panel.querySelector(".list");
    offers.forEach(o => {
        const pending = o.status === "PENDING";
        const row = document.createElement("div");
        row.className = "row reveal";
        row.innerHTML = `
            <div class="row__main">
                <span class="row__title">${money(o.proposedPrice)}</span>
                <span class="row__meta">from ${escapeHtml(o.buyerEmail)} · product ${shortRef(o.productReference)}</span>
            </div>
            <div class="row__actions">
                ${pending ? `
                    <button class="btn btn--sm" data-approve="${o.reference}">Approve</button>
                    <button class="btn btn--danger btn--sm" data-reject="${o.reference}">Reject</button>
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
    panel.innerHTML = `<div class="list"></div>`;
    const list = panel.querySelector(".list");
    sellers.forEach(u => {
        const row = document.createElement("div");
        row.className = "row reveal";
        const state = !u.active ? `<span class="stamp stamp--rejected">deactivated</span>`
            : u.approved ? `<span class="stamp stamp--approved">approved</span>`
            : `<span class="stamp stamp--pending">awaiting</span>`;
        row.innerHTML = `
            <div class="row__main">
                <span class="row__title">${escapeHtml(u.email)}</span>
                <span class="row__meta">role ${u.role.toLowerCase()}</span>
            </div>
            <div class="row__actions">
                ${state}
                ${!u.approved && u.active ? `<button class="btn btn--sm" data-approve>Approve</button>` : ""}
                ${u.active ? `<button class="btn btn--danger btn--sm" data-deact>Deactivate</button>` : ""}
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
function skeletons(n) { return Array.from({ length: n }, () => `<div class="card skeleton" style="height:210px;border:0"></div>`).join(""); }
function skeletonRows(n) { return Array.from({ length: n }, () => `<div class="row skeleton" style="height:64px;border:0"></div>`).join(""); }
function emptyState(title, sub) { return `<div class="empty"><span class="serif-italic">${escapeHtml(title)}</span>${escapeHtml(sub || "")}</div>`; }
function noticeBanner(msg) {
    return `<div class="panel" style="border-left:3px solid var(--pending)">
        <span class="eyebrow">Awaiting approval</span>
        <p style="margin-top:.5rem;color:var(--ink-soft)">${escapeHtml(msg)}</p></div>`;
}
function fmtDate(s) { try { return new Date(s).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" }); } catch { return s; } }

/* ---------------- Modals ---------------- */
function showModal(inner, setup) {
    const back = document.createElement("div");
    back.className = "modal-back";
    back.innerHTML = `<div class="modal panel">${inner}</div>`;
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
                <button type="button" class="btn btn--ghost" data-x style="flex:1">Cancel</button>
                <button type="submit" class="btn btn--accent" style="flex:1">Submit</button>
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
                <button class="btn btn--ghost" data-x style="flex:1">Cancel</button>
                <button class="btn btn--accent" data-ok style="flex:1">${escapeHtml(confirmLabel)}</button>
            </div>`, (m, close) => {
            m.querySelector("[data-x]").onclick = () => { close(); resolve(false); };
            m.querySelector("[data-ok]").onclick = () => { close(); resolve(true); };
        });
    });
}

function newListingModal(onDone) {
    showModal(`
        <h3>New listing</h3><p>Put a product on the floor.</p>
        <form id="lf">
            <div class="field"><label>Name</label><input name="name" required></div>
            <div class="field"><label>Description</label><input name="description"></div>
            <div class="field"><label>Asking price (RON)</label><input name="price" type="number" min="0.01" step="0.01" required></div>
            <div class="field"><label>Sale type</label>
                <select name="saleType"><option value="FIXED_PRICE">Fixed price</option><option value="NEGOTIABLE">Negotiable</option></select></div>
            <div class="field" id="minWrap" style="display:none"><label>Minimum price (private)</label><input name="minimumPrice" type="number" min="0.01" step="0.01"></div>
            <div class="role-toggle" style="margin-top:.3rem">
                <button type="button" class="btn btn--ghost" data-x style="flex:1">Cancel</button>
                <button type="submit" class="btn btn--accent" style="flex:1">List it</button>
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
            try { await Api.createProduct(body); close(); toast("ok", "Listed", `“${body.name}” is on the floor.`); onDone && onDone(); }
            catch (err) { toast("err", "Couldn’t list", err.message); }
        };
    });
}
