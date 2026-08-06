// ============================================================
// app-dashboard.js — Overview chart, alert stats, activity log,
//                    summary metric cards, quick alerts list
// ============================================================

function renderSummary(summary) {
    elements.metricTotal.textContent        = summary.totalTransactions;
    elements.metricFlagged.textContent      = summary.flaggedTransactions;
    elements.metricActiveAlerts.textContent = summary.openAlerts;
    elements.metricVolume.textContent       = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(summary.totalVolume);

    const activeCount = state.alerts.filter(
        a => ['OPEN', 'ACKNOWLEDGED', 'INVESTIGATING'].includes(a.status)
    ).length;
    if (activeCount > 0) {
        elements.sidebarAlertBadge.textContent    = activeCount;
        elements.sidebarAlertBadge.style.display  = 'inline-block';
    } else {
        elements.sidebarAlertBadge.style.display = 'none';
    }
}

// ── Activity Log ──────────────────────────────────────────────────────────────
async function loadActivityLog() {
    try {
        const activity = await request('/api/activity');
        renderActivityLog(activity);
    } catch (_) { /* non-critical */ }
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
    const statusClass   = {
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
            <td><span class="badge ${statusClass[e.status]   || ''}">${safeHTML(e.status)}</span></td>
            <td style="font-size:0.82rem;color:var(--text-secondary);max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;"
                title="${safeHTML(e.operatorNotes)}">
                ${safeHTML(e.operatorNotes) || '<em style="color:var(--text-muted)">—</em>'}
            </td>
        </tr>
    `).join('');
}

// ── Volume / Velocity Chart ───────────────────────────────────────────────────
function renderOverviewVisualizations() {
    const canvas = elements.overviewChart;
    if (!canvas) return;

    const container = canvas.parentElement;
    canvas.width  = container.clientWidth;
    canvas.height = container.clientHeight;

    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    const txs = [...state.transactions].reverse(); // oldest first
    if (txs.length === 0) {
        ctx.fillStyle = '#94A3B8';
        ctx.font      = "13px 'Plus Jakarta Sans', sans-serif";
        ctx.textAlign = 'center';
        ctx.fillText('No transaction data yet.', canvas.width / 2, canvas.height / 2);
    } else {
        const MAX_POINTS = 120;
        const step    = Math.max(1, Math.floor(txs.length / MAX_POINTS));
        const sampled = txs.filter((_, i) => i % step === 0);

        const pL = 50, pR = 20, pT = 30, pB = 40;
        const gW = canvas.width  - pL - pR;
        const gH = canvas.height - pT - pB;

        const maxAmount = Math.max(...sampled.map(t => Number(t.amount)), 1000);

        ctx.fillStyle = '#F7FAFD';
        ctx.fillRect(pL, pT, gW, gH);

        // Gridlines
        for (let i = 0; i <= 3; i++) {
            const y   = pT + (gH * i) / 3;
            const val = maxAmount - (maxAmount * i) / 3;
            ctx.strokeStyle = i === 3 ? '#C8D6EF' : '#E8EEF7';
            ctx.lineWidth   = 1;
            ctx.setLineDash(i === 0 ? [] : [3, 3]);
            ctx.beginPath();
            ctx.moveTo(pL, y);
            ctx.lineTo(canvas.width - pR, y);
            ctx.stroke();
            ctx.setLineDash([]);
            ctx.fillStyle  = '#7B96BC';
            ctx.font       = '9px Plus Jakarta Sans, sans-serif';
            ctx.textAlign  = 'right';
            const lbl = val >= 1e6 ? '$'+(val/1e6).toFixed(1)+'M'
                      : val >= 1e3 ? '$'+(val/1e3).toFixed(0)+'K'
                      : '$'+Math.round(val);
            ctx.fillText(lbl, pL - 5, y + 3);
        }

        // Axis border
        ctx.strokeStyle = '#C8D6EF';
        ctx.lineWidth   = 1.5;
        ctx.beginPath();
        ctx.moveTo(pL, pT);
        ctx.lineTo(pL, pT + gH);
        ctx.lineTo(pL + gW, pT + gH);
        ctx.stroke();

        // Data points
        const points = sampled.map((t, idx) => ({
            x: pL + (gW * idx) / Math.max(sampled.length - 1, 1),
            y: pT + gH - (Number(t.amount) / maxAmount) * gH,
            transaction: t
        }));

        // Area fill
        if (points.length > 1) {
            ctx.beginPath();
            ctx.moveTo(points[0].x, pT + gH);
            points.forEach(p => ctx.lineTo(p.x, p.y));
            ctx.lineTo(points[points.length - 1].x, pT + gH);
            ctx.closePath();
            const grad = ctx.createLinearGradient(0, pT, 0, pT + gH);
            grad.addColorStop(0, 'rgba(100,130,200,0.10)');
            grad.addColorStop(1, 'rgba(100,130,200,0.01)');
            ctx.fillStyle = grad;
            ctx.fill();
        }

        // Line
        ctx.beginPath();
        points.forEach((p, i) => i === 0 ? ctx.moveTo(p.x, p.y) : ctx.lineTo(p.x, p.y));
        ctx.strokeStyle = '#7A9CC8';
        ctx.lineWidth   = 1.8;
        ctx.lineJoin    = 'round';
        ctx.stroke();

        // Dots for small datasets
        if (sampled.length <= 60) {
            points.forEach(p => {
                ctx.beginPath();
                ctx.arc(p.x, p.y, 3, 0, 2 * Math.PI);
                ctx.fillStyle = p.transaction.status === 'FLAGGED' ? '#C49090' : '#7BB59A';
                ctx.fill();
            });
        }

        // Date range labels
        ctx.fillStyle  = '#7B96BC';
        ctx.font       = '9px Plus Jakarta Sans, sans-serif';
        const fmtDate  = d => new Date(d).toLocaleDateString('en-GB', { day: '2-digit', month: 'short' });
        ctx.textAlign  = 'left';
        ctx.fillText(fmtDate(sampled[0].timestamp), pL, pT + gH + 14);
        ctx.textAlign  = 'right';
        ctx.fillText(fmtDate(sampled[sampled.length - 1].timestamp), pL + gW, pT + gH + 14);
        ctx.textAlign  = 'center';
        ctx.fillText(`${sampled.length} data points`, pL + gW / 2, pT + gH + 14);
    }

    // ── Alert Status KPI tiles ─────────────────────────────────────────────────
    const counts = { OPEN: 0, ACKNOWLEDGED: 0, INVESTIGATING: 0, CLOSED: 0, DISMISSED: 0 };
    state.alerts.forEach(a => { if (counts[a.status] !== undefined) counts[a.status]++; });
    const total = state.alerts.length || 1;

    const statusMeta = {
        OPEN:          { color: '#C49090', bg: '#FAF0F0', label: 'Open' },
        ACKNOWLEDGED:  { color: '#C4A970', bg: '#FAF5E8', label: 'Acknowledged' },
        INVESTIGATING: { color: '#7A9CC8', bg: '#EEF4FA', label: 'Investigating' },
        CLOSED:        { color: '#7BB5A0', bg: '#EEF7F3', label: 'Closed' },
        DISMISSED:     { color: '#A8B5C8', bg: '#F2F4F8', label: 'Dismissed' }
    };

    elements.alertDistributionDonut.innerHTML = `
        <div class="ast-header">
            <div class="ast-total">
                <span class="ast-total-num">${state.alerts.length}</span>
                <span class="ast-total-lbl">Total Alerts</span>
            </div>
            <div class="ast-stacked">
                ${Object.entries(counts).map(([s, c]) =>
                    c > 0 ? `<div style="flex:${c};background:${statusMeta[s].color};"
                                  title="${statusMeta[s].label}: ${c}"></div>` : ''
                ).join('')}
            </div>
        </div>`;

    elements.alertDistributionLegend.innerHTML = Object.entries(counts).map(([status, count]) => {
        const pct  = (count / total * 100).toFixed(0);
        const meta = statusMeta[status];
        return `
        <li class="ast-tile" style="background:${meta.bg};border-color:${meta.color}30;">
            <div class="ast-tile-top">
                <span class="ast-tile-dot"  style="background:${meta.color};"></span>
                <span class="ast-tile-name">${meta.label}</span>
            </div>
            <div class="ast-tile-bottom">
                <span class="ast-tile-num">${count}</span>
                <span class="ast-tile-pct">${pct}%</span>
            </div>
        </li>`;
    }).join('');

    // ── Recent Critical Alerts list ────────────────────────────────────────────
    const criticalAlerts = state.alerts
        .filter(a => ['OPEN', 'ACKNOWLEDGED', 'INVESTIGATING'].includes(a.status))
        .slice(0, 5);

    elements.quickAlertList.innerHTML = criticalAlerts.length === 0
        ? `<div class="empty">No active alerts. Cases are completely resolved!</div>`
        : criticalAlerts.map(alert => `
            <div class="quick-alert-row" onclick="openAlertDrawer(${alert.id})">
                <div class="quick-alert-desc">
                    <span class="quick-alert-title">${safeHTML(alert.customerName)}</span>
                    <span class="quick-alert-subtitle">${safeHTML(alert.reasons[0])}</span>
                </div>
                <div style="display:flex;gap:10px;align-items:center;">
                    <span class="badge ${alert.severity.toLowerCase()}">${alert.severity}</span>
                    <span class="badge ${alert.status.toLowerCase()}">${alert.status}</span>
                </div>
            </div>`).join('');
}
