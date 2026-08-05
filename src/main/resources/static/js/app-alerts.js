// ============================================================
// app-alerts.js — Alert queue rendering, case drawer,
//                 history timeline, operator workflow actions
// ============================================================

function renderAlertsQueue() {
    let open = 0, ack = 0, inv = 0;
    state.alerts.forEach(a => {
        if (a.status === 'OPEN')          open++;
        else if (a.status === 'ACKNOWLEDGED') ack++;
        else if (a.status === 'INVESTIGATING') inv++;
    });
    elements.countOpen.textContent = open;
    elements.countAck.textContent  = ack;
    elements.countInv.textContent  = inv;

    const filter   = state.alertFilter;
    const filtered = state.alerts.filter(alert => {
        if (filter === 'ALL')    return true;
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
        </div>`
    ).join('');
}

elements.alertsTabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
        elements.alertsTabBtns.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        state.alertFilter = btn.dataset.alertFilter;
        renderAlertsQueue();
    });
});

// ── Case Drawer ───────────────────────────────────────────────────────────────
async function openAlertDrawer(id) {
    try {
        const [alert, history, tx] = await Promise.all([
            request(`/api/alerts/${id}`),
            request(`/api/alerts/${id}/history`),
            request(`/api/alerts/${id}`).then(a => request(`/api/transactions/${a.transactionId}`))
        ]);

        state.selectedAlert = alert;
        elements.caseTitle.textContent = `Alert Case File #${alert.id}`;

        const severityColors = { HIGH: 'var(--danger-color)', MEDIUM: 'var(--warning-color)', LOW: 'var(--success-color)' };
        document.getElementById('caseDrawer').style.borderTop = `4px solid ${severityColors[alert.severity] || 'var(--border-color)'}`;

        elements.caseBadgeStatus.textContent  = alert.status;
        elements.caseBadgeStatus.className    = `badge ${alert.status.toLowerCase()}`;
        elements.caseBadgeSeverity.textContent = alert.severity;
        elements.caseBadgeSeverity.className  = `badge ${alert.severity.toLowerCase()}`;
        elements.caseScore.textContent        = alert.riskScore;
        elements.caseScore.style.color        =
            alert.riskScore >= 60 ? 'var(--danger-color)'
          : alert.riskScore >= 30 ? 'var(--warning-color)'
          : 'var(--success-color)';
        elements.caseCustomerName.textContent = alert.customerName;
        elements.caseTime.textContent         = date.format(new Date(alert.createdAt));

        // Flag cards
        elements.caseReasons.innerHTML = alert.reasons.length
            ? alert.reasons.map(r => {
                const m = r.match(/^Rule \[(.+?)\]: (.+)$/);
                return m
                    ? `<div class="flag-card flag-rule">
                           <div class="flag-tag">Rule Violation</div>
                           <div class="flag-title">${safeHTML(m[1])}</div>
                           <div class="flag-desc">${safeHTML(m[2])}</div>
                       </div>`
                    : `<div class="flag-card flag-system">
                           <div class="flag-tag">System Flag</div>
                           <div class="flag-title">${safeHTML(r)}</div>
                       </div>`;
            }).join('')
            : '<div style="color:var(--text-muted);font-style:italic;font-size:0.82rem;">No triggers recorded.</div>';

        // Transaction details
        elements.caseTxId.textContent          = `#${tx.id}`;
        elements.caseTxAmount.textContent      = formatMoney(tx.amount, tx.transactionCountry);
        elements.caseTxPayee.textContent       = tx.payeeId;
        elements.caseTxCountry.textContent     = tx.transactionCountry;
        elements.caseTxDescription.textContent = tx.description || '—';

        elements.operatorNotes.value = '';
        renderAlertHistory(history);
        renderOperatorWorkflowActions(alert.status);
        elements.caseDrawer.classList.add('open');
    } catch (err) {
        toast(err.message, 'error');
    }
}
window.openAlertDrawer = openAlertDrawer;

function renderAlertHistory(history) {
    if (history.length === 0) {
        elements.caseHistoryTimeline.innerHTML = `<div class="empty">No status audit history.</div>`;
        return;
    }
    elements.caseHistoryTimeline.innerHTML = history.map(item => `
        <div class="timeline-item active">
            <span class="timeline-time">${date.format(new Date(item.changedAt))}</span>
            <div class="timeline-desc">Status changed to:
                <span class="badge ${item.status.toLowerCase()}">${item.status}</span>
            </div>
            ${item.operatorNotes
                ? `<div class="timeline-notes">"${safeHTML(item.operatorNotes)}"</div>`
                : ''}
        </div>`
    ).join('');
}

function renderOperatorWorkflowActions(status) {
    const act    = elements.caseActionSection;
    const btnBox = elements.caseActionButtons;

    if (['CLOSED', 'DISMISSED'].includes(status)) {
        act.style.display = 'none';
        return;
    }
    act.style.display = 'block';

    const actions = {
        OPEN: `
            <div class="action-buttons-wrap-row">
                <button class="button small" onclick="updateAlertStatus('ACKNOWLEDGED')">Acknowledge</button>
                <button class="button secondary small" onclick="updateAlertStatus('DISMISSED')">Dismiss (False Alarm)</button>
            </div>`,
        ACKNOWLEDGED: `
            <div class="action-buttons-wrap-row">
                <button class="button small" onclick="updateAlertStatus('INVESTIGATING')">Mark Investigating</button>
                <button class="button secondary small" onclick="updateAlertStatus('DISMISSED')">Dismiss Alert</button>
            </div>`,
        INVESTIGATING: `
            <div class="action-buttons-wrap-row">
                <button class="button small" onclick="updateAlertStatus('CLOSED')">Resolve & Close Case</button>
                <button class="button secondary small" onclick="updateAlertStatus('DISMISSED')">Dismiss Case</button>
            </div>`
    };
    btnBox.innerHTML = actions[status] || '';
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
window.updateAlertStatus = updateAlertStatus;
