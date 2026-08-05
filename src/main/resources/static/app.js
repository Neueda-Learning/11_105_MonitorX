// MonitorX - Client Application Engine

const state = {
    transactions: [],
    alerts: [],
    rules: [],
    customers: [],
    activeTab: 'overview',
    selectedAlert: null,
    alertFilter: 'ALL'
};

const elements = {
    // Navigation & Tabs
    navItems: document.querySelectorAll('.nav-item'),
    tabPanes: document.querySelectorAll('.tab-pane'),
    pageTitle: document.getElementById('pageTitle'),
    pageDescription: document.getElementById('pageDescription'),
    sidebarAlertBadge: document.getElementById('sidebarAlertBadge'),

    // Summary Metrics
    metricVolume: document.getElementById('metricVolume'),
    metricTotal: document.getElementById('metricTotal'),
    metricFlagged: document.getElementById('metricFlagged'),
    metricActiveAlerts: document.getElementById('metricActiveAlerts'),

    // Chart & Stats
    overviewChart: document.getElementById('overviewChart'),
    alertDistributionDonut: document.getElementById('alertDistributionDonut'),
    alertDistributionLegend: document.getElementById('alertDistributionLegend'),
    quickAlertList: document.getElementById('quickAlertList'),

    // Transactions Feed
    transactionRows: document.getElementById('transactionRows'),
    transactionEmpty: document.getElementById('transactionEmpty'),
    searchDesc: document.getElementById('searchDesc'),
    filterCustomer: document.getElementById('filterCustomer'),
    filterAmountMin: document.getElementById('filterAmountMin'),
    filterAmountMax: document.getElementById('filterAmountMax'),
    filterStatus: document.getElementById('filterStatus'),
    btnApplyFilters: document.getElementById('btnApplyFilters'),
    btnResetFilters: document.getElementById('btnResetFilters'),

    // Alerts Cases Queue
    alertsGrid: document.getElementById('alertsGrid'),
    alertEmpty: document.getElementById('alertEmpty'),
    alertsTabBtns: document.querySelectorAll('.alerts-tab-btn'),
    countOpen: document.getElementById('countOpen'),
    countAck: document.getElementById('countAck'),
    countInv: document.getElementById('countInv'),

    // Rules CRUD Table
    ruleRows: document.getElementById('ruleRows'),

    // Modals
    transactionDialog: document.getElementById('transactionDialog'),
    transactionForm: document.getElementById('transactionForm'),
    customerId: document.getElementById('customerId'),
    country: document.getElementById('country'),
    amount: document.getElementById('amount'),
    payeeId: document.getElementById('payeeId'),
    timestamp: document.getElementById('timestamp'),
    description: document.getElementById('description'),
    formError: document.getElementById('formError'),
    newTransactionButton: document.getElementById('newTransactionButton'),
    closeDialog: document.getElementById('closeDialog'),
    cancelDialog: document.getElementById('cancelDialog'),

    ruleDialog: document.getElementById('ruleDialog'),
    ruleForm: document.getElementById('ruleForm'),
    ruleId: document.getElementById('ruleId'),
    ruleName: document.getElementById('ruleName'),
    ruleType: document.getElementById('ruleType'),
    ruleSeverity: document.getElementById('ruleSeverity'),
    ruleActive: document.getElementById('ruleActive'),
    ruleFormError: document.getElementById('ruleFormError'),
    btnNewRule: document.getElementById('btnNewRule'),
    closeRuleDialog: document.getElementById('closeRuleDialog'),
    cancelRuleDialog: document.getElementById('cancelRuleDialog'),

    // Rule parameter panels
    paramAmountThreshold: document.getElementById('param-AMOUNT_THRESHOLD'),
    paramVelocity: document.getElementById('param-VELOCITY'),
    paramNewPayee: document.getElementById('param-NEW_PAYEE'),
    paramDailyLimit: document.getElementById('param-DAILY_LIMIT'),
    
    // Rule input elements
    inputThreshold: document.getElementById('paramThreshold'),
    inputVelocityMins: document.getElementById('paramVelocityMins'),
    inputVelocityMaxCount: document.getElementById('paramVelocityMaxCount'),
    inputDailyLimit: document.getElementById('paramDailyLimit'),

    // Slide-out Drawer
    caseDrawer: document.getElementById('caseDrawer'),
    closeCaseDrawer: document.getElementById('closeCaseDrawer'),
    caseTitle: document.getElementById('caseTitle'),
    caseBadgeStatus: document.getElementById('caseBadgeStatus'),
    caseBadgeSeverity: document.getElementById('caseBadgeSeverity'),
    caseScore: document.getElementById('caseScore'),
    caseCustomerName: document.getElementById('caseCustomerName'),
    caseTime: document.getElementById('caseTime'),
    caseReasons: document.getElementById('caseReasons'),
    caseTxId: document.getElementById('caseTxId'),
    caseTxAmount: document.getElementById('caseTxAmount'),
    caseTxPayee: document.getElementById('caseTxPayee'),
    caseTxCountry: document.getElementById('caseTxCountry'),
    caseTxDescription: document.getElementById('caseTxDescription'),
    caseHistoryTimeline: document.getElementById('caseHistoryTimeline'),
    operatorNotes: document.getElementById('operatorNotes'),
    caseActionButtons: document.getElementById('caseActionButtons'),
    caseActionSection: document.getElementById('caseActionSection'),

    // Toast Notification & Seeding
    toast: document.getElementById('toast'),
    seedButton: document.getElementById('seedButton'),

    // Authentication Elements
    loginOverlay: document.getElementById('loginOverlay'),
    loginForm: document.getElementById('loginForm'),
    loginUsername: document.getElementById('loginUsername'),
    loginPassword: document.getElementById('loginPassword'),
    loginError: document.getElementById('loginError'),
    btnLogout: document.getElementById('btnLogout'),

    // Customer Elements
    customerRows: document.getElementById('customerRows'),
    customerEmpty: document.getElementById('customerEmpty'),
    btnNewCustomer: document.getElementById('btnNewCustomer'),
    customerDialog: document.getElementById('customerDialog'),
    customerForm: document.getElementById('customerForm'),
    customerFormId: document.getElementById('customerFormId'),
    customerFormName: document.getElementById('customerFormName'),
    customerFormAccount: document.getElementById('customerFormAccount'),
    customerFormCountry: document.getElementById('customerFormCountry'),
    customerFormError: document.getElementById('customerFormError'),
    closeCustomerDialog: document.getElementById('closeCustomerDialog'),
    cancelCustomerDialog: document.getElementById('cancelCustomerDialog')
};

// Format Helpers
const money = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });
const date = new Intl.DateTimeFormat("en-US", { dateStyle: "medium", timeStyle: "short" });

// HTTP Request Utility
async function request(url, options = {}) {
    const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
    const token = localStorage.getItem("monitorx_token");
    if (token) {
        headers["Authorization"] = `Bearer ${token}`;
    }

    const response = await fetch(url, {
        headers,
        ...options
    });

    if (response.status === 401 && !url.includes("/api/auth/login")) {
        handleUnauthorized();
        throw new Error("Session expired. Please log in again.");
    }

    if (!response.ok) {
        let message = `Request failed (${response.status})`;
        try {
            const body = await response.json();
            message = body.message || body.detail || body.error || message;
        } catch (_) { /* keep status error */ }
        throw new Error(message);
    }
    return response.status === 204 ? null : response.json();
}

function handleUnauthorized() {
    localStorage.removeItem("monitorx_token");
    localStorage.removeItem("monitorx_user");
    elements.loginOverlay.style.display = "flex";
}

function safeHTML(value) {
    const node = document.createElement("span");
    node.textContent = value ?? "";
    return node.innerHTML;
}

function toast(message, type = 'success') {
    elements.toast.textContent = message;
    elements.toast.style.borderLeftColor = type === 'error' ? 'var(--danger-color)' : 'var(--accent-color)';
    elements.toast.classList.add("show");
    window.setTimeout(() => elements.toast.classList.remove("show"), 3000);
}

// ----------------------------------------------------
// Navigation / Routing
// ----------------------------------------------------
function initNavigation() {
    window.addEventListener('hashchange', handleRoute);
    handleRoute();

    elements.navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const hash = item.getAttribute('href');
            window.location.hash = hash;
        });
    });
}

function handleRoute() {
    let hash = window.location.hash.substring(1) || 'overview';
    if (!['overview', 'transactions', 'alerts', 'rules', 'customers'].includes(hash)) {
        hash = 'overview';
    }

    state.activeTab = hash;

    // Toggle active classes
    elements.navItems.forEach(item => {
        item.classList.toggle('active', item.getAttribute('href') === `#${hash}`);
    });

    elements.tabPanes.forEach(pane => {
        pane.classList.toggle('active', pane.getAttribute('id') === `tab-${hash}`);
    });

    // Update Header Meta
    const titles = {
        overview: { title: "Overview Dashboard", desc: "Real-time indicators & transaction monitoring summary" },
        transactions: { title: "Transaction Activity", desc: "Audit trail and filtering of system transactions" },
        alerts: { title: "Alerts Case Queue", desc: "Case files and status lifecycle operations" },
        rules: { title: "Rules Engine", desc: "Configure and toggle automated monitoring patterns" },
        customers: { title: "Customer Directory", desc: "Manage customer profiles, accounts, and registered countries" }
    };

    elements.pageTitle.textContent = titles[hash].title;
    elements.pageDescription.textContent = titles[hash].desc;

    // Reset drawer if switching pages
    closeDrawer();
}

// ----------------------------------------------------
// API Syncing & Refreshes
// ----------------------------------------------------
async function syncData() {
    try {
        const [summary, txs, alerts, rules, custs] = await Promise.all([
            request("/api/summary"),
            request("/api/transactions"),
            request("/api/alerts"),
            request("/api/rules"),
            request("/api/customers")
        ]);

        state.transactions = txs;
        state.alerts = alerts;
        state.rules = rules;
        state.customers = custs;

        renderSummary(summary);
        renderTransactionsFeed();
        renderAlertsQueue();
        renderRulesGrid();
        renderOverviewVisualizations();
        populateCustomersDropdown();
        renderCustomersGrid();
        loadActivityLog();
    } catch (err) {
        toast(err.message, 'error');
    }
}

// ----------------------------------------------------
// Activity / Audit Log
// ----------------------------------------------------
async function loadActivityLog() {
    try {
        const activity = await request('/api/activity');
        renderActivityLog(activity);
    } catch (_) { /* non-critical, silently ignore */ }
}

function renderActivityLog(entries) {
    const tbody = document.getElementById('activityLogRows');
    const empty = document.getElementById('activityLogEmpty');
    if (!tbody) return;

    if (!entries || entries.length === 0) {
        tbody.innerHTML = '';
        if (empty) empty.style.display = 'block';
        return;
    }
    if (empty) empty.style.display = 'none';

    const severityClass = { HIGH: 'high', MEDIUM: 'medium', LOW: 'low' };
    const statusClass = {
        OPEN: 'open', ACKNOWLEDGED: 'acknowledged', INVESTIGATING: 'investigating',
        CLOSED: 'closed', DISMISSED: 'dismissed'
    };

    tbody.innerHTML = entries.map((e, i) => `
        <tr>
            <td style="color:var(--text-muted);font-size:0.8rem;">${i + 1}</td>
            <td style="font-size:0.82rem;white-space:nowrap;">${date.format(new Date(e.changedAt))}</td>
            <td style="font-weight:600;">${safeHTML(e.customerName)}</td>
            <td style="font-size:0.85rem;">A-${e.alertId}</td>
            <td style="font-size:0.85rem;">T-${e.transactionId}</td>
            <td><span class="badge ${severityClass[e.severity] || ''}">${safeHTML(e.severity)}</span></td>
            <td><span class="badge ${statusClass[e.status] || ''}">${safeHTML(e.status)}</span></td>
            <td style="font-size:0.82rem;color:var(--text-secondary);max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="${safeHTML(e.operatorNotes)}">${safeHTML(e.operatorNotes) || '<em style="color:var(--text-muted)">—</em>'}</td>
        </tr>
    `).join('');
}

function renderSummary(summary) {
    elements.metricTotal.textContent = summary.totalTransactions;
    elements.metricFlagged.textContent = summary.flaggedTransactions;
    elements.metricActiveAlerts.textContent = summary.openAlerts;
    elements.metricVolume.textContent = money.format(summary.totalVolume);

    // Update sidebar counts
    const activeCount = state.alerts.filter(a => ['OPEN', 'ACKNOWLEDGED', 'INVESTIGATING'].includes(a.status)).length;
    if (activeCount > 0) {
        elements.sidebarAlertBadge.textContent = activeCount;
        elements.sidebarAlertBadge.style.display = 'inline-block';
    } else {
        elements.sidebarAlertBadge.style.display = 'none';
    }
}

// ----------------------------------------------------
// Tab 1: Overview Visualizations (Line Graph & Legend)
// ----------------------------------------------------
function renderOverviewVisualizations() {
    // 1. Draw Custom Spark Line/Bar Chart on Canvas
    const canvas = elements.overviewChart;
    if (!canvas) return;

    // Adjust canvas dimensions dynamically
    const container = canvas.parentElement;
    canvas.width = container.clientWidth;
    canvas.height = container.clientHeight;

    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    const txs = [...state.transactions].reverse(); // oldest first
    if (txs.length === 0) {
        ctx.fillStyle = "rgba(255, 255, 255, 0.1)";
        ctx.font = "14px 'Plus Jakarta Sans'";
        ctx.textAlign = "center";
        ctx.fillText("No transaction metrics logged yet.", canvas.width / 2, canvas.height / 2);
        return;
    }

    // Graph scaling parameters
    const paddingLeft = 50;
    const paddingRight = 20;
    const paddingTop = 30;
    const paddingBottom = 40;

    const graphWidth = canvas.width - paddingLeft - paddingRight;
    const graphHeight = canvas.height - paddingTop - paddingBottom;

    // Calculate dynamic coordinates
    const maxAmount = Math.max(...txs.map(t => Number(t.amount)), 1000);
    const minAmount = 0;

    ctx.strokeStyle = "rgba(255, 255, 255, 0.05)";
    ctx.lineWidth = 1;

    // Draw horizontal gridlines (3 lines)
    for (let i = 0; i <= 3; i++) {
        const y = paddingTop + (graphHeight * i) / 3;
        ctx.beginPath();
        ctx.moveTo(paddingLeft, y);
        ctx.lineTo(canvas.width - paddingRight, y);
        ctx.stroke();

        // Labels
        ctx.fillStyle = "var(--text-muted)";
        ctx.font = "10px sans-serif";
        ctx.textAlign = "right";
        const val = maxAmount - (maxAmount * i) / 3;
        ctx.fillText(money.format(val).split('.')[0], paddingLeft - 8, y + 3);
    }

    // Coordinates points
    const points = txs.map((t, idx) => {
        const x = paddingLeft + (graphWidth * idx) / Math.max(txs.length - 1, 1);
        const amt = Number(t.amount);
        const y = paddingTop + graphHeight - ((amt - minAmount) / (maxAmount - minAmount)) * graphHeight;
        return { x, y, transaction: t };
    });

    // Draw area under curve
    if (points.length > 1) {
        ctx.beginPath();
        ctx.moveTo(points[0].x, paddingTop + graphHeight);
        points.forEach(p => ctx.lineTo(p.x, p.y));
        ctx.lineTo(points[points.length - 1].x, paddingTop + graphHeight);
        ctx.closePath();

        const grad = ctx.createLinearGradient(0, paddingTop, 0, paddingTop + graphHeight);
        grad.addColorStop(0, "rgba(99, 102, 241, 0.15)");
        grad.addColorStop(1, "rgba(99, 102, 241, 0.0)");
        ctx.fillStyle = grad;
        ctx.fill();
    }

    // Draw glowing lines
    ctx.beginPath();
    points.forEach((p, idx) => {
        if (idx === 0) ctx.moveTo(p.x, p.y);
        else ctx.lineTo(p.x, p.y);
    });
    ctx.strokeStyle = "var(--info-color)";
    ctx.lineWidth = 2.5;
    ctx.shadowColor = "rgba(99, 102, 241, 0.4)";
    ctx.shadowBlur = 8;
    ctx.stroke();
    ctx.shadowBlur = 0; // reset

    // Draw interactive hover indicators/points
    points.forEach((p, idx) => {
        ctx.beginPath();
        ctx.arc(p.x, p.y, 4, 0, 2 * Math.PI);
        ctx.fillStyle = p.transaction.status === 'FLAGGED' ? "var(--danger-color)" : "var(--success-color)";
        ctx.fill();
    });

    // 2. Populating Alert Status Distribution legend/counts
    const counts = { OPEN: 0, ACKNOWLEDGED: 0, INVESTIGATING: 0, CLOSED: 0, DISMISSED: 0 };
    state.alerts.forEach(a => { if (counts[a.status] !== undefined) counts[a.status]++; });

    const total = state.alerts.length || 1;
    elements.alertDistributionDonut.innerHTML = `
        <div class="donut-center">
            <h3>${state.alerts.length}</h3>
            <span>Total Alerts</span>
        </div>
    `;

    // Visual legend breakdown
    elements.alertDistributionLegend.innerHTML = Object.entries(counts).map(([status, count]) => {
        const pct = Math.round((count / total) * 100);
        return `
            <li>
                <span class="badge ${status.toLowerCase()}">${status}</span>
                <strong>${count}</strong>
                <small>${pct}%</small>
            </li>
        `;
    }).join("");

    // 3. Populate Recent Critical Alerts list
    const criticalAlerts = state.alerts
        .filter(a => ['OPEN', 'ACKNOWLEDGED', 'INVESTIGATING'].includes(a.status))
        .slice(0, 5);

    if (criticalAlerts.length === 0) {
        elements.quickAlertList.innerHTML = `<div class="empty">No active alerts. Cases are completely resolved!</div>`;
    } else {
        elements.quickAlertList.innerHTML = criticalAlerts.map(alert => `
            <div class="quick-alert-row" onclick="openAlertDrawer(${alert.id})">
                <div class="quick-alert-desc">
                    <span class="quick-alert-title">${safeHTML(alert.customerName)}</span>
                    <span class="quick-alert-subtitle">${safeHTML(alert.reasons[0])}</span>
                </div>
                <div style="display:flex; gap:10px; align-items:center;">
                    <span class="badge ${alert.severity.toLowerCase()}">${alert.severity}</span>
                    <span class="badge ${alert.status.toLowerCase()}">${alert.status}</span>
                </div>
            </div>
        `).join("");
    }
}

// ----------------------------------------------------
// Tab 2: Transactions Feed & Filtering
// ----------------------------------------------------
function populateCustomersDropdown() {
    const list = state.customers.map(c => `
        <option value="${c.id}" data-country="${safeHTML(c.registeredCountry)}">${safeHTML(c.name)} · ${c.accountNumber}</option>
    `).join("");

    elements.customerId.innerHTML = list;
    
    // Filter customer list
    const filterOptions = `<option value="">All Customers</option>` + state.customers.map(c => `
        <option value="${c.id}">${safeHTML(c.name)}</option>
    `).join("");
    elements.filterCustomer.innerHTML = filterOptions;

    setCustomerCountry();
}

function setCustomerCountry() {
    const option = elements.customerId.selectedOptions[0];
    if (option) elements.country.value = option.dataset.country;
}

elements.customerId.addEventListener("change", setCustomerCountry);

function renderTransactionsFeed() {
    const searchVal = elements.searchDesc.value.toLowerCase().trim();
    const customerVal = elements.filterCustomer.value;
    const amountMin = Number(elements.filterAmountMin.value) || 0;
    const amountMax = Number(elements.filterAmountMax.value) || Infinity;
    const statusVal = elements.filterStatus.value;

    // Client-side filtering for immediate speed
    const filtered = state.transactions.filter(t => {
        const matchesSearch = String(t.id).includes(searchVal) || t.description.toLowerCase().includes(searchVal);
        const matchesCustomer = !customerVal || t.customerId === Number(customerVal);
        const amt = Number(t.amount);
        const matchesAmount = amt >= amountMin && amt <= amountMax;
        const matchesStatus = !statusVal || t.status === statusVal;
        return matchesSearch && matchesCustomer && matchesAmount && matchesStatus;
    });

    elements.transactionEmpty.style.display = filtered.length === 0 ? 'block' : 'none';
    
    elements.transactionRows.innerHTML = filtered.map(t => {
        const alertTriggered = t.status === 'FLAGGED' ? 'alert-row-highlight' : '';
        return `
            <tr class="${alertTriggered}" onclick="viewTransactionDetails(${t.id})">
                <td><strong>#${t.id}</strong></td>
                <td>
                    <strong>${safeHTML(t.customerName)}</strong>
                    <small style="display:block; color:var(--text-muted)">Cust ID: ${t.customerId}</small>
                </td>
                <td><code>${safeHTML(t.payeeId)}</code></td>
                <td><strong>${money.format(t.amount)}</strong></td>
                <td>
                    <span class="badge ${t.riskScore >= 60 ? 'high' : t.riskScore >= 30 ? 'medium' : 'low'}">
                        ${t.riskScore}
                    </span>
                </td>
                <td>${safeHTML(t.transactionCountry)}</td>
                <td>${date.format(new Date(t.timestamp))}</td>
                <td><small>${safeHTML(t.description || 'N/A')}</small></td>
                <td><span class="badge ${t.status.toLowerCase()}">${t.status}</span></td>
            </tr>
        `;
    }).join("");
}

// Transactions Filters Events
elements.btnApplyFilters.addEventListener('click', renderTransactionsFeed);
elements.btnResetFilters.addEventListener('click', () => {
    elements.searchDesc.value = '';
    elements.filterCustomer.value = '';
    elements.filterAmountMin.value = '';
    elements.filterAmountMax.value = '';
    elements.filterStatus.value = '';
    renderTransactionsFeed();
});

// Trigger modal add transaction
elements.newTransactionButton.addEventListener('click', () => {
    elements.transactionForm.reset();
    elements.formError.textContent = '';
    setCustomerCountry();
    elements.timestamp.value = new Date().toISOString().substring(0, 16);
    elements.transactionDialog.showModal();
});

elements.closeDialog.addEventListener('click', () => elements.transactionDialog.close());
elements.cancelDialog.addEventListener('click', () => elements.transactionDialog.close());

elements.transactionForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    elements.formError.textContent = '';

    const body = {
        customerId: Number(elements.customerId.value),
        amount: Number(elements.amount.value),
        payeeId: elements.payeeId.value.trim(),
        transactionCountry: elements.country.value.trim(),
        timestamp: elements.timestamp.value || null,
        description: elements.description.value.trim()
    };

    try {
        const saved = await request('/api/transactions', {
            method: 'POST',
            body: JSON.stringify(body)
        });
        elements.transactionDialog.close();
        toast(saved.status === 'FLAGGED' ? 'Transaction FLAGGED by rules engine' : 'Transaction APPROVED successfully');
        await syncData();
    } catch (err) {
        elements.formError.textContent = err.message;
    }
});

// ----------------------------------------------------
// Tab 3: Alerts cases Queue & Lifecycles
// ----------------------------------------------------
function renderAlertsQueue() {
    // Update count labels in alert filter bar
    let open = 0, ack = 0, inv = 0;
    state.alerts.forEach(a => {
        if (a.status === 'OPEN') open++;
        else if (a.status === 'ACKNOWLEDGED') ack++;
        else if (a.status === 'INVESTIGATING') inv++;
    });

    elements.countOpen.textContent = open;
    elements.countAck.textContent = ack;
    elements.countInv.textContent = inv;

    // Filter alerts list based on tab
    const filter = state.alertFilter;
    const filtered = state.alerts.filter(alert => {
        if (filter === 'ALL') return true;
        if (filter === 'CLOSED') return alert.status === 'CLOSED' || alert.status === 'DISMISSED';
        return alert.status === filter;
    });

    elements.alertEmpty.style.display = filtered.length === 0 ? 'block' : 'none';

    elements.alertsGrid.innerHTML = filtered.map(a => `
        <div class="alert-card" onclick="openAlertDrawer(${a.id})">
            <div class="alert-top">
                <span class="badge ${a.severity.toLowerCase()}">${a.severity}</span>
                <span class="badge ${a.status.toLowerCase()}">${a.status}</span>
            </div>
            <h3>${safeHTML(a.customerName)}</h3>
            <p>${a.reasons.map(r => `• ${safeHTML(r)}`).join('<br>')}</p>
            <div class="alert-footer">
                <span>Case ID: #${a.id}</span>
                <time>${date.format(new Date(a.createdAt))}</time>
            </div>
        </div>
    `).join("");
}

// Attach Tab Filters events
elements.alertsTabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
        elements.alertsTabBtns.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        state.alertFilter = btn.dataset.alertFilter;
        renderAlertsQueue();
    });
});

// Case drawer Details
async function openAlertDrawer(id) {
    try {
        const alert = await request(`/api/alerts/${id}`);
        const history = await request(`/api/alerts/${id}/history`);
        const tx = await request(`/api/transactions/${alert.transactionId}`);

        state.selectedAlert = alert;

        elements.caseTitle.textContent = `Alert Case File #${alert.id}`;
        elements.caseBadgeStatus.textContent = alert.status;
        elements.caseBadgeStatus.className = `badge ${alert.status.toLowerCase()}`;
        elements.caseBadgeSeverity.textContent = alert.severity;
        elements.caseBadgeSeverity.className = `badge ${alert.severity.toLowerCase()}`;
        elements.caseScore.textContent = alert.riskScore;
        elements.caseScore.style.color = alert.riskScore >= 60 ? 'var(--danger-color)' : alert.riskScore >= 30 ? 'var(--warning-color)' : 'var(--success-color)';

        elements.caseCustomerName.textContent = alert.customerName;
        elements.caseTime.textContent = date.format(new Date(alert.createdAt));

        // Reasons
        elements.caseReasons.innerHTML = alert.reasons.map(r => `<div>• ${safeHTML(r)}</div>`).join("");

        // Associated Tx details
        elements.caseTxId.textContent = `#${tx.id}`;
        elements.caseTxAmount.textContent = money.format(tx.amount);
        elements.caseTxPayee.textContent = tx.payeeId;
        elements.caseTxCountry.textContent = tx.transactionCountry;
        elements.caseTxDescription.textContent = tx.description || 'No description provided';

        // Operator input & timeline history
        elements.operatorNotes.value = '';
        renderAlertHistory(history);
        renderOperatorWorkflowActions(alert.status);

        elements.caseDrawer.classList.add('open');
    } catch (err) {
        toast(err.message, 'error');
    }
}

function renderAlertHistory(history) {
    if (history.length === 0) {
        elements.caseHistoryTimeline.innerHTML = `<div class="empty">No status audit history.</div>`;
        return;
    }

    elements.caseHistoryTimeline.innerHTML = history.map(item => `
        <div class="timeline-item active">
            <span class="timeline-time">${date.format(new Date(item.changedAt))}</span>
            <div class="timeline-desc">Status changed to: <span class="badge ${item.status.toLowerCase()}">${item.status}</span></div>
            ${item.operatorNotes ? `<div class="timeline-notes">"${safeHTML(item.operatorNotes)}"</div>` : ''}
        </div>
    `).join("");
}

function renderOperatorWorkflowActions(status) {
    const act = elements.caseActionSection;
    const btnBox = elements.caseActionButtons;

    if (['CLOSED', 'DISMISSED'].includes(status)) {
        act.style.display = 'none';
        return;
    }

    act.style.display = 'block';
    
    // Open workflow options
    if (status === 'OPEN') {
        btnBox.innerHTML = `
            <div class="action-buttons-wrap-row">
                <button class="button small" onclick="updateAlertStatus('ACKNOWLEDGED')">Acknowledge</button>
                <button class="button secondary small" onclick="updateAlertStatus('DISMISSED')">Dismiss (False Alarm)</button>
            </div>
        `;
    } else if (status === 'ACKNOWLEDGED') {
        btnBox.innerHTML = `
            <div class="action-buttons-wrap-row">
                <button class="button small" onclick="updateAlertStatus('INVESTIGATING')">Mark Investigating</button>
                <button class="button secondary small" onclick="updateAlertStatus('DISMISSED')">Dismiss Alert</button>
            </div>
        `;
    } else if (status === 'INVESTIGATING') {
        btnBox.innerHTML = `
            <div class="action-buttons-wrap-row">
                <button class="button small" onclick="updateAlertStatus('CLOSED')">Resolve & Close Case</button>
                <button class="button secondary small" onclick="updateAlertStatus('DISMISSED')">Dismiss Case</button>
            </div>
        `;
    }
}

async function updateAlertStatus(nextStatus) {
    if (!state.selectedAlert) return;
    const notes = elements.operatorNotes.value.trim();

    try {
        await request(`/api/alerts/${state.selectedAlert.id}/status`, {
            method: 'PATCH',
            body: JSON.stringify({ status: nextStatus, notes })
        });
        toast(`Case successfully moved to ${nextStatus}`);
        closeDrawer();
        await syncData();
    } catch (err) {
        toast(err.message, 'error');
    }
}

// Window reference so buttons can invoke status changes
window.updateAlertStatus = updateAlertStatus;

function closeDrawer() {
    elements.caseDrawer.classList.remove('open');
    state.selectedAlert = null;
}

elements.closeCaseDrawer.addEventListener('click', closeDrawer);

// ----------------------------------------------------
// Tab 4: Rules Configurator CRUD
// ----------------------------------------------------
function renderRulesGrid() {
    elements.ruleRows.innerHTML = state.rules.map(rule => {
        // Human readable params format
        let paramsDesc = '';
        try {
            const p = JSON.parse(rule.parameters);
            if (rule.type === 'AMOUNT_THRESHOLD') paramsDesc = `Exceeds: <strong>${money.format(p.threshold)}</strong>`;
            else if (rule.type === 'VELOCITY') paramsDesc = `Limit: <strong>${p.maxCount} tx</strong> within <strong>${p.timeWindowMinutes} mins</strong>`;
            else if (rule.type === 'NEW_PAYEE') paramsDesc = `Triggers on previously unseen payee accounts`;
            else if (rule.type === 'DAILY_LIMIT') paramsDesc = `Daily cumulative limit: <strong>${money.format(p.dailyLimit)}</strong>`;
        } catch (_) { paramsDesc = rule.parameters; }

        return `
            <tr>
                <td>#${rule.id}</td>
                <td><strong>${safeHTML(rule.name)}</strong></td>
                <td><code style="background:rgba(255,255,255,0.05); padding:2px 6px; border-radius:4px">${rule.type}</code></td>
                <td><span class="badge ${rule.severity.toLowerCase()}">${rule.severity}</span></td>
                <td><small>${paramsDesc}</small></td>
                <td>
                    <label class="switch">
                        <input type="checkbox" ${rule.isActive ? 'checked' : ''} onclick="toggleRuleActive(${rule.id})">
                        <span class="slider"></span>
                    </label>
                </td>
                <td>
                    <div style="display:flex; gap:8px;">
                        <button class="button small secondary" onclick="editRuleModal(${rule.id})">Edit</button>
                        <button class="button small secondary" style="color:var(--danger-color); border-color:rgba(239,68,68,0.2)" onclick="deleteRuleClick(${rule.id})">Delete</button>
                    </div>
                </td>
            </tr>
        `;
    }).join("");
}

async function toggleRuleActive(id) {
    try {
        await request(`/api/rules/${id}/toggle`, { method: 'PATCH' });
        toast("Rule status toggled successfully");
        await syncData();
    } catch (err) {
        toast(err.message, 'error');
    }
}
window.toggleRuleActive = toggleRuleActive;

// Open rule modal
elements.btnNewRule.addEventListener('click', () => {
    elements.ruleForm.reset();
    elements.ruleId.value = '';
    elements.ruleFormError.textContent = '';
    elements.ruleModalTitle.textContent = 'Add Monitoring Rule';
    toggleRuleTypeFields();
    elements.ruleDialog.showModal();
});

elements.closeRuleDialog.addEventListener('click', () => elements.ruleDialog.close());
elements.cancelRuleDialog.addEventListener('click', () => elements.ruleDialog.close());

elements.ruleType.addEventListener('change', toggleRuleTypeFields);

function toggleRuleTypeFields() {
    const type = elements.ruleType.value;
    elements.paramAmountThreshold.style.display = type === 'AMOUNT_THRESHOLD' ? 'block' : 'none';
    elements.paramVelocity.style.display = type === 'VELOCITY' ? 'block' : 'none';
    elements.paramNewPayee.style.display = type === 'NEW_PAYEE' ? 'block' : 'none';
    elements.paramDailyLimit.style.display = type === 'DAILY_LIMIT' ? 'block' : 'none';
}

function editRuleModal(id) {
    const rule = state.rules.find(r => r.id === id);
    if (!rule) return;

    elements.ruleFormError.textContent = '';
    elements.ruleId.value = rule.id;
    elements.ruleName.value = rule.name;
    elements.ruleType.value = rule.type;
    elements.ruleSeverity.value = rule.severity;
    elements.ruleActive.checked = rule.isActive;

    toggleRuleTypeFields();

    // Populate parameters
    try {
        const p = JSON.parse(rule.parameters);
        if (rule.type === 'AMOUNT_THRESHOLD') elements.inputThreshold.value = p.threshold;
        else if (rule.type === 'VELOCITY') {
            elements.inputVelocityMins.value = p.timeWindowMinutes;
            elements.inputVelocityMaxCount.value = p.maxCount;
        } else if (rule.type === 'DAILY_LIMIT') elements.inputDailyLimit.value = p.dailyLimit;
    } catch (_) {}

    elements.ruleModalTitle.textContent = 'Edit Monitoring Rule';
    elements.ruleDialog.showModal();
}
window.editRuleModal = editRuleModal;

elements.ruleForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    elements.ruleFormError.textContent = '';

    const id = elements.ruleId.value;
    const type = elements.ruleType.value;
    
    // Construct parameters JSON
    let paramsObj = {};
    if (type === 'AMOUNT_THRESHOLD') paramsObj = { threshold: Number(elements.inputThreshold.value) };
    else if (type === 'VELOCITY') paramsObj = { timeWindowMinutes: Number(elements.inputVelocityMins.value), maxCount: Number(elements.inputVelocityMaxCount.value) };
    else if (type === 'NEW_PAYEE') paramsObj = {};
    else if (type === 'DAILY_LIMIT') paramsObj = { dailyLimit: Number(elements.inputDailyLimit.value) };

    const body = {
        id: id ? Number(id) : 0,
        name: elements.ruleName.value.trim(),
        type: type,
        severity: elements.ruleSeverity.value,
        parameters: JSON.stringify(paramsObj),
        isActive: elements.ruleActive.checked
    };

    try {
        if (id) {
            await request(`/api/rules/${id}`, {
                method: 'PUT',
                body: JSON.stringify(body)
            });
            toast("Rule updated successfully");
        } else {
            await request('/api/rules', {
                method: 'POST',
                body: JSON.stringify(body)
            });
            toast("New rule created successfully");
        }
        elements.ruleDialog.close();
        await syncData();
    } catch (err) {
        elements.ruleFormError.textContent = err.message;
    }
});

async function deleteRuleClick(id) {
    if (!confirm("Are you sure you want to delete this monitoring rule? This cannot be undone.")) return;
    try {
        await request(`/api/rules/${id}`, { method: 'DELETE' });
        toast("Rule deleted successfully");
        await syncData();
    } catch (err) {
        toast(err.message, 'error');
    }
}
window.deleteRuleClick = deleteRuleClick;

// ----------------------------------------------------
// Demo Data Seeder
// ----------------------------------------------------
elements.seedButton.addEventListener("click", async (e) => {
    e.currentTarget.disabled = true;
    try {
        const result = await request("/api/demo", { method: "POST", body: "{}" });
        toast(`Loaded ${result.transactions} transactions and generated ${result.alerts} alerts!`);
        await syncData();
    } catch (error) {
        toast(error.message, 'error');
    } finally {
        e.currentTarget.disabled = false;
    }
});

// View transaction details in a simple alert/toast
async function viewTransactionDetails(id) {
    try {
        const tx = await request(`/api/transactions/${id}`);
        // Let's draw an alert popup or open the transaction drawer details
        let details = `Transaction #${tx.id} Details:\n` +
                      `Customer: ${tx.customerName} (ID: ${tx.customerId})\n` +
                      `Amount: ${money.format(tx.amount)}\n` +
                      `Payee ID: ${tx.payeeId}\n` +
                      `Country: ${tx.transactionCountry}\n` +
                      `Time: ${date.format(new Date(tx.timestamp))}\n` +
                      `Status: ${tx.status}\n` +
                      `Risk Score: ${tx.riskScore}\n`;
        if (tx.reasons.length > 0) {
            details += `Reasons:\n` + tx.reasons.map(r => ` - ${r}`).join('\n');
        }
        alert(details);
    } catch (err) {
        toast(err.message, 'error');
    }
}
window.viewTransactionDetails = viewTransactionDetails;

// ----------------------------------------------------
// Tab 5: Customer Directory & Profile Lifecycle
// ----------------------------------------------------
function renderCustomersGrid() {
    const rows = state.customers.map(c => `
        <tr>
            <td><strong>#${c.id}</strong></td>
            <td><strong>${safeHTML(c.name)}</strong></td>
            <td><code>${safeHTML(c.accountNumber)}</code></td>
            <td>${safeHTML(c.registeredCountry)}</td>
            <td>
                <div class="actions-group">
                    <button class="button secondary btn-small" onclick="editCustomerModal(${c.id})">Edit</button>
                    <button class="button secondary danger-btn btn-small" onclick="deleteCustomerClick(${c.id})">Delete</button>
                </div>
            </td>
        </tr>
    `).join("");

    elements.customerRows.innerHTML = rows;
    elements.customerEmpty.style.display = state.customers.length === 0 ? 'block' : 'none';
}

// Trigger modal add customer
elements.btnNewCustomer.addEventListener('click', () => {
    elements.customerForm.reset();
    elements.customerFormId.value = '';
    elements.customerFormError.textContent = '';
    elements.customerModalTitle.textContent = 'Add New Customer';
    elements.customerDialog.showModal();
});

elements.closeCustomerDialog.addEventListener('click', () => elements.customerDialog.close());
elements.cancelCustomerDialog.addEventListener('click', () => elements.customerDialog.close());

elements.customerForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    elements.customerFormError.textContent = '';

    const id = elements.customerFormId.value;
    const body = {
        id: id ? Number(id) : 0,
        name: elements.customerFormName.value.trim(),
        accountNumber: elements.customerFormAccount.value.trim(),
        registeredCountry: elements.customerFormCountry.value.trim()
    };

    try {
        if (id) {
            await request(`/api/customers/${id}`, {
                method: 'PUT',
                body: JSON.stringify(body)
            });
            toast("Customer updated successfully");
        } else {
            await request('/api/customers', {
                method: 'POST',
                body: JSON.stringify(body)
            });
            toast("Customer added successfully");
        }
        elements.customerDialog.close();
        await syncData();
    } catch (err) {
        elements.customerFormError.textContent = err.message;
    }
});

function editCustomerModal(id) {
    const c = state.customers.find(item => item.id === id);
    if (!c) return;

    elements.customerFormError.textContent = '';
    elements.customerFormId.value = c.id;
    elements.customerFormName.value = c.name;
    elements.customerFormAccount.value = c.accountNumber;
    elements.customerFormCountry.value = c.registeredCountry;

    elements.customerModalTitle.textContent = 'Edit Customer Profile';
    elements.customerDialog.showModal();
}
window.editCustomerModal = editCustomerModal;

async function deleteCustomerClick(id) {
    if (!confirm("Are you sure you want to delete this customer? This will cascade delete their transactions and alerts.")) return;
    try {
        await request(`/api/customers/${id}`, { method: 'DELETE' });
        toast("Customer deleted successfully");
        await syncData();
    } catch (err) {
        toast(err.message, 'error');
    }
}
window.deleteCustomerClick = deleteCustomerClick;

// ----------------------------------------------------
// User Authentication Event Handlers
// ----------------------------------------------------
elements.loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    elements.loginError.textContent = '';

    const body = {
        username: elements.loginUsername.value.trim(),
        password: elements.loginPassword.value
    };

    try {
        const res = await request('/api/auth/login', {
            method: 'POST',
            body: JSON.stringify(body)
        });
        localStorage.setItem("monitorx_token", res.token);
        localStorage.setItem("monitorx_user", res.username);
        elements.loginOverlay.style.display = "none";
        toast(`Welcome back, ${res.username}!`);
        await syncData();
    } catch (err) {
        elements.loginError.textContent = err.message;
    }
});

elements.btnLogout.addEventListener('click', async () => {
    try {
        await request('/api/auth/logout', { method: 'POST' });
    } catch (_) {}
    handleUnauthorized();
    toast("Logged out successfully");
});

// Initial bootstrap
async function bootstrap() {
    initNavigation();
    
    // Check local token validity on startup
    const token = localStorage.getItem("monitorx_token");
    if (!token) {
        handleUnauthorized();
    } else {
        try {
            await syncData();
        } catch (_) {
            handleUnauthorized();
        }
    }
    
    // Auto-update stats graph on window resize
    window.addEventListener('resize', renderOverviewVisualizations);
}

bootstrap().catch(err => toast(err.message, 'error'));