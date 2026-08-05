// ============================================================
// app-customers.js — Customer directory table and CRUD modal
// ============================================================

function renderCustomersGrid() {
    elements.customerRows.innerHTML = state.customers.map(c => `
        <tr>
            <td><strong>#${c.id}</strong></td>
            <td><strong>${safeHTML(c.name)}</strong></td>
            <td><code>${safeHTML(c.accountNumber)}</code></td>
            <td>${safeHTML(c.registeredCountry)}</td>
            <td>
                <div class="actions-group" style="gap:6px;">
                    <button class="button secondary small" onclick="editCustomerModal(${c.id})">Edit</button>
                    <button class="button danger-btn small" onclick="deleteCustomerClick(${c.id})">Delete</button>
                </div>
            </td>
        </tr>`
    ).join('');
    elements.customerEmpty.style.display = state.customers.length === 0 ? 'block' : 'none';
}

function editCustomerModal(id) {
    const c = state.customers.find(item => item.id === id);
    if (!c) return;
    elements.customerFormError.textContent  = '';
    elements.customerFormId.value           = c.id;
    elements.customerFormName.value         = c.name;
    elements.customerFormAccount.value      = c.accountNumber;
    elements.customerFormCountry.value      = c.registeredCountry;
    elements.customerModalTitle.textContent = 'Edit Customer Profile';
    elements.customerDialog.showModal();
}
window.editCustomerModal = editCustomerModal;

async function deleteCustomerClick(id) {
    if (!confirm('Delete this customer? This will cascade delete their transactions and alerts.')) return;
    try {
        await request(`/api/customers/${id}`, { method: 'DELETE' });
        toast('Customer deleted successfully');
        await syncData();
    } catch (err) {
        toast(err.message, 'error');
    }
}
window.deleteCustomerClick = deleteCustomerClick;

// ── Event Listeners ───────────────────────────────────────────────────────────
elements.btnNewCustomer.addEventListener('click', () => {
    elements.customerForm.reset();
    elements.customerFormId.value           = '';
    elements.customerFormError.textContent  = '';
    elements.customerModalTitle.textContent = 'Add New Customer';
    elements.customerDialog.showModal();
});

elements.closeCustomerDialog.addEventListener('click',  () => elements.customerDialog.close());
elements.cancelCustomerDialog.addEventListener('click', () => elements.customerDialog.close());

elements.customerForm.addEventListener('submit', async e => {
    e.preventDefault();
    elements.customerFormError.textContent = '';
    const id   = elements.customerFormId.value;
    const body = {
        id:                id ? Number(id) : 0,
        name:              elements.customerFormName.value.trim(),
        accountNumber:     elements.customerFormAccount.value.trim(),
        registeredCountry: elements.customerFormCountry.value.trim()
    };
    try {
        if (id) {
            await request(`/api/customers/${id}`, { method: 'PUT',  body: JSON.stringify(body) });
            toast('Customer updated successfully');
        } else {
            await request('/api/customers',        { method: 'POST', body: JSON.stringify(body) });
            toast('Customer added successfully');
        }
        elements.customerDialog.close();
        await syncData();
    } catch (err) {
        elements.customerFormError.textContent = err.message;
    }
});
