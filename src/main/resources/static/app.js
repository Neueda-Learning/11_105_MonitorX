// ============================================================
// app.js — Bootstrap entry point
// All modules are loaded via <script> tags in index.html
// Load order: app-state → app-utils → app-nav → app-dashboard
//             → app-transactions → app-alerts → app-rules
//             → app-customers → app-auth → app-sync → app.js
// ============================================================

async function bootstrap() {
    initNavigation();

    const token = localStorage.getItem('monitorx_token');
    if (!token) {
        handleUnauthorized();
    } else {
        try {
            await syncData();
        } catch (_) {
            handleUnauthorized();
        }
    }

    window.addEventListener('resize', renderOverviewVisualizations);
}

bootstrap().catch(err => toast(err.message, 'error'));
