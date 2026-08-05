// ============================================================
// app-transactions.js — Transaction feed, filters, form,
//                       customer dropdown, currency label
// ============================================================

function updateAmountLabel() {
    const country = elements.country.value.trim();
    const code    = country ? (countryCurrency[country.toLowerCase()] || 'USD') : 'USD';
    const symbols = {
        'INR': '₹ INR', 'USD': '$ USD', 'GBP': '£ GBP', 'EUR': '€ EUR',
        'AED': 'AED',   'JPY': '¥ JPY', 'SGD': 'S$ SGD','CNY': '¥ CNY',
        'BRL': 'R$ BRL','CAD': 'CA$ CAD','AUD': 'A$ AUD',
        'CHF': 'CHF',   'ZAR': 'R ZAR'
    };
    const label = document.getElementById('amountLabel');
    if (label) label.textContent = `Transaction Amount (${symbols[code] || code})`;
}

function setCustomerCountry() {
    const option = elements.customerId.selectedOptions[0];
    if (option) elements.country.value = option.dataset.country;
    updateAmountLabel();
}

function populateCustomersDropdown() {
    elements.customerId.innerHTML = state.customers.map(c =>
        `<option value="${c.id}" data-country="${safeHTML(c.registeredCountry)}">
            ${safeHTML(c.name)} · ${c.accountNumber}
         </option>`
    ).join('');

    elements.filterCustomer.innerHTML =
        `<option value="">All Customers</option>` +
        state.customers.map(c =>
            `<option value="${c.id}">${safeHTML(c.name)}</option>`
        ).join('');

    setCustomerCountry();
}

function renderTransactionsFeed() {
    const searchVal   = elements.searchDesc.value.toLowerCase().trim();
    const customerVal = elements.filterCustomer.value;
    const amountMin   = Number(elements.filterAmountMin.value) || 0;
    const amountMax   = Number(elements.filterAmountMax.value) || Infinity;
    const statusVal   = elements.filterStatus.value;

    const filtered = state.transactions.filter(t => {
        const matchesSearch   = String(t.id).includes(searchVal) || t.description.toLowerCase().includes(searchVal);
        const matchesCustomer = !customerVal || t.customerId === Number(customerVal);
        const amt             = Number(t.amount);
        const matchesAmount   = amt >= amountMin && amt <= amountMax;
        const matchesStatus   = !statusVal || t.status === statusVal;
        return matchesSearch && matchesCustomer && matchesAmount && matchesStatus;
    });

    elements.transactionEmpty.style.display = filtered.length === 0 ? 'block' : 'none';
    elements.transactionRows.innerHTML = filtered.map(t => `
        <tr class="${t.status === 'FLAGGED' ? 'alert-row-highlight' : ''}"
            onclick="viewTransactionDetails(${t.id})">
            <td><strong>#${t.id}</strong></td>
            <td>
                <strong>${safeHTML(t.customerName)}</strong>
                <small style="display:block;color:var(--text-muted)">Cust ID: ${t.customerId}</small>
            </td>
            <td><code>${safeHTML(t.payeeId)}</code></td>
            <td><strong>${formatMoney(t.amount, t.transactionCountry)}</strong></td>
            <td><span class="badge ${t.riskScore >= 60 ? 'high' : t.riskScore >= 30 ? 'medium' : 'low'}">${t.riskScore}</span></td>
            <td>${safeHTML(t.transactionCountry)}</td>
            <td>${date.format(new Date(t.timestamp))}</td>
            <td><small>${safeHTML(t.description || 'N/A')}</small></td>
            <td><span class="badge ${t.status.toLowerCase()}">${t.status}</span></td>
        </tr>`
    ).join('');
}

async function viewTransactionDetails(id) {
    try {
        const tx = await request(`/api/transactions/${id}`);
        let details = `Transaction #${tx.id} Details:\n`
            + `Customer: ${tx.customerName} (ID: ${tx.customerId})\n`
            + `Amount: ${formatMoney(tx.amount, tx.transactionCountry)}\n`
            + `Payee ID: ${tx.payeeId}\n`
            + `Country: ${tx.transactionCountry}\n`
            + `Time: ${date.format(new Date(tx.timestamp))}\n`
            + `Status: ${tx.status}\n`
            + `Risk Score: ${tx.riskScore}\n`;
        if (tx.reasons.length > 0) {
            details += 'Reasons:\n' + tx.reasons.map(r => ` - ${r}`).join('\n');
        }
        alert(details);
    } catch (err) {
        toast(err.message, 'error');
    }
}
window.viewTransactionDetails = viewTransactionDetails;

// ── Event Listeners ───────────────────────────────────────────────────────────
elements.customerId.addEventListener('change', setCustomerCountry);
elements.country.addEventListener('input', updateAmountLabel);

elements.btnApplyFilters.addEventListener('click', renderTransactionsFeed);
elements.btnResetFilters.addEventListener('click', () => {
    elements.searchDesc.value     = '';
    elements.filterCustomer.value = '';
    elements.filterAmountMin.value = '';
    elements.filterAmountMax.value = '';
    elements.filterStatus.value   = '';
    renderTransactionsFeed();
});

elements.newTransactionButton.addEventListener('click', () => {
    elements.transactionForm.reset();
    elements.formError.textContent = '';
    setCustomerCountry();
    elements.timestamp.value = new Date().toISOString().substring(0, 16);
    elements.transactionDialog.showModal();
});

elements.closeDialog.addEventListener('click',  () => elements.transactionDialog.close());
elements.cancelDialog.addEventListener('click', () => elements.transactionDialog.close());

elements.transactionForm.addEventListener('submit', async e => {
    e.preventDefault();
    elements.formError.textContent = '';
    const body = {
        customerId:          Number(elements.customerId.value),
        amount:              Number(elements.amount.value),
        payeeId:             elements.payeeId.value.trim(),
        transactionCountry:  elements.country.value.trim(),
        timestamp:           elements.timestamp.value || null,
        description:         elements.description.value.trim()
    };
    try {
        const saved = await request('/api/transactions', { method: 'POST', body: JSON.stringify(body) });
        elements.transactionDialog.close();
        toast(saved.status === 'FLAGGED'
            ? 'Transaction FLAGGED by rules engine'
            : 'Transaction APPROVED successfully');
        await syncData();
    } catch (err) {
        elements.formError.textContent = err.message;
    }
});
