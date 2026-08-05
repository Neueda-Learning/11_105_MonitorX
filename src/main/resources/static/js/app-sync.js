// ============================================================
// app-sync.js — Central data sync: fetches all API data and
//               calls all render functions
// ============================================================

async function syncData() {
    try {
        const [summary, txs, alerts, rules, custs] = await Promise.all([
            request('/api/summary'),
            request('/api/transactions'),
            request('/api/alerts'),
            request('/api/rules'),
            request('/api/customers')
        ]);

        state.transactions = txs;
        state.alerts       = alerts;
        state.rules        = rules;
        state.customers    = custs;

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
