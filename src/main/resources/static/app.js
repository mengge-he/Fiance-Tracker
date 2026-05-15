const state = {
    token: localStorage.getItem("financeTrackerToken"),
    user: JSON.parse(localStorage.getItem("financeTrackerUser") || "null"),
    records: []
};

const money = new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD"
});

const els = {
    authView: document.getElementById("authView"),
    dashboardView: document.getElementById("dashboardView"),
    showSignIn: document.getElementById("showSignIn"),
    showSignUp: document.getElementById("showSignUp"),
    signInForm: document.getElementById("signInForm"),
    signUpForm: document.getElementById("signUpForm"),
    authMessage: document.getElementById("authMessage"),
    userLabel: document.getElementById("userLabel"),
    logoutButton: document.getElementById("logoutButton"),
    refreshButton: document.getElementById("refreshButton"),
    monthInput: document.getElementById("monthInput"),
    monthTitle: document.getElementById("monthTitle"),
    incomeTotal: document.getElementById("incomeTotal"),
    expenseTotal: document.getElementById("expenseTotal"),
    netTotal: document.getElementById("netTotal"),
    transactionForm: document.getElementById("transactionForm"),
    transactionId: document.getElementById("transactionId"),
    typeInput: document.getElementById("typeInput"),
    amountInput: document.getElementById("amountInput"),
    categoryInput: document.getElementById("categoryInput"),
    dateInput: document.getElementById("dateInput"),
    noteInput: document.getElementById("noteInput"),
    formTitle: document.getElementById("formTitle"),
    saveRecordButton: document.getElementById("saveRecordButton"),
    cancelEditButton: document.getElementById("cancelEditButton"),
    formMessage: document.getElementById("formMessage"),
    typeFilter: document.getElementById("typeFilter"),
    categoryFilter: document.getElementById("categoryFilter"),
    recordsBody: document.getElementById("recordsBody"),
    recordCount: document.getElementById("recordCount"),
    emptyState: document.getElementById("emptyState")
};

function todayIso() {
    return new Date().toISOString().slice(0, 10);
}

function currentMonthValue() {
    return todayIso().slice(0, 7);
}

function selectedMonthRange() {
    const [year, month] = els.monthInput.value.split("-").map(Number);
    const fromDate = `${year}-${String(month).padStart(2, "0")}-01`;
    const lastDay = new Date(year, month, 0).getDate();
    const toDate = `${year}-${String(month).padStart(2, "0")}-${lastDay}`;
    return { year, month, fromDate, toDate };
}

function setMessage(element, text, isSuccess = false) {
    element.textContent = text;
    element.classList.toggle("success", isSuccess);
}

async function api(path, options = {}) {
    const headers = {
        "Content-Type": "application/json",
        ...(options.headers || {})
    };

    if (state.token) {
        headers.Authorization = `Bearer ${state.token}`;
    }

    const response = await fetch(path, {
        ...options,
        headers
    });

    if (response.status === 401 || response.status === 403) {
        clearSession();
        throw new Error("Your session expired. Please sign in again.");
    }

    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.message || body.error || "Request failed");
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}

function saveSession(auth, user) {
    state.token = auth.token;
    state.user = user;
    localStorage.setItem("financeTrackerToken", auth.token);
    localStorage.setItem("financeTrackerUser", JSON.stringify(user));
}

function clearSession() {
    state.token = null;
    state.user = null;
    state.records = [];
    localStorage.removeItem("financeTrackerToken");
    localStorage.removeItem("financeTrackerUser");
    showAuth();
}

function showAuth() {
    els.authView.classList.remove("hidden");
    els.dashboardView.classList.add("hidden");
}

async function showDashboard() {
    els.authView.classList.add("hidden");
    els.dashboardView.classList.remove("hidden");
    els.userLabel.textContent = state.user?.email ? `Signed in as ${state.user.email}` : "Signed in";
    await loadRecords();
}

function switchAuthMode(mode) {
    const signIn = mode === "signin";
    els.showSignIn.classList.toggle("active", signIn);
    els.showSignUp.classList.toggle("active", !signIn);
    els.signInForm.classList.toggle("active", signIn);
    els.signUpForm.classList.toggle("active", !signIn);
    setMessage(els.authMessage, "");
}

async function handleSignIn(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    try {
        const email = String(form.get("email")).trim().toLowerCase();
        const auth = await api("/api/auth/login", {
            method: "POST",
            body: JSON.stringify({
                email,
                password: form.get("password")
            })
        });
        saveSession(auth, { email });
        await showDashboard();
    } catch (error) {
        setMessage(els.authMessage, error.message);
    }
}

async function handleSignUp(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    try {
        const user = {
            name: String(form.get("name")).trim(),
            email: String(form.get("email")).trim().toLowerCase()
        };
        const auth = await api("/api/auth/register", {
            method: "POST",
            body: JSON.stringify({
                ...user,
                password: form.get("password")
            })
        });
        saveSession(auth, user);
        await showDashboard();
    } catch (error) {
        setMessage(els.authMessage, error.message);
    }
}

async function loadRecords() {
    const { year, month, fromDate, toDate } = selectedMonthRange();
    const params = new URLSearchParams({
        fromDate,
        toDate,
        sort: "date,desc"
    });
    const result = await api(`/api/transactions?${params}`);
    state.records = result.content || [];
    const monthName = new Date(year, month - 1).toLocaleString("en-US", {
        month: "long",
        year: "numeric"
    });
    els.monthTitle.textContent = monthName;
    render();
}

function render() {
    const visible = filteredRecords();
    const totals = state.records.reduce((acc, record) => {
        const amount = Number(record.amount);
        if (record.type === "INCOME") {
            acc.income += amount;
        } else {
            acc.expense += amount;
        }
        return acc;
    }, { income: 0, expense: 0 });

    els.incomeTotal.textContent = money.format(totals.income);
    els.expenseTotal.textContent = money.format(totals.expense);
    els.netTotal.textContent = money.format(totals.income - totals.expense);
    els.recordCount.textContent = `${visible.length} ${visible.length === 1 ? "record" : "records"}`;

    els.recordsBody.innerHTML = visible.map(record => `
        <tr>
            <td>${escapeHtml(record.date)}</td>
            <td><span class="badge ${record.type.toLowerCase()}">${record.type}</span></td>
            <td>${escapeHtml(record.category)}</td>
            <td class="amount">${money.format(Number(record.amount))}</td>
            <td>${escapeHtml(record.note || "")}</td>
            <td>
                <div class="row-actions">
                    <button type="button" data-action="edit" data-id="${record.id}">Edit</button>
                    <button type="button" data-action="delete" data-id="${record.id}">Delete</button>
                </div>
            </td>
        </tr>
    `).join("");

    els.emptyState.classList.toggle("hidden", visible.length > 0);
}

function filteredRecords() {
    const type = els.typeFilter.value;
    const category = els.categoryFilter.value.trim().toLowerCase();
    return state.records.filter(record => {
        const matchesType = !type || record.type === type;
        const matchesCategory = !category || record.category.toLowerCase().includes(category);
        return matchesType && matchesCategory;
    });
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

async function handleSaveRecord(event) {
    event.preventDefault();
    const id = els.transactionId.value;
    const payload = {
        type: els.typeInput.value,
        amount: Number(els.amountInput.value),
        category: els.categoryInput.value.trim(),
        date: els.dateInput.value,
        note: els.noteInput.value.trim()
    };

    try {
        await api(id ? `/api/transactions/${id}` : "/api/transactions", {
            method: id ? "PUT" : "POST",
            body: JSON.stringify(payload)
        });
        resetForm();
        setMessage(els.formMessage, "Record saved.", true);
        await loadRecords();
    } catch (error) {
        setMessage(els.formMessage, error.message);
    }
}

function editRecord(id) {
    const record = state.records.find(item => String(item.id) === String(id));
    if (!record) {
        return;
    }
    els.transactionId.value = record.id;
    els.typeInput.value = record.type;
    els.amountInput.value = record.amount;
    els.categoryInput.value = record.category;
    els.dateInput.value = record.date;
    els.noteInput.value = record.note || "";
    els.formTitle.textContent = "Edit bill record";
    els.saveRecordButton.textContent = "Update record";
    els.cancelEditButton.classList.remove("hidden");
    setMessage(els.formMessage, "");
    els.transactionForm.scrollIntoView({ behavior: "smooth", block: "start" });
}

async function deleteRecord(id) {
    const ok = window.confirm("Delete this bill record?");
    if (!ok) {
        return;
    }

    try {
        await api(`/api/transactions/${id}`, { method: "DELETE" });
        await loadRecords();
    } catch (error) {
        setMessage(els.formMessage, error.message);
    }
}

function resetForm() {
    els.transactionForm.reset();
    els.transactionId.value = "";
    els.dateInput.value = todayIso();
    els.formTitle.textContent = "Add bill record";
    els.saveRecordButton.textContent = "Save record";
    els.cancelEditButton.classList.add("hidden");
}

function bindEvents() {
    els.showSignIn.addEventListener("click", () => switchAuthMode("signin"));
    els.showSignUp.addEventListener("click", () => switchAuthMode("signup"));
    els.signInForm.addEventListener("submit", handleSignIn);
    els.signUpForm.addEventListener("submit", handleSignUp);
    els.logoutButton.addEventListener("click", clearSession);
    els.refreshButton.addEventListener("click", loadRecords);
    els.monthInput.addEventListener("change", loadRecords);
    els.transactionForm.addEventListener("submit", handleSaveRecord);
    els.cancelEditButton.addEventListener("click", () => {
        resetForm();
        setMessage(els.formMessage, "");
    });
    els.typeFilter.addEventListener("change", render);
    els.categoryFilter.addEventListener("input", render);
    els.recordsBody.addEventListener("click", event => {
        const button = event.target.closest("button[data-action]");
        if (!button) {
            return;
        }
        if (button.dataset.action === "edit") {
            editRecord(button.dataset.id);
        } else if (button.dataset.action === "delete") {
            deleteRecord(button.dataset.id);
        }
    });
}

function init() {
    bindEvents();
    els.monthInput.value = currentMonthValue();
    els.dateInput.value = todayIso();
    if (state.token) {
        showDashboard().catch(error => {
            setMessage(els.authMessage, error.message);
            showAuth();
        });
    } else {
        showAuth();
    }
}

init();
