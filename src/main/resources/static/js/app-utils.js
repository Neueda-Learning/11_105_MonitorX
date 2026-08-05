// ============================================================
// app-utils.js — HTTP client, auth helpers, sanitisation, toast
// ============================================================

async function request(url, options = {}) {
    const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
    const token = localStorage.getItem('monitorx_token');
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const response = await fetch(url, { headers, ...options });

    if (response.status === 401 && !url.includes('/api/auth/login')) {
        handleUnauthorized();
        throw new Error('Session expired. Please log in again.');
    }
    if (!response.ok) {
        let message = `Request failed (${response.status})`;
        try {
            const body = await response.json();
            message = body.message || body.detail || body.error || message;
        } catch (_) {}
        throw new Error(message);
    }
    return response.status === 204 ? null : response.json();
}

function handleUnauthorized() {
    localStorage.removeItem('monitorx_token');
    localStorage.removeItem('monitorx_user');
    elements.loginOverlay.style.display = 'flex';
}

function safeHTML(value) {
    const node = document.createElement('span');
    node.textContent = value ?? '';
    return node.innerHTML;
}

function toast(message, type = 'success') {
    elements.toast.textContent = message;
    elements.toast.style.borderLeftColor =
        type === 'error' ? 'var(--danger-color)' : 'var(--accent-color)';
    elements.toast.classList.add('show');
    window.setTimeout(() => elements.toast.classList.remove('show'), 3000);
}
