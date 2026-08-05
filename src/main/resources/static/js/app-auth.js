// ============================================================
// app-auth.js — Login, logout, and demo data loader
// ============================================================

elements.loginForm.addEventListener('submit', async e => {
    e.preventDefault();
    elements.loginError.textContent = '';
    const body = {
        username: elements.loginUsername.value.trim(),
        password: elements.loginPassword.value
    };
    try {
        const res = await request('/api/auth/login', { method: 'POST', body: JSON.stringify(body) });
        localStorage.setItem('monitorx_token', res.token);
        localStorage.setItem('monitorx_user',  res.username);
        elements.loginOverlay.style.display = 'none';
        toast(`Welcome back, ${res.username}!`);
        await syncData();
    } catch (err) {
        elements.loginError.textContent = err.message;
    }
});

elements.btnLogout.addEventListener('click', async () => {
    try { await request('/api/auth/logout', { method: 'POST' }); } catch (_) {}
    handleUnauthorized();
    toast('Logged out successfully');
});

// ── Demo Data Loader ──────────────────────────────────────────────────────────
elements.seedButton.addEventListener('click', async e => {
    e.currentTarget.disabled = true;
    try {
        const status = await request('/api/demo/status');
        let force = false;

        if (status.hasData) {
            const confirmed = window.confirm(
                `Database already has ${status.transactions} transactions and ${status.alerts} alerts.\n\n`
                + `OK = CLEAR all data and reload fresh 1000 transactions.\n`
                + `Cancel = keep existing data.`
            );
            if (!confirmed) {
                toast(`Kept existing data (${status.transactions} transactions).`);
                e.currentTarget.disabled = false;
                return;
            }
            force = true;
        }

        toast('Loading 1000 transactions... please wait (~2 min).');
        const result = await request(`/api/demo?force=${force}`, { method: 'POST', body: '{}' });

        if (result.skipped) {
            toast(result.message);
        } else {
            toast(`✓ Loaded ${result.customers} customers · ${result.transactions} transactions · ${result.alerts} alerts.`);
        }
        await syncData();
    } catch (err) {
        toast(err.message, 'error');
    } finally {
        e.currentTarget.disabled = false;
    }
});
