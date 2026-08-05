// ============================================================
// app-rules.js — Rules table (read-only display) + CRUD modal
// ============================================================

function renderRulesGrid() {
    elements.ruleRows.innerHTML = state.rules.map(rule => {
        let paramsDesc = '';
        try {
            const p = JSON.parse(rule.parameters);
            if      (rule.type === 'AMOUNT_THRESHOLD') paramsDesc = `Exceeds: <strong>${money.format(p.threshold)}</strong>`;
            else if (rule.type === 'VELOCITY')         paramsDesc = `Limit: <strong>${p.maxCount} tx</strong> within <strong>${p.timeWindowMinutes} mins</strong>`;
            else if (rule.type === 'NEW_PAYEE')        paramsDesc = `Triggers on previously unseen payee accounts`;
            else if (rule.type === 'DAILY_LIMIT')      paramsDesc = `Daily cumulative limit: <strong>${money.format(p.dailyLimit)}</strong>`;
        } catch (_) { paramsDesc = rule.parameters; }

        return `
        <tr>
            <td>#${rule.id}</td>
            <td><strong>${safeHTML(rule.name)}</strong></td>
            <td><code style="background:var(--accent-light);color:var(--accent-color);
                             padding:2px 6px;border-radius:4px;font-size:0.75rem;">${rule.type}</code></td>
            <td><span class="badge ${rule.severity.toLowerCase()}">${rule.severity}</span></td>
            <td><small>${paramsDesc}</small></td>
        </tr>`;
    }).join('');
}

async function toggleRuleActive(id) {
    try {
        await request(`/api/rules/${id}/toggle`, { method: 'PATCH' });
        toast('Rule status toggled successfully');
        await syncData();
    } catch (err) {
        toast(err.message, 'error');
    }
}
window.toggleRuleActive = toggleRuleActive;

function toggleRuleTypeFields() {
    const type = elements.ruleType.value;
    elements.paramAmountThreshold.style.display = type === 'AMOUNT_THRESHOLD' ? 'block' : 'none';
    elements.paramVelocity.style.display        = type === 'VELOCITY'         ? 'block' : 'none';
    elements.paramNewPayee.style.display        = type === 'NEW_PAYEE'        ? 'block' : 'none';
    elements.paramDailyLimit.style.display      = type === 'DAILY_LIMIT'      ? 'block' : 'none';
}

function editRuleModal(id) {
    const rule = state.rules.find(r => r.id === id);
    if (!rule) return;

    elements.ruleFormError.textContent = '';
    elements.ruleId.value        = rule.id;
    elements.ruleName.value      = rule.name;
    elements.ruleType.value      = rule.type;
    elements.ruleSeverity.value  = rule.severity;
    elements.ruleActive.checked  = rule.isActive;
    toggleRuleTypeFields();

    try {
        const p = JSON.parse(rule.parameters);
        if      (rule.type === 'AMOUNT_THRESHOLD') elements.inputThreshold.value = p.threshold;
        else if (rule.type === 'VELOCITY') {
            elements.inputVelocityMins.value     = p.timeWindowMinutes;
            elements.inputVelocityMaxCount.value = p.maxCount;
        }
        else if (rule.type === 'DAILY_LIMIT') elements.inputDailyLimit.value = p.dailyLimit;
    } catch (_) {}

    elements.ruleModalTitle.textContent = 'Edit Monitoring Rule';
    elements.ruleDialog.showModal();
}
window.editRuleModal = editRuleModal;

async function deleteRuleClick(id) {
    if (!confirm('Are you sure you want to delete this monitoring rule?')) return;
    try {
        await request(`/api/rules/${id}`, { method: 'DELETE' });
        toast('Rule deleted successfully');
        await syncData();
    } catch (err) {
        toast(err.message, 'error');
    }
}
window.deleteRuleClick = deleteRuleClick;

// ── Event Listeners ───────────────────────────────────────────────────────────
if (elements.btnNewRule) {
    elements.btnNewRule.addEventListener('click', () => {
        elements.ruleForm.reset();
        elements.ruleId.value              = '';
        elements.ruleFormError.textContent = '';
        elements.ruleModalTitle.textContent = 'Add Monitoring Rule';
        toggleRuleTypeFields();
        elements.ruleDialog.showModal();
    });
}

elements.closeRuleDialog.addEventListener('click',  () => elements.ruleDialog.close());
elements.cancelRuleDialog.addEventListener('click', () => elements.ruleDialog.close());
elements.ruleType.addEventListener('change', toggleRuleTypeFields);

elements.ruleForm.addEventListener('submit', async e => {
    e.preventDefault();
    elements.ruleFormError.textContent = '';
    const id   = elements.ruleId.value;
    const type = elements.ruleType.value;

    const paramsMap = {
        AMOUNT_THRESHOLD: { threshold: Number(elements.inputThreshold.value) },
        VELOCITY:         { timeWindowMinutes: Number(elements.inputVelocityMins.value), maxCount: Number(elements.inputVelocityMaxCount.value) },
        NEW_PAYEE:        {},
        DAILY_LIMIT:      { dailyLimit: Number(elements.inputDailyLimit.value) }
    };

    const body = {
        id:         id ? Number(id) : 0,
        name:       elements.ruleName.value.trim(),
        type,
        severity:   elements.ruleSeverity.value,
        parameters: JSON.stringify(paramsMap[type] || {}),
        isActive:   elements.ruleActive.checked
    };

    try {
        if (id) {
            await request(`/api/rules/${id}`, { method: 'PUT',  body: JSON.stringify(body) });
            toast('Rule updated successfully');
        } else {
            await request('/api/rules',        { method: 'POST', body: JSON.stringify(body) });
            toast('New rule created successfully');
        }
        elements.ruleDialog.close();
        await syncData();
    } catch (err) {
        elements.ruleFormError.textContent = err.message;
    }
});
