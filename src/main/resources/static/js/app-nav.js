// ============================================================
// app-nav.js — Hash routing, tab switching, drawer close
// ============================================================

function initNavigation() {
    window.addEventListener('hashchange', handleRoute);
    handleRoute();

    elements.navItems.forEach(item => {
        item.addEventListener('click', e => {
            e.preventDefault();
            window.location.hash = item.getAttribute('href');
        });
    });
}

function handleRoute() {
    let hash = window.location.hash.substring(1) || 'overview';
    if (!['overview', 'transactions', 'alerts', 'rules', 'customers'].includes(hash)) {
        hash = 'overview';
    }
    state.activeTab = hash;

    elements.navItems.forEach(item =>
        item.classList.toggle('active', item.getAttribute('href') === `#${hash}`)
    );
    elements.tabPanes.forEach(pane =>
        pane.classList.toggle('active', pane.id === `tab-${hash}`)
    );

    const titles = {
        overview:     { title: 'Overview Dashboard',      desc: 'Real-time indicators & transaction monitoring summary' },
        transactions: { title: 'Transaction Activity',    desc: 'Audit trail and filtering of system transactions' },
        alerts:       { title: 'Alerts Case Queue',       desc: 'Case files and status lifecycle operations' },
        rules:        { title: 'Rules Engine',            desc: 'Configure and toggle automated monitoring patterns' },
        customers:    { title: 'Customer Directory',      desc: 'Manage customer profiles, accounts, and registered countries' }
    };
    elements.pageTitle.textContent       = titles[hash].title;
    elements.pageDescription.textContent = titles[hash].desc;

    closeDrawer();
}

function closeDrawer() {
    elements.caseDrawer.classList.remove('open');
    state.selectedAlert = null;
}

elements.closeCaseDrawer.addEventListener('click', closeDrawer);
