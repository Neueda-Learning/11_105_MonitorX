// ============================================================
// app-state.js — Global state, DOM element references, and
//                shared format helpers / constants
// ============================================================

const state = {
    transactions: [],
    alerts: [],
    rules: [],
    customers: [],
    activeTab: 'overview',
    selectedAlert: null,
    alertFilter: 'ALL'
};

const elements = {
    // Navigation & Tabs
    navItems:           document.querySelectorAll('.nav-item'),
    tabPanes:           document.querySelectorAll('.tab-pane'),
    pageTitle:          document.getElementById('pageTitle'),
    pageDescription:    document.getElementById('pageDescription'),
    sidebarAlertBadge:  document.getElementById('sidebarAlertBadge'),

    // Summary Metrics
    metricVolume:       document.getElementById('metricVolume'),
    metricTotal:        document.getElementById('metricTotal'),
    metricFlagged:      document.getElementById('metricFlagged'),
    metricActiveAlerts: document.getElementById('metricActiveAlerts'),

    // Chart & Dashboard Stats
    overviewChart:             document.getElementById('overviewChart'),
    alertDistributionDonut:    document.getElementById('alertDistributionDonut'),
    alertDistributionLegend:   document.getElementById('alertDistributionLegend'),
    quickAlertList:            document.getElementById('quickAlertList'),

    // Transactions
    transactionRows:    document.getElementById('transactionRows'),
    transactionEmpty:   document.getElementById('transactionEmpty'),
    searchDesc:         document.getElementById('searchDesc'),
    filterCustomer:     document.getElementById('filterCustomer'),
    filterAmountMin:    document.getElementById('filterAmountMin'),
    filterAmountMax:    document.getElementById('filterAmountMax'),
    filterStatus:       document.getElementById('filterStatus'),
    btnApplyFilters:    document.getElementById('btnApplyFilters'),
    btnResetFilters:    document.getElementById('btnResetFilters'),

    // Alerts Queue
    alertsGrid:     document.getElementById('alertsGrid'),
    alertEmpty:     document.getElementById('alertEmpty'),
    alertsTabBtns:  document.querySelectorAll('.alerts-tab-btn'),
    countOpen:      document.getElementById('countOpen'),
    countAck:       document.getElementById('countAck'),
    countInv:       document.getElementById('countInv'),

    // Rules
    ruleRows:           document.getElementById('ruleRows'),

    // Transaction Modal
    transactionDialog:      document.getElementById('transactionDialog'),
    transactionForm:        document.getElementById('transactionForm'),
    customerId:             document.getElementById('customerId'),
    country:                document.getElementById('country'),
    amount:                 document.getElementById('amount'),
    payeeId:                document.getElementById('payeeId'),
    timestamp:              document.getElementById('timestamp'),
    description:            document.getElementById('description'),
    formError:              document.getElementById('formError'),
    newTransactionButton:   document.getElementById('newTransactionButton'),
    closeDialog:            document.getElementById('closeDialog'),
    cancelDialog:           document.getElementById('cancelDialog'),

    // Rule Modal
    ruleDialog:         document.getElementById('ruleDialog'),
    ruleForm:           document.getElementById('ruleForm'),
    ruleId:             document.getElementById('ruleId'),
    ruleName:           document.getElementById('ruleName'),
    ruleType:           document.getElementById('ruleType'),
    ruleSeverity:       document.getElementById('ruleSeverity'),
    ruleActive:         document.getElementById('ruleActive'),
    ruleFormError:      document.getElementById('ruleFormError'),
    btnNewRule:         null,
    closeRuleDialog:    document.getElementById('closeRuleDialog'),
    cancelRuleDialog:   document.getElementById('cancelRuleDialog'),
    paramAmountThreshold: document.getElementById('param-AMOUNT_THRESHOLD'),
    paramVelocity:      document.getElementById('param-VELOCITY'),
    paramNewPayee:      document.getElementById('param-NEW_PAYEE'),
    paramDailyLimit:    document.getElementById('param-DAILY_LIMIT'),
    inputThreshold:     document.getElementById('paramThreshold'),
    inputVelocityMins:  document.getElementById('paramVelocityMins'),
    inputVelocityMaxCount: document.getElementById('paramVelocityMaxCount'),
    inputDailyLimit:    document.getElementById('paramDailyLimit'),

    // Alert Case Drawer
    caseDrawer:             document.getElementById('caseDrawer'),
    closeCaseDrawer:        document.getElementById('closeCaseDrawer'),
    caseTitle:              document.getElementById('caseTitle'),
    caseBadgeStatus:        document.getElementById('caseBadgeStatus'),
    caseBadgeSeverity:      document.getElementById('caseBadgeSeverity'),
    caseScore:              document.getElementById('caseScore'),
    caseCustomerName:       document.getElementById('caseCustomerName'),
    caseTime:               document.getElementById('caseTime'),
    caseReasons:            document.getElementById('caseReasons'),
    caseTxId:               document.getElementById('caseTxId'),
    caseTxAmount:           document.getElementById('caseTxAmount'),
    caseTxPayee:            document.getElementById('caseTxPayee'),
    caseTxCountry:          document.getElementById('caseTxCountry'),
    caseTxDescription:      document.getElementById('caseTxDescription'),
    caseHistoryTimeline:    document.getElementById('caseHistoryTimeline'),
    operatorNotes:          document.getElementById('operatorNotes'),
    caseActionButtons:      document.getElementById('caseActionButtons'),
    caseActionSection:      document.getElementById('caseActionSection'),

    // Toast & Seed
    toast:       document.getElementById('toast'),
    seedButton:  document.getElementById('seedButton'),

    // Authentication
    loginOverlay:   document.getElementById('loginOverlay'),
    loginForm:      document.getElementById('loginForm'),
    loginUsername:  document.getElementById('loginUsername'),
    loginPassword:  document.getElementById('loginPassword'),
    loginError:     document.getElementById('loginError'),
    btnLogout:      document.getElementById('btnLogout'),

    // Customers
    customerRows:           document.getElementById('customerRows'),
    customerEmpty:          document.getElementById('customerEmpty'),
    btnNewCustomer:         document.getElementById('btnNewCustomer'),
    customerDialog:         document.getElementById('customerDialog'),
    customerForm:           document.getElementById('customerForm'),
    customerFormId:         document.getElementById('customerFormId'),
    customerFormName:       document.getElementById('customerFormName'),
    customerFormAccount:    document.getElementById('customerFormAccount'),
    customerFormCountry:    document.getElementById('customerFormCountry'),
    customerFormError:      document.getElementById('customerFormError'),
    closeCustomerDialog:    document.getElementById('closeCustomerDialog'),
    cancelCustomerDialog:   document.getElementById('cancelCustomerDialog'),
    customerModalTitle:     document.getElementById('customerModalTitle')
};

// ── Format helpers ────────────────────────────────────────────────────────────
const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
const date  = new Intl.DateTimeFormat('en-US', { dateStyle: 'medium', timeStyle: 'short' });

// Country → ISO 4217 currency code
const countryCurrency = {
    'india': 'INR',
    'usa': 'USD', 'united states': 'USD',
    'uk': 'GBP', 'united kingdom': 'GBP', 'england': 'GBP',
    'germany': 'EUR', 'france': 'EUR', 'netherlands': 'EUR',
    'uae': 'AED', 'united arab emirates': 'AED',
    'japan': 'JPY',
    'singapore': 'SGD',
    'china': 'CNY',
    'brazil': 'BRL',
    'canada': 'CAD',
    'australia': 'AUD',
    'switzerland': 'CHF',
    'south africa': 'ZAR'
};

function formatMoney(amount, country) {
    const code = country ? (countryCurrency[country.toLowerCase()] || 'USD') : 'USD';
    try {
        return new Intl.NumberFormat('en-US', { style: 'currency', currency: code }).format(amount);
    } catch (_) {
        return money.format(amount);
    }
}
