/* =========================================================================
   MERIDIAN — API client
   Thin wrapper over the stateless JWT REST backend (/api/**). Stores the token
   client-side and attaches it as a Bearer header. Same origin → no CORS.
   ========================================================================= */

const KEY = "meridian.session";

const Session = {
    get() {
        try { return JSON.parse(localStorage.getItem(KEY)); }
        catch { return null; }
    },
    set(s) { localStorage.setItem(KEY, JSON.stringify(s)); },
    clear() { localStorage.removeItem(KEY); },
    get token() { return this.get()?.token ?? null; },
    get email() { return this.get()?.email ?? null; },
    get role()  { return this.get()?.role  ?? null; },
};

/**
 * Core request helper.
 * Resolves with parsed JSON (or null for empty bodies); rejects with an
 * { status, message } object the UI can surface.
 */
async function request(method, path, { body, auth = true } = {}) {
    const headers = {};
    if (body !== undefined) headers["Content-Type"] = "application/json";
    if (auth && Session.token) headers["Authorization"] = "Bearer " + Session.token;

    let res;
    try {
        res = await fetch(path, {
            method,
            headers,
            body: body !== undefined ? JSON.stringify(body) : undefined,
        });
    } catch {
        throw { status: 0, message: "Network error — is the server running?" };
    }

    // Expired/invalid token → bounce to sign-in.
    if (res.status === 401 && auth && Session.token) {
        Session.clear();
        if (!location.pathname.startsWith("/auth")) location.href = "/auth";
        throw { status: 401, message: "Session expired — please sign in again." };
    }

    const text = await res.text();
    const data = text ? safeParse(text) : null;

    if (!res.ok) {
        const message = (data && (data.message || firstError(data))) || `Request failed (${res.status})`;
        throw { status: res.status, message };
    }
    return data;
}

function safeParse(t) { try { return JSON.parse(t); } catch { return { message: t }; } }
function firstError(d) { return d.errors ? Object.values(d.errors)[0] : null; }

const Api = {
    // auth
    login: (email, password) => request("POST", "/api/auth/login", { body: { email, password }, auth: false }),
    registerBuyer: (email, password) => request("POST", "/api/auth/register/buyer", { body: { email, password }, auth: false }),
    registerSeller: (email, password) => request("POST", "/api/auth/register/seller", { body: { email, password }, auth: false }),

    // catalogue (public)
    products: () => request("GET", "/api/products", { auth: false }),

    // buyer
    submitOffer: (ref, proposedPrice) => request("POST", `/api/buyer/products/${ref}/offers`, { body: { proposedPrice } }),
    purchase: (ref) => request("POST", `/api/buyer/products/${ref}/purchase`),
    myOffers: () => request("GET", "/api/buyer/offers"),
    myPurchases: () => request("GET", "/api/buyer/purchases"),

    // seller
    sellerProducts: () => request("GET", "/api/seller/products"),
    createProduct: (p) => request("POST", "/api/seller/products", { body: p }),
    cancelListing: (ref) => request("DELETE", `/api/seller/products/${ref}`),
    sellerOffers: () => request("GET", "/api/seller/offers"),
    approveOffer: (ref) => request("POST", `/api/seller/offers/${ref}/approve`),
    rejectOffer: (ref) => request("POST", `/api/seller/offers/${ref}/reject`),

    // admin
    sellers: () => request("GET", "/api/admin/sellers"),
    approveSeller: (email) => request("POST", `/api/admin/sellers/${encodeURIComponent(email)}/approve`),
    deactivateSeller: (email) => request("POST", `/api/admin/sellers/${encodeURIComponent(email)}/deactivate`),
};

/* ---------- UI helpers shared across pages ---------- */
function toast(kind, title, msg) {
    let stack = document.querySelector(".toast-stack");
    if (!stack) { stack = document.createElement("div"); stack.className = "toast-stack"; document.body.appendChild(stack); }
    const el = document.createElement("div");
    el.className = "toast " + (kind === "ok" ? "toast--ok" : kind === "err" ? "toast--err" : "");
    el.innerHTML = `<b>${title}</b>${escapeHtml(msg)}`;
    stack.appendChild(el);
    setTimeout(() => { el.style.transition = "opacity .3s, transform .3s"; el.style.opacity = "0"; el.style.transform = "translateX(20px)"; setTimeout(() => el.remove(), 320); }, 4200);
}

function money(v) {
    return `${Number(v).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} <span class="cur">RON</span>`;
}
function escapeHtml(s) {
    return String(s ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}
function shortRef(r) { return r ? r.slice(0, 8) : "—"; }
