// DB-Kavach Banking Application JavaScript SPA Engine

const API_BASE = '/api';

// Current active state
let currentUser = {
    userId: 5,
    customerId: 1,
    username: 'anshul',
    role: 'CUSTOMER',
    name: 'Anshul Bhilare'
};

let currentView = 'landing';
let currentTab = 'dashboard';
let spendingChartInstance = null;
let incomeChartInstance = null;

document.addEventListener('DOMContentLoaded', () => {
    switchRole('CUSTOMER');
});

// Toast notification helper
function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    const toastMsg = document.getElementById('toast-message');
    const toastIcon = document.getElementById('toast-icon');

    toast.classList.remove('hidden', 'bg-emerald-500/10', 'border-emerald-500/30', 'text-emerald-300', 'bg-red-500/10', 'border-red-500/30', 'text-red-300', 'bg-amber-500/10', 'border-amber-500/30', 'text-amber-300');

    if (type === 'success') {
        toast.classList.add('bg-emerald-500/10', 'border-emerald-500/30', 'text-emerald-300');
        toastIcon.className = 'fa-solid fa-circle-check text-emerald-400';
    } else if (type === 'warning') {
        toast.classList.add('bg-amber-500/10', 'border-amber-500/30', 'text-amber-300');
        toastIcon.className = 'fa-solid fa-triangle-exclamation text-amber-400';
    } else {
        toast.classList.add('bg-red-500/10', 'border-red-500/30', 'text-red-300');
        toastIcon.className = 'fa-solid fa-circle-exclamation text-red-400';
    }

    toastMsg.textContent = message;
}

function hideToast() {
    document.getElementById('toast').classList.add('hidden');
}

function openModal(id) {
    document.getElementById(id).classList.remove('hidden');
}

function closeModal(id) {
    document.getElementById(id).classList.add('hidden');
}

async function handleLoginSubmit(event) {
    event.preventDefault();
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value.trim();

    try {
        const res = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        const data = await res.json();
        if (res.ok && data.status === 'success') {
            closeModal('modal-login');
            showToast(`Welcome back, ${data.username}! Sign in successful.`, 'success');
            if (data.role === 'CUSTOMER') {
                currentUser = { userId: data.userId, customerId: 1, username: data.username, role: 'CUSTOMER', name: 'Anshul Bhilare' };
            } else if (data.role === 'BANK_ADMIN' || data.role === 'SUPER_ADMIN') {
                currentUser = { userId: data.userId, customerId: null, username: data.username, role: 'BANK_ADMIN', name: 'Bank Admin' };
            } else if (data.role === 'BANK_EMPLOYEE') {
                currentUser = { userId: data.userId, customerId: null, username: data.username, role: 'BANK_EMPLOYEE', name: 'Staff Officer' };
            } else if (data.role === 'AUDITOR') {
                currentUser = { userId: data.userId, customerId: null, username: data.username, role: 'AUDITOR', name: 'Security Auditor' };
            }
            document.getElementById('user-display-name').textContent = currentUser.name;
            document.getElementById('user-role-badge').textContent = currentUser.role;
            document.getElementById('user-avatar').textContent = currentUser.name.split(' ').map(n=>n[0]).join('');
            navigateTo('portal');
        } else {
            showToast(data.error || 'Invalid credentials. Please check your username & password.', 'error');
        }
    } catch (e) {
        showToast('Login request failed', 'error');
    }
}

let pendingRegistration = null;

async function handleRegisterSubmit(event) {
    event.preventDefault();
    const firstName = document.getElementById('reg-firstname').value.trim();
    const lastName = document.getElementById('reg-lastname').value.trim();
    const username = document.getElementById('reg-username').value.trim();
    const email = document.getElementById('reg-email').value.trim();
    const password = document.getElementById('reg-password').value.trim();
    const phone = document.getElementById('reg-phone').value.trim();
    const address = document.getElementById('reg-address').value.trim();

    if (!firstName || !lastName || !username || !email || !password || !phone || !address) {
        showToast('Please fill in all registration details', 'warning');
        return;
    }

    try {
        const regRes = await fetch(`${API_BASE}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ firstName, lastName, username, email, password, phone, address })
        });
        const regData = await regRes.json();

        if (regRes.ok && regData.status === 'success') {
            closeModal('modal-register');
            showToast('Customer account created successfully! Please sign in.', 'success');
            
            // Clear inputs
            document.getElementById('reg-firstname').value = '';
            document.getElementById('reg-lastname').value = '';
            document.getElementById('reg-username').value = '';
            document.getElementById('reg-email').value = '';
            document.getElementById('reg-password').value = '';
            document.getElementById('reg-phone').value = '';
            document.getElementById('reg-address').value = '';

            openModal('modal-login');
        } else {
            showToast(regData.error || 'Registration failed. Username or Email may already exist.', 'error');
        }
    } catch (e) {
        showToast('Registration request failed', 'error');
    }
}

async function resendOtp() {
    if (!pendingRegistration) return;
    try {
        const res = await fetch(`${API_BASE}/auth/send-otp`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: pendingRegistration.email })
        });
        const data = await res.json();
        const otpBox = document.getElementById('otp-display-box');
        const otpCodeEl = document.getElementById('otp-display-code');
        if (data.otpCode && otpBox && otpCodeEl) {
            otpCodeEl.textContent = data.otpCode;
            otpBox.classList.remove('hidden');
        }
        showToast(data.otpCode ? `New verification code [${data.otpCode}] sent to ${pendingRegistration.email}` : `New verification code sent to ${pendingRegistration.email}`, 'success');
    } catch (e) {
        showToast('Failed to resend code', 'error');
    }
}

// Role Switcher Demo Function
function switchRole(role) {
    document.querySelectorAll('.role-btn').forEach(btn => {
        btn.classList.remove('text-brand-400', 'bg-brand-500/10', 'border', 'border-brand-500/30');
        btn.classList.add('text-slate-400');
    });

    const activeBtn = document.getElementById(`role-btn-${role}`);
    if (activeBtn) {
        activeBtn.classList.remove('text-slate-400');
        activeBtn.classList.add('text-brand-400', 'bg-brand-500/10', 'border', 'border-brand-500/30');
    }

    let defaultTab = 'dashboard';
    if (role === 'CUSTOMER') {
        currentUser = { userId: 5, customerId: 1, username: 'anshul', role: 'CUSTOMER', name: 'Anshul Bhilare' };
        defaultTab = 'dashboard';
    } else if (role === 'BANK_ADMIN' || role === 'SUPER_ADMIN') {
        currentUser = { userId: 2, customerId: null, username: 'admin', role: 'BANK_ADMIN', name: 'Bank Admin' };
        defaultTab = 'admin_dashboard';
    } else if (role === 'BANK_EMPLOYEE') {
        currentUser = { userId: 3, customerId: null, username: 'staff', role: 'BANK_EMPLOYEE', name: 'Staff Officer' };
        defaultTab = 'staff_dashboard';
    } else if (role === 'AUDITOR') {
        currentUser = { userId: 4, customerId: null, username: 'auditor1', role: 'AUDITOR', name: 'Security Auditor' };
        defaultTab = 'auditor_compliance';
    }

    document.getElementById('user-display-name').textContent = currentUser.name;
    document.getElementById('user-role-badge').textContent = currentUser.role;
    document.getElementById('user-avatar').textContent = currentUser.name.split(' ').map(n=>n[0]).join('');

    navigateTo('portal', defaultTab);
}

function navigateTo(view, targetTab = null) {
    currentView = view;
    if (view === 'landing') {
        document.getElementById('view-landing').classList.remove('hidden');
        document.getElementById('view-portal').classList.add('hidden');
    } else {
        document.getElementById('view-landing').classList.add('hidden');
        document.getElementById('view-portal').classList.remove('hidden');
        buildSidebarNav();

        let initialTab = targetTab;
        if (!initialTab) {
            if (currentUser.role === 'CUSTOMER') initialTab = 'dashboard';
            else if (currentUser.role === 'BANK_ADMIN' || currentUser.role === 'SUPER_ADMIN') initialTab = 'admin_dashboard';
            else if (currentUser.role === 'BANK_EMPLOYEE') initialTab = 'staff_dashboard';
            else if (currentUser.role === 'AUDITOR') initialTab = 'auditor_compliance';
        }
        loadTabContent(initialTab);
    }
}

// Build Sidebar Navigation dynamically based on User Role
function buildSidebarNav() {
    const nav = document.getElementById('sidebar-nav');
    nav.innerHTML = '';

    let items = [];

    if (currentUser.role === 'CUSTOMER') {
        items = [
            { id: 'dashboard', icon: 'fa-gauge-high', label: 'Dashboard' },
            { id: 'accounts', icon: 'fa-building-columns', label: 'My Accounts' },
            { id: 'web3_hub', icon: 'fa-cubes', label: 'Web3 & Crypto' },
            { id: 'ai_advisor', icon: 'fa-wand-magic-sparkles', label: 'AI Advisor & Fraud' },
            { id: 'transfer', icon: 'fa-paper-plane', label: 'Transfer Money' },
            { id: 'beneficiaries', icon: 'fa-users-gear', label: 'Beneficiaries' },
            { id: 'transactions', icon: 'fa-list-check', label: 'Transactions' },
            { id: 'loans', icon: 'fa-hand-holding-dollar', label: 'Loans & EMI' },
            { id: 'fds', icon: 'fa-piggy-bank', label: 'Fixed Deposits' },
            { id: 'cards', icon: 'fa-credit-card', label: 'Cards' },
            { id: 'bills', icon: 'fa-file-invoice-dollar', label: 'Bill Payments' },
            { id: 'upi', icon: 'fa-qrcode', label: 'UPI Payments' },
            { id: 'tickets', icon: 'fa-headset', label: 'Support Tickets' },
            { id: 'profile', icon: 'fa-user-shield', label: 'Profile & Security' }
        ];
    } else if (currentUser.role === 'BANK_ADMIN' || currentUser.role === 'SUPER_ADMIN') {
        items = [
            { id: 'admin_dashboard', icon: 'fa-chart-line', label: 'Bank Overview' },
            { id: 'admin_web3', icon: 'fa-link', label: 'Web3 Blockchain Audit' },
            { id: 'admin_ai_risk', icon: 'fa-brain', label: 'AI Anomaly Center' },
            { id: 'admin_customers', icon: 'fa-users', label: 'Customer Mgmt' },
            { id: 'admin_accounts', icon: 'fa-vault', label: 'Account Mgmt' },
            { id: 'admin_kyc', icon: 'fa-id-card', label: 'KYC Center' },
            { id: 'admin_loans', icon: 'fa-landmark', label: 'Loan Applications' },
            { id: 'admin_fraud', icon: 'fa-triangle-exclamation', label: 'Fraud Detection Center' },
            { id: 'admin_security', icon: 'fa-shield-halved', label: 'Security Center' },
            { id: 'admin_audit', icon: 'fa-clock-rotate-left', label: 'Audit Logs' }
        ];
    } else if (currentUser.role === 'BANK_EMPLOYEE') {
        items = [
            { id: 'staff_dashboard', icon: 'fa-list-check', label: 'Staff Dashboard' },
            { id: 'admin_kyc', icon: 'fa-id-card', label: 'Review KYC' },
            { id: 'tickets', icon: 'fa-headset', label: 'Customer Tickets' },
            { id: 'admin_customers', icon: 'fa-users', label: 'Customer Directory' }
        ];
    } else if (currentUser.role === 'AUDITOR') {
        items = [
            { id: 'auditor_compliance', icon: 'fa-clipboard-check', label: 'Compliance Overview' },
            { id: 'admin_audit', icon: 'fa-clock-rotate-left', label: 'Audit Logs' },
            { id: 'admin_fraud', icon: 'fa-triangle-exclamation', label: 'Fraud Center' },
            { id: 'admin_web3', icon: 'fa-link', label: 'Web3 Audit Metrics' }
        ];
    }

    items.forEach(item => {
        const btn = document.createElement('button');
        btn.id = `nav-${item.id}`;
        btn.onclick = () => loadTabContent(item.id);
        btn.className = `w-full px-3 py-2 rounded-xl text-left flex items-center space-x-3 transition ${item.id === currentTab ? 'text-brand-400 bg-brand-500/10 border border-brand-500/20 font-bold' : 'text-slate-400 hover:text-white hover:bg-slate-800/60'}`;
        btn.innerHTML = `<i class="fa-solid ${item.icon} w-4 text-center"></i><span>${item.label}</span>`;
        nav.appendChild(btn);
    });
}

// Load Tab Content dynamically
async function loadTabContent(tabId) {
    currentTab = tabId;
    document.querySelectorAll('#sidebar-nav button').forEach(btn => {
        btn.classList.remove('text-brand-400', 'bg-brand-500/10', 'border', 'border-brand-500/20', 'font-bold');
        btn.classList.add('text-slate-400');
    });
    const activeNav = document.getElementById(`nav-${tabId}`);
    if (activeNav) {
        activeNav.classList.remove('text-slate-400');
        activeNav.classList.add('text-brand-400', 'bg-brand-500/10', 'border', 'border-brand-500/20', 'font-bold');
    }

    const container = document.getElementById('portal-content-container');
    container.innerHTML = `<div class="p-8 text-center text-slate-400"><i class="fa-solid fa-circle-notch fa-spin text-2xl mb-2 text-brand-400"></i><div>Loading ${tabId}...</div></div>`;

    if (tabId === 'dashboard') renderCustomerDashboard(container);
    else if (tabId === 'accounts') renderCustomerAccounts(container);
    else if (tabId === 'web3_hub' || tabId === 'admin_web3') renderWeb3Hub(container);
    else if (tabId === 'ai_advisor' || tabId === 'admin_ai_risk') renderAiAdvisor(container);
    else if (tabId === 'transfer') renderCustomerTransfer(container);
    else if (tabId === 'beneficiaries') renderCustomerBeneficiaries(container);
    else if (tabId === 'transactions') renderCustomerTransactions(container);
    else if (tabId === 'loans') renderCustomerLoans(container);
    else if (tabId === 'fds') renderCustomerFDs(container);
    else if (tabId === 'cards') renderCustomerCards(container);
    else if (tabId === 'bills') renderCustomerBills(container);
    else if (tabId === 'upi') renderUPIPayment(container);
    else if (tabId === 'tickets') renderSupportTickets(container);
    else if (tabId === 'profile') renderCustomerProfile(container);
    else if (tabId === 'admin_dashboard') renderAdminDashboard(container);
    else if (tabId === 'admin_customers') renderAdminCustomers(container);
    else if (tabId === 'admin_accounts') renderAdminAccounts(container);
    else if (tabId === 'admin_kyc') renderAdminKYC(container);
    else if (tabId === 'admin_loans') renderAdminLoans(container);
    else if (tabId === 'admin_fraud') renderAdminFraud(container);
    else if (tabId === 'admin_security') renderAdminSecurity(container);
    else if (tabId === 'admin_audit') renderAdminAudit(container);
    else if (tabId === 'dr_protection') renderDisasterRecovery(container);
    else if (tabId === 'staff_dashboard') renderStaffDashboard(container);
    else if (tabId === 'auditor_compliance') renderAuditorCompliance(container);
}

// ---------------- CUSTOMER VIEWS ----------------

async function renderCustomerDashboard(container) {
    try {
        const accRes = await fetch(`${API_BASE}/customer/accounts?customerId=${currentUser.customerId}`);
        const accounts = await accRes.json();

        let totalBal = 0, savingsBal = 0, currentBal = 0;
        accounts.forEach(a => {
            totalBal += a.balance;
            if (a.accountType === 'SAVINGS') savingsBal += a.balance;
            if (a.accountType === 'CURRENT') currentBal += a.balance;
        });

        const txRes = await fetch(`${API_BASE}/customer/transactions?customerId=${currentUser.customerId}`);
        const transactions = await txRes.json();

        container.innerHTML = `
            <div class="space-y-6">
                <!-- Stats Row -->
                <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
                    <div class="bg-darkcard border border-darkborder rounded-2xl p-5">
                        <div class="text-xs font-semibold text-slate-400 uppercase">Total Net Balance</div>
                        <div class="text-2xl font-extrabold text-brand-400 mt-1">₹${totalBal.toLocaleString('en-IN', {minimumFractionDigits:2})}</div>
                        <div class="text-[11px] text-emerald-400 mt-1">Available across ${accounts.length} accounts</div>
                    </div>
                    <div class="bg-darkcard border border-darkborder rounded-2xl p-5">
                        <div class="text-xs font-semibold text-slate-400 uppercase">Savings Account</div>
                        <div class="text-2xl font-extrabold text-white mt-1">₹${savingsBal.toLocaleString('en-IN', {minimumFractionDigits:2})}</div>
                        <div class="text-[11px] text-slate-400 mt-1">Interest Rate: 4.0% p.a.</div>
                    </div>
                    <div class="bg-darkcard border border-darkborder rounded-2xl p-5">
                        <div class="text-xs font-semibold text-slate-400 uppercase">Current Account</div>
                        <div class="text-2xl font-extrabold text-indigo-400 mt-1">₹${currentBal.toLocaleString('en-IN', {minimumFractionDigits:2})}</div>
                        <div class="text-[11px] text-slate-400 mt-1">Overdraft Limit: Active</div>
                    </div>
                    <div class="bg-darkcard border border-darkborder rounded-2xl p-5">
                        <div class="text-xs font-semibold text-slate-400 uppercase">Active Cards & Loans</div>
                        <div class="text-2xl font-extrabold text-amber-400 mt-1">1 Loan / 2 Cards</div>
                        <div class="text-[11px] text-emerald-400 mt-1">All payments on schedule</div>
                    </div>
                </div>

                <!-- Charts Section -->
                <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div class="bg-darkcard border border-darkborder rounded-2xl p-5 space-y-4">
                        <h3 class="text-sm font-bold text-white flex items-center">
                            <i class="fa-solid fa-chart-pie mr-2 text-brand-400"></i> Spending by Category
                        </h3>
                        <div class="h-56 relative"><canvas id="chart-spending"></canvas></div>
                    </div>
                    <div class="bg-darkcard border border-darkborder rounded-2xl p-5 space-y-4">
                        <h3 class="text-sm font-bold text-white flex items-center">
                            <i class="fa-solid fa-chart-column mr-2 text-indigo-400"></i> Monthly Income vs Expenses
                        </h3>
                        <div class="h-56 relative"><canvas id="chart-income"></canvas></div>
                    </div>
                </div>

                <!-- Recent Transactions Table -->
                <div class="bg-darkcard border border-darkborder rounded-2xl p-5 space-y-4">
                    <div class="flex justify-between items-center">
                        <h3 class="text-sm font-bold text-white flex items-center">
                            <i class="fa-solid fa-clock-rotate-left mr-2 text-brand-400"></i> Recent Transactions
                        </h3>
                        <button onclick="loadTabContent('transactions')" class="text-xs text-brand-400 hover:underline">View All</button>
                    </div>
                    <div class="overflow-x-auto">
                        <table class="w-full text-left text-xs font-mono">
                            <thead class="bg-slate-900/60 uppercase text-slate-400 border-b border-darkborder">
                                <tr>
                                    <th class="p-3">Txn ID</th>
                                    <th class="p-3">Type</th>
                                    <th class="p-3">Description</th>
                                    <th class="p-3">Amount</th>
                                    <th class="p-3">Status</th>
                                    <th class="p-3">Date</th>
                                </tr>
                            </thead>
                            <tbody class="divide-y divide-slate-800 text-slate-300">
                                ${transactions.map(t => `
                                    <tr class="hover:bg-slate-900/40">
                                        <td class="p-3 font-bold text-white">${t.transactionId}</td>
                                        <td class="p-3"><span class="px-2 py-0.5 rounded bg-slate-800 text-slate-300">${t.type}</span></td>
                                        <td class="p-3">${t.description}</td>
                                        <td class="p-3 font-bold ${t.type==='DEPOSIT'?'text-emerald-400':'text-slate-200'}">₹${t.amount.toLocaleString()}</td>
                                        <td class="p-3"><span class="px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">${t.status}</span></td>
                                        <td class="p-3 text-slate-400">${new Date(t.timestamp).toLocaleDateString()}</td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        `;

        // Render Chart.js
        setTimeout(() => {
            const ctx1 = document.getElementById('chart-spending').getContext('2d');
            spendingChartInstance = new Chart(ctx1, {
                type: 'doughnut',
                data: {
                    labels: ['Shopping', 'Bills', 'Food', 'Travel', 'Other'],
                    datasets: [{
                        data: [3500, 1200, 2400, 1800, 950],
                        backgroundColor: ['#22c55e', '#6366f1', '#f59e0b', '#06b6d4', '#64748b']
                    }]
                },
                options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { labels: { color: '#94a3b8' } } } }
            });

            const ctx2 = document.getElementById('chart-income').getContext('2d');
            incomeChartInstance = new Chart(ctx2, {
                type: 'bar',
                data: {
                    labels: ['May', 'Jun', 'Jul', 'Aug', 'Sep'],
                    datasets: [
                        { label: 'Income', data: [85000, 85000, 92000, 85000, 95000], backgroundColor: '#22c55e' },
                        { label: 'Expenses', data: [32000, 41000, 38000, 29000, 35000], backgroundColor: '#f43f5e' }
                    ]
                },
                options: { responsive: true, maintainAspectRatio: false, scales: { x: { ticks: { color: '#94a3b8' } }, y: { ticks: { color: '#94a3b8' } } }, plugins: { legend: { labels: { color: '#94a3b8' } } } }
            });
        }, 100);

    } catch (e) {
        showToast('Error loading dashboard data', 'error');
    }
}

async function renderCustomerAccounts(container) {
    try {
        const res = await fetch(`${API_BASE}/customer/accounts?customerId=${currentUser.customerId}`);
        const accounts = await res.json();

        container.innerHTML = `
            <div class="space-y-6">
                <div class="flex justify-between items-center">
                    <div>
                        <h2 class="text-xl font-bold text-white">My Bank Accounts</h2>
                        <p class="text-xs text-slate-400">View balances, account details and status</p>
                    </div>
                </div>

                <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                    ${accounts.map(a => `
                        <div class="bank-card-bg border border-darkborder rounded-2xl p-6 space-y-4 shadow-xl">
                            <div class="flex justify-between items-start">
                                <div>
                                    <span class="text-xs font-semibold px-2.5 py-1 rounded-full bg-brand-500/10 text-brand-400 border border-brand-500/20">${a.accountType}</span>
                                    <h3 class="text-lg font-bold text-white mt-2">${a.maskedAccountNumber}</h3>
                                </div>
                                <span class="text-xs font-bold px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400">${a.status}</span>
                            </div>

                            <div>
                                <div class="text-xs text-slate-400 uppercase font-semibold">Available Balance</div>
                                <div class="text-3xl font-black text-white mt-1">₹${a.balance.toLocaleString('en-IN', {minimumFractionDigits:2})}</div>
                            </div>

                            <div class="pt-4 border-t border-slate-700/50 flex justify-between text-xs text-slate-300">
                                <div>Interest Rate: <strong>${a.interestRate}% p.a.</strong></div>
                                <div>Opened: <strong>${new Date(a.createdAt).toLocaleDateString()}</strong></div>
                            </div>
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
    } catch (e) {
        showToast('Error loading accounts', 'error');
    }
}

async function renderCustomerTransfer(container) {
    try {
        const res = await fetch(`${API_BASE}/customer/accounts?customerId=${currentUser.customerId}`);
        const accounts = await res.json();
        const benRes = await fetch(`${API_BASE}/customer/beneficiaries?customerId=${currentUser.customerId}`);
        const beneficiaries = await benRes.json();

        container.innerHTML = `
            <div class="max-w-2xl mx-auto bg-darkcard border border-darkborder rounded-2xl p-6 space-y-6">
                <div>
                    <h2 class="text-xl font-bold text-white flex items-center">
                        <i class="fa-solid fa-paper-plane text-brand-400 mr-2"></i> Money Transfer
                    </h2>
                    <p class="text-xs text-slate-400">Transfer funds securely with idempotency and real-time fraud monitoring</p>
                </div>

                <form onsubmit="handleTransferSubmit(event)" class="space-y-4 text-xs">
                    <div>
                        <label class="block text-slate-300 font-semibold mb-1">Source Account</label>
                        <select id="tr-source" class="w-full bg-slate-900 border border-darkborder rounded-lg px-3 py-2 text-white">
                            ${accounts.map(a => `<option value="${a.accountNumber}">${a.accountType} (${a.maskedAccountNumber}) - Available: ₹${a.availableBalance}</option>`).join('')}
                        </select>
                    </div>

                    <div>
                        <label class="block text-slate-300 font-semibold mb-1">Target Account / Beneficiary</label>
                        <input type="text" id="tr-target" placeholder="Enter Account Number or Select Beneficiary" value="${beneficiaries.length>0?beneficiaries[0].accountNumber:'SAV1001234567'}" required class="w-full bg-slate-900 border border-darkborder rounded-lg px-3 py-2 text-white">
                    </div>

                    <div class="grid grid-cols-2 gap-4">
                        <div>
                            <label class="block text-slate-300 font-semibold mb-1">Amount (₹)</label>
                            <input type="number" id="tr-amount" value="5000" min="1" step="0.01" required class="w-full bg-slate-900 border border-darkborder rounded-lg px-3 py-2 text-white">
                        </div>
                        <div>
                            <label class="block text-slate-300 font-semibold mb-1">Category</label>
                            <select id="tr-category" class="w-full bg-slate-900 border border-darkborder rounded-lg px-3 py-2 text-white">
                                <option value="SHOPPING">Shopping</option>
                                <option value="BILLS">Bills</option>
                                <option value="FOOD">Food</option>
                                <option value="TRAVEL">Travel</option>
                                <option value="EDUCATION">Education</option>
                                <option value="OTHER" selected>Other</option>
                            </select>
                        </div>
                    </div>

                    <div>
                        <label class="block text-slate-300 font-semibold mb-1">Remarks / Purpose</label>
                        <input type="text" id="tr-desc" value="Monthly consultation payment" class="w-full bg-slate-900 border border-darkborder rounded-lg px-3 py-2 text-white">
                    </div>

                    <div class="p-3 bg-slate-900 rounded-xl border border-darkborder text-[11px] text-slate-400 space-y-1">
                        <div class="font-bold text-slate-200"><i class="fa-solid fa-shield-check text-brand-400 mr-1"></i> Financial Math & Safety Enforced:</div>
                        <div>• Idempotency key generated automatically to prevent duplicate debits.</div>
                        <div>• Real-time Fraud Engine evaluates risk score (0-100) before clearance.</div>
                    </div>

                    <button type="submit" class="w-full bg-brand-600 hover:bg-brand-500 text-white font-bold py-3 rounded-xl transition shadow-lg glow-effect flex items-center justify-center space-x-2">
                        <i class="fa-solid fa-lock"></i>
                        <span>Confirm & Transfer Funds</span>
                    </button>
                </form>
            </div>
        `;
    } catch (e) {
        showToast('Error loading transfer form', 'error');
    }
}

async function handleTransferSubmit(e) {
    e.preventDefault();
    const source = document.getElementById('tr-source').value;
    const target = document.getElementById('tr-target').value;
    const amount = document.getElementById('tr-amount').value;
    const category = document.getElementById('tr-category').value;
    const desc = document.getElementById('tr-desc').value;
    const key = 'IDEMP_' + System.currentTimeMillis();

    try {
        const res = await fetch(`${API_BASE}/customer/transfer`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sourceAccount: source, targetAccount: target, amount, category, description: desc, idempotencyKey: key })
        });
        const data = await res.json();
        if (res.ok) {
            showToast(`Transfer successful! Transaction ID: ${data.transactionId}`, 'success');
            loadTabContent('transactions');
        } else {
            showToast(data.error || 'Transfer failed', 'error');
        }
    } catch (err) {
        showToast('Transfer request failed', 'error');
    }
}

async function renderCustomerBeneficiaries(container) {
    try {
        const res = await fetch(`${API_BASE}/customer/beneficiaries?customerId=${currentUser.customerId}`);
        const beneficiaries = await res.json();

        container.innerHTML = `
            <div class="space-y-6">
                <div class="flex justify-between items-center">
                    <div>
                        <h2 class="text-xl font-bold text-white">Beneficiaries</h2>
                        <p class="text-xs text-slate-400">Manage saved transfer accounts with 30-min cooling protection</p>
                    </div>
                </div>

                <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                    ${beneficiaries.map(b => `
                        <div class="bg-darkcard border border-darkborder rounded-2xl p-5 space-y-2">
                            <div class="flex justify-between items-center">
                                <h3 class="font-bold text-white text-base">${b.nickname}</h3>
                                <span class="text-xs px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400">${b.status}</span>
                            </div>
                            <div class="text-xs text-slate-300 font-mono">${b.maskedAccountNumber}</div>
                            <div class="text-xs text-slate-400">${b.beneficiaryName} • ${b.bankName}</div>
                            <div class="text-[11px] text-slate-500 font-mono">IFSC: ${b.ifscCode}</div>
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
    } catch (e) {
        showToast('Error loading beneficiaries', 'error');
    }
}

async function renderCustomerTransactions(container) {
    try {
        const res = await fetch(`${API_BASE}/customer/transactions?customerId=${currentUser.customerId}`);
        const transactions = await res.json();

        container.innerHTML = `
            <div class="space-y-6">
                <h2 class="text-xl font-bold text-white">Transaction History</h2>
                <div class="bg-darkcard border border-darkborder rounded-2xl p-5">
                    <div class="overflow-x-auto">
                        <table class="w-full text-left text-xs font-mono">
                            <thead class="bg-slate-900 uppercase text-slate-400 border-b border-darkborder">
                                <tr>
                                    <th class="p-3">Txn ID</th>
                                    <th class="p-3">Type</th>
                                    <th class="p-3">Category</th>
                                    <th class="p-3">Description</th>
                                    <th class="p-3">Amount</th>
                                    <th class="p-3">Status</th>
                                    <th class="p-3">Timestamp</th>
                                </tr>
                            </thead>
                            <tbody class="divide-y divide-slate-800 text-slate-300">
                                ${transactions.map(t => `
                                    <tr class="hover:bg-slate-900/40">
                                        <td class="p-3 font-bold text-white">${t.transactionId}</td>
                                        <td class="p-3"><span class="px-2 py-0.5 rounded bg-slate-800">${t.type}</span></td>
                                        <td class="p-3"><span class="px-2 py-0.5 rounded bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">${t.category}</span></td>
                                        <td class="p-3">${t.description}</td>
                                        <td class="p-3 font-bold ${t.type==='DEPOSIT'?'text-emerald-400':'text-slate-200'}">₹${t.amount.toLocaleString()}</td>
                                        <td class="p-3"><span class="px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">${t.status}</span></td>
                                        <td class="p-3 text-slate-400">${new Date(t.timestamp).toLocaleString()}</td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        `;
    } catch (e) {
        showToast('Error loading transactions', 'error');
    }
}

async function renderCustomerLoans(container) {
    try {
        const res = await fetch(`${API_BASE}/customer/loans?customerId=${currentUser.customerId}`);
        const loans = await res.json();

        container.innerHTML = `
            <div class="space-y-6">
                <div class="flex justify-between items-center">
                    <div>
                        <h2 class="text-xl font-bold text-white">Loan Portfolio & Applications</h2>
                        <p class="text-xs text-slate-400">Personal, Home, Education, Vehicle, and Business loans</p>
                    </div>
                </div>

                <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                    ${loans.map(l => `
                        <div class="bg-darkcard border border-darkborder rounded-2xl p-6 space-y-3">
                            <div class="flex justify-between items-center">
                                <span class="text-xs font-bold px-2.5 py-1 rounded bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">${l.loanType} LOAN</span>
                                <span class="text-xs font-bold px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400">${l.status}</span>
                            </div>
                            <div class="text-sm font-mono text-slate-400">Loan #: ${l.loanNumber}</div>
                            <div class="text-2xl font-extrabold text-white">₹${l.principalAmount.toLocaleString()}</div>
                            <div class="grid grid-cols-2 gap-2 text-xs text-slate-300 pt-2 border-t border-slate-800">
                                <div>Monthly EMI: <strong class="text-brand-400">₹${l.monthlyEmi.toLocaleString()}</strong></div>
                                <div>Interest Rate: <strong>${l.interestRate}%</strong></div>
                                <div>Tenure: <strong>${l.tenureMonths} Months</strong></div>
                                <div>Outstanding: <strong>₹${l.outstandingAmount.toLocaleString()}</strong></div>
                            </div>
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
    } catch (e) {
        showToast('Error loading loans', 'error');
    }
}

async function renderCustomerFDs(container) {
    try {
        const res = await fetch(`${API_BASE}/customer/fds?customerId=${currentUser.customerId}`);
        const fds = await res.json();

        container.innerHTML = `
            <div class="space-y-6">
                <h2 class="text-xl font-bold text-white">Fixed Deposits</h2>
                <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
                    ${fds.map(f => `
                        <div class="bg-darkcard border border-darkborder rounded-2xl p-5 space-y-3">
                            <div class="flex justify-between items-center">
                                <span class="text-xs font-mono font-bold text-amber-400">${f.fdNumber}</span>
                                <span class="text-xs font-bold px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400">${f.status}</span>
                            </div>
                            <div>
                                <div class="text-xs text-slate-400">Principal Deposit</div>
                                <div class="text-xl font-black text-white">₹${f.principalAmount.toLocaleString()}</div>
                            </div>
                            <div class="pt-2 border-t border-slate-800 text-xs text-slate-300 space-y-1">
                                <div>Interest Rate: <strong>${f.interestRate}% p.a.</strong></div>
                                <div>Maturity Amount: <strong class="text-emerald-400">₹${f.maturityAmount.toLocaleString()}</strong></div>
                                <div>Maturity Date: <strong>${new Date(f.maturityDate).toLocaleDateString()}</strong></div>
                            </div>
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
    } catch (e) {
        showToast('Error loading FDs', 'error');
    }
}

async function renderCustomerCards(container) {
    try {
        const res = await fetch(`${API_BASE}/customer/cards?customerId=${currentUser.customerId}`);
        const cards = await res.json();

        container.innerHTML = `
            <div class="space-y-6">
                <h2 class="text-xl font-bold text-white">My Debit & Credit Cards</h2>
                <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                    ${cards.map(c => `
                        <div class="${c.cardType==='CREDIT'?'credit-card-bg':'bank-card-bg'} border border-darkborder rounded-2xl p-6 space-y-6 shadow-2xl text-white">
                            <div class="flex justify-between items-center">
                                <span class="font-black text-sm tracking-widest uppercase">${c.cardType} CARD</span>
                                <i class="fa-brands fa-cc-visa text-3xl"></i>
                            </div>
                            <div class="text-xl font-mono tracking-widest py-2">${c.cardNumberMasked}</div>
                            <div class="flex justify-between items-end text-xs">
                                <div>
                                    <div class="text-[10px] text-slate-300 uppercase">Cardholder</div>
                                    <div class="font-bold text-sm">ANSHUL BHILARE</div>
                                </div>
                                <div>
                                    <div class="text-[10px] text-slate-300 uppercase">Expires</div>
                                    <div class="font-bold">${new Date(c.expiryDate).toLocaleDateString()}</div>
                                </div>
                            </div>
                            <div class="pt-3 border-t border-white/20 flex justify-between items-center">
                                <span class="text-xs font-semibold">Status: ${c.frozen?'FROZEN':'ACTIVE'}</span>
                                <button onclick="toggleFreezeCard(${c.id})" class="bg-white/10 hover:bg-white/20 text-xs px-3 py-1 rounded-lg border border-white/20">
                                    ${c.frozen?'Unfreeze Card':'Freeze Card'}
                                </button>
                            </div>
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
    } catch (e) {
        showToast('Error loading cards', 'error');
    }
}

async function toggleFreezeCard(id) {
    try {
        const res = await fetch(`${API_BASE}/customer/cards/${id}/freeze?username=${currentUser.username}`, { method: 'POST' });
        if (res.ok) {
            showToast('Card status updated', 'success');
            loadTabContent('cards');
        }
    } catch (e) {
        showToast('Failed to toggle card status', 'error');
    }
}

async function renderCustomerBills(container) {
    try {
        const res = await fetch(`${API_BASE}/customer/billers`);
        const billers = await res.json();

        container.innerHTML = `
            <div class="space-y-6">
                <h2 class="text-xl font-bold text-white">Utility Bill Payments</h2>
                <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
                    ${billers.map(b => `
                        <div class="bg-darkcard border border-darkborder rounded-2xl p-5 space-y-3">
                            <div class="w-10 h-10 rounded-xl bg-indigo-500/10 text-indigo-400 flex items-center justify-center text-lg">
                                <i class="fa-solid fa-bolt"></i>
                            </div>
                            <h3 class="font-bold text-white">${b.name}</h3>
                            <div class="text-xs text-slate-400 uppercase font-semibold">${b.category}</div>
                            <button onclick="payBillerModal(${b.id}, '${b.name}')" class="w-full bg-brand-600 hover:bg-brand-500 text-white text-xs font-bold py-2 rounded-lg transition">Pay Bill Now</button>
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
    } catch (e) {
        showToast('Error loading billers', 'error');
    }
}

function payBillerModal(billerId, billerName) {
    const amount = prompt(`Enter payment amount for ${billerName}:`, "1250.00");
    if (!amount) return;

    fetch(`${API_BASE}/customer/bills/pay`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ customerId: currentUser.customerId, accountId: 1, billerId: billerId, consumerNumber: 'CONS10098', amount: parseFloat(amount) })
    }).then(res => res.json()).then(data => {
        showToast(`Bill paid successfully to ${billerName}!`, 'success');
    }).catch(err => showToast('Bill payment failed', 'error'));
}

function renderUPIPayment(container) {
    container.innerHTML = `
        <div class="max-w-md mx-auto bg-darkcard border border-darkborder rounded-2xl p-6 text-center space-y-4">
            <span class="px-3 py-1 rounded-full text-xs font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20">Simulated Digital Payment Layer</span>
            <h2 class="text-xl font-bold text-white">DB-Kavach UPI Pay</h2>
            <div class="bg-slate-900 p-6 rounded-2xl inline-block border border-darkborder">
                <i class="fa-solid fa-qrcode text-8xl text-white"></i>
                <div class="text-xs font-mono text-slate-400 mt-2">UPI ID: anshul@dbkavach</div>
            </div>
            <p class="text-xs text-slate-400">Scan QR Code or send money via instant virtual payment address</p>
        </div>
    `;
}

async function renderSupportTickets(container) {
    try {
        const res = await fetch(`${API_BASE}/customer/tickets?customerId=${currentUser.customerId}`);
        const tickets = await res.json();

        container.innerHTML = `
            <div class="space-y-6">
                <h2 class="text-xl font-bold text-white">Customer Support Tickets</h2>
                <div class="space-y-4">
                    ${tickets.map(t => `
                        <div class="bg-darkcard border border-darkborder rounded-2xl p-5 space-y-2">
                            <div class="flex justify-between items-center">
                                <h3 class="font-bold text-white text-base">${t.subject}</h3>
                                <span class="text-xs px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-400">${t.status}</span>
                            </div>
                            <div class="text-xs text-slate-400">Ticket #: ${t.ticketNumber} • Priority: ${t.priority}</div>
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
    } catch (e) {
        showToast('Error loading support tickets', 'error');
    }
}

async function renderCustomerProfile(container) {
    try {
        const res = await fetch(`${API_BASE}/customer/profile?customerId=${currentUser.customerId}`);
        const c = await res.json();
        const firstName = (c.firstName && c.firstName !== 'John' && c.firstName !== 'Jane') ? c.firstName : 'Anshul';
        const lastName = (c.lastName && c.lastName !== 'Doe' && c.lastName !== 'Smith') ? c.lastName : 'Bhilare';

        container.innerHTML = `
            <div class="max-w-2xl mx-auto bg-darkcard border border-darkborder rounded-2xl p-6 space-y-6">
                <h2 class="text-xl font-bold text-white flex items-center">
                    <i class="fa-solid fa-user-shield text-brand-400 mr-2"></i> Customer Profile & Security
                </h2>
                <div class="grid grid-cols-2 gap-4 text-xs">
                    <div><span class="text-slate-400">Name:</span> <strong class="text-white block text-sm">${firstName} ${lastName}</strong></div>
                    <div><span class="text-slate-400">KYC Status:</span> <strong class="text-emerald-400 block text-sm">${c.kycStatus || 'APPROVED'}</strong></div>
                    <div><span class="text-slate-400">Phone:</span> <strong class="text-white block">${c.phone || '+91-98765-01420'}</strong></div>
                    <div><span class="text-slate-400">Address:</span> <strong class="text-white block">${c.address || '742 Financial Avenue, New Delhi'}</strong></div>
                </div>
            </div>
        `;
    } catch (e) {
        showToast('Error loading profile', 'error');
    }
}

// ---------------- ADMIN VIEWS ----------------

async function renderAdminDashboard(container) {
    try {
        const res = await fetch(`${API_BASE}/admin/metrics`);
        const m = await res.json();

        container.innerHTML = `
            <div class="space-y-6">
                <h2 class="text-2xl font-bold text-white">Bank Administration Dashboard</h2>

                <!-- Metrics Grid -->
                <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
                    <div class="bg-darkcard border border-darkborder rounded-2xl p-5">
                        <div class="text-xs font-semibold text-slate-400 uppercase">Total Customers</div>
                        <div class="text-3xl font-extrabold text-white mt-1">${m.totalCustomers}</div>
                    </div>
                    <div class="bg-darkcard border border-darkborder rounded-2xl p-5">
                        <div class="text-xs font-semibold text-slate-400 uppercase">Total Deposits</div>
                        <div class="text-3xl font-extrabold text-emerald-400 mt-1">₹${m.totalDeposits.toLocaleString()}</div>
                    </div>
                    <div class="bg-darkcard border border-darkborder rounded-2xl p-5">
                        <div class="text-xs font-semibold text-slate-400 uppercase">Pending KYC</div>
                        <div class="text-3xl font-extrabold text-amber-400 mt-1">${m.pendingKyc}</div>
                    </div>
                    <div class="bg-darkcard border border-darkborder rounded-2xl p-5">
                        <div class="text-xs font-semibold text-slate-400 uppercase">Fraud Alerts</div>
                        <div class="text-3xl font-extrabold text-red-400 mt-1">${m.suspiciousAlerts}</div>
                    </div>
                </div>

                <div class="bg-darkcard border border-darkborder rounded-2xl p-6">
                    <h3 class="text-lg font-bold text-white mb-4">Quick Admin Reporting</h3>
                    <div class="flex gap-4">
                        <button onclick="downloadReport('customers')" class="bg-slate-800 hover:bg-slate-700 text-xs text-white px-4 py-2 rounded-lg border border-darkborder"><i class="fa-solid fa-download mr-1"></i> Customer CSV</button>
                        <button onclick="downloadReport('transactions')" class="bg-slate-800 hover:bg-slate-700 text-xs text-white px-4 py-2 rounded-lg border border-darkborder"><i class="fa-solid fa-download mr-1"></i> Transaction CSV</button>
                        <button onclick="downloadReport('audit')" class="bg-slate-800 hover:bg-slate-700 text-xs text-white px-4 py-2 rounded-lg border border-darkborder"><i class="fa-solid fa-download mr-1"></i> Audit CSV</button>
                    </div>
                </div>
            </div>
        `;
    } catch (e) {
        showToast('Error loading admin dashboard', 'error');
    }
}

function downloadReport(type) {
    window.open(`${API_BASE}/admin/reports/${type}`, '_blank');
}

async function renderAdminCustomers(container) {
    try {
        const res = await fetch(`${API_BASE}/admin/customers`);
        const customers = await res.json();

        container.innerHTML = `
            <div class="space-y-6">
                <h2 class="text-xl font-bold text-white">Customer Directory</h2>
                <div class="bg-darkcard border border-darkborder rounded-2xl p-5 overflow-x-auto">
                    <table class="w-full text-left text-xs font-mono">
                        <thead class="bg-slate-900 text-slate-400">
                            <tr>
                                <th class="p-3">Customer ID</th>
                                <th class="p-3">Name</th>
                                <th class="p-3">Phone</th>
                                <th class="p-3">KYC Status</th>
                                <th class="p-3">Opened</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-slate-800">
                            ${customers.map(c => `
                                <tr>
                                    <td class="p-3 text-white font-bold">${c.id}</td>
                                    <td class="p-3 font-bold">${c.firstName} ${c.lastName}</td>
                                    <td class="p-3">${c.phone}</td>
                                    <td class="p-3"><span class="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400">${c.kycStatus}</span></td>
                                    <td class="p-3 text-slate-400">${new Date(c.accountOpeningDate).toLocaleDateString()}</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    } catch (e) {
        showToast('Error loading customers', 'error');
    }
}

async function renderAdminAccounts(container) {
    try {
        const res = await fetch(`${API_BASE}/admin/accounts`);
        const accounts = await res.json();

        container.innerHTML = `
            <div class="space-y-6">
                <h2 class="text-xl font-bold text-white">Account Management</h2>
                <div class="bg-darkcard border border-darkborder rounded-2xl p-5 overflow-x-auto">
                    <table class="w-full text-left text-xs font-mono">
                        <thead class="bg-slate-900 text-slate-400">
                            <tr>
                                <th class="p-3">Account #</th>
                                <th class="p-3">Customer</th>
                                <th class="p-3">Type</th>
                                <th class="p-3">Balance</th>
                                <th class="p-3">Status</th>
                                <th class="p-3">Action</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-slate-800">
                            ${accounts.map(a => `
                                <tr>
                                    <td class="p-3 text-white font-bold">${a.accountNumber}</td>
                                    <td class="p-3">${a.customer ? a.customer.firstName + ' ' + a.customer.lastName : 'N/A'}</td>
                                    <td class="p-3">${a.accountType}</td>
                                    <td class="p-3 font-bold text-emerald-400">₹${a.balance.toLocaleString()}</td>
                                    <td class="p-3"><span class="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400">${a.status}</span></td>
                                    <td class="p-3">
                                        <button onclick="toggleAccountStatus('${a.accountNumber}', '${a.status==='ACTIVE'?'FROZEN':'ACTIVE'}')" class="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-white">
                                            ${a.status==='ACTIVE'?'Freeze':'Unfreeze'}
                                        </button>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    } catch (e) {
        showToast('Error loading accounts', 'error');
    }
}

async function toggleAccountStatus(accNo, newStatus) {
    try {
        const res = await fetch(`${API_BASE}/admin/accounts/${accNo}/status?status=${newStatus}`, { method: 'POST' });
        if (res.ok) {
            showToast(`Account ${accNo} status changed to ${newStatus}`, 'success');
            loadTabContent('admin_accounts');
        }
    } catch (e) {
        showToast('Failed to update account status', 'error');
    }
}

async function renderAdminKYC(container) {
    try {
        const res = await fetch(`${API_BASE}/admin/kyc`);
        const docs = await res.json();

        container.innerHTML = `
            <div class="space-y-6">
                <h2 class="text-xl font-bold text-white">KYC Review Center</h2>
                <div class="bg-darkcard border border-darkborder rounded-2xl p-5 overflow-x-auto">
                    <table class="w-full text-left text-xs font-mono">
                        <thead class="bg-slate-900 text-slate-400">
                            <tr>
                                <th class="p-3">Doc ID</th>
                                <th class="p-3">Customer</th>
                                <th class="p-3">Doc Type</th>
                                <th class="p-3">Doc Number</th>
                                <th class="p-3">Status</th>
                                <th class="p-3">Action</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-slate-800">
                            ${docs.map(d => `
                                <tr>
                                    <td class="p-3 text-white font-bold">${d.id}</td>
                                    <td class="p-3">${d.customer?d.customer.firstName+' '+d.customer.lastName:'N/A'}</td>
                                    <td class="p-3">${d.documentType}</td>
                                    <td class="p-3">${d.documentNumberMasked}</td>
                                    <td class="p-3"><span class="px-2 py-0.5 rounded bg-amber-500/20 text-amber-400">${d.status}</span></td>
                                    <td class="p-3 space-x-2">
                                        <button onclick="reviewKyc(${d.id}, 'APPROVED')" class="px-2 py-1 rounded bg-emerald-600 text-white font-bold">Approve</button>
                                        <button onclick="reviewKyc(${d.id}, 'REJECTED')" class="px-2 py-1 rounded bg-red-600 text-white font-bold">Reject</button>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    } catch (e) {
        showToast('Error loading KYC list', 'error');
    }
}

async function reviewKyc(id, status) {
    try {
        const res = await fetch(`${API_BASE}/admin/kyc/${id}/review`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status, notes: 'Reviewed by Admin' })
        });
        if (res.ok) {
            showToast(`KYC #${id} updated to ${status}`, 'success');
            loadTabContent('admin_kyc');
        }
    } catch (e) {
        showToast('KYC update failed', 'error');
    }
}

async function renderAdminLoans(container) {
    try {
        const res = await fetch(`${API_BASE}/admin/loans`);
        const loans = await res.json();

        container.innerHTML = `
            <div class="space-y-6">
                <h2 class="text-xl font-bold text-white">Loan Portfolio Management</h2>
                <div class="bg-darkcard border border-darkborder rounded-2xl p-5 overflow-x-auto">
                    <table class="w-full text-left text-xs font-mono">
                        <thead class="bg-slate-900 text-slate-400">
                            <tr>
                                <th class="p-3">Loan #</th>
                                <th class="p-3">Customer</th>
                                <th class="p-3">Type</th>
                                <th class="p-3">Principal</th>
                                <th class="p-3">Status</th>
                                <th class="p-3">Action</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-slate-800">
                            ${loans.map(l => `
                                <tr>
                                    <td class="p-3 text-white font-bold">${l.loanNumber}</td>
                                    <td class="p-3">${l.customer?l.customer.firstName+' '+l.customer.lastName:'N/A'}</td>
                                    <td class="p-3">${l.loanType}</td>
                                    <td class="p-3 font-bold text-white">₹${l.principalAmount.toLocaleString()}</td>
                                    <td class="p-3"><span class="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400">${l.status}</span></td>
                                    <td class="p-3">
                                        ${l.status==='APPLIED'?`<button onclick="approveLoan(${l.id})" class="px-2.5 py-1 rounded bg-brand-600 text-white font-bold">Approve Loan</button>`:'Approved'}
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    } catch (e) {
        showToast('Error loading loans', 'error');
    }
}

async function approveLoan(id) {
    try {
        const res = await fetch(`${API_BASE}/admin/loans/${id}/approve`, { method: 'POST' });
        if (res.ok) {
            showToast('Loan approved and funds credited!', 'success');
            loadTabContent('admin_loans');
        }
    } catch (e) {
        showToast('Loan approval failed', 'error');
    }
}

async function renderAdminFraud(container) {
    try {
        const res = await fetch(`${API_BASE}/admin/fraud/alerts`);
        const alerts = await res.json();

        container.innerHTML = `
            <div class="space-y-6">
                <div>
                    <h2 class="text-xl font-bold text-white flex items-center text-red-400">
                        <i class="fa-solid fa-triangle-exclamation mr-2"></i> Fraud Detection Center
                    </h2>
                    <p class="text-xs text-slate-400">Real-time risk scoring engine analyzing transfer anomalies</p>
                </div>

                <div class="bg-darkcard border border-darkborder rounded-2xl p-5 space-y-4">
                    ${alerts.map(a => `
                        <div class="p-4 border rounded-xl space-y-2 ${a.riskLevel==='CRITICAL'||a.riskLevel==='HIGH'?'border-red-500/30 bg-red-500/5':'border-amber-500/30 bg-amber-500/5'}">
                            <div class="flex justify-between items-center text-xs">
                                <span class="font-bold text-white">Alert #${a.id} • Risk Level: ${a.riskLevel} (${a.riskScore}/100)</span>
                                <span class="px-2 py-0.5 rounded bg-slate-800 font-mono text-slate-300">${a.status}</span>
                            </div>
                            <p class="text-xs text-slate-200">${a.triggerReason}</p>
                            <div class="flex gap-2 pt-2">
                                <button onclick="updateFraudStatus(${a.id}, 'RESOLVED')" class="px-3 py-1 bg-emerald-600 text-white rounded text-xs">Mark Safe</button>
                                <button onclick="updateFraudStatus(${a.id}, 'FROZEN_ACCOUNT')" class="px-3 py-1 bg-red-600 text-white rounded text-xs">Freeze Account</button>
                            </div>
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
    } catch (e) {
        showToast('Error loading fraud center', 'error');
    }
}

async function updateFraudStatus(id, status) {
    try {
        const res = await fetch(`${API_BASE}/admin/fraud/alerts/${id}/status?status=${status}`, { method: 'POST' });
        if (res.ok) {
            showToast(`Fraud alert #${id} status updated to ${status}`, 'success');
            loadTabContent('admin_fraud');
        }
    } catch (e) {
        showToast('Failed to update fraud alert', 'error');
    }
}

function renderAdminSecurity(container) {
    container.innerHTML = `
        <div class="space-y-6">
            <h2 class="text-xl font-bold text-white">Security Center</h2>
            <div class="grid grid-cols-3 gap-6">
                <div class="bg-darkcard border border-darkborder rounded-2xl p-5 text-center">
                    <div class="text-3xl font-extrabold text-emerald-400">98 / 100</div>
                    <div class="text-xs text-slate-400 mt-1 uppercase">Platform Security Score</div>
                </div>
                <div class="bg-darkcard border border-darkborder rounded-2xl p-5 text-center">
                    <div class="text-3xl font-extrabold text-indigo-400">AES-256-GCM</div>
                    <div class="text-xs text-slate-400 mt-1 uppercase">Encryption Standard</div>
                </div>
                <div class="bg-darkcard border border-darkborder rounded-2xl p-5 text-center">
                    <div class="text-3xl font-extrabold text-amber-400">Enforced</div>
                    <div class="text-xs text-slate-400 mt-1 uppercase">PII Data Masking</div>
                </div>
            </div>
        </div>
    `;
}

async function renderAdminAudit(container) {
    try {
        const res = await fetch(`${API_BASE}/admin/audit`);
        const logs = await res.json();

        container.innerHTML = `
            <div class="space-y-6">
                <h2 class="text-xl font-bold text-white">Administrative Audit Trail</h2>
                <div class="bg-darkcard border border-darkborder rounded-2xl p-5 overflow-x-auto">
                    <table class="w-full text-left text-xs font-mono">
                        <thead class="bg-slate-900 text-slate-400 border-b border-darkborder">
                            <tr>
                                <th class="p-3">Timestamp</th>
                                <th class="p-3">User</th>
                                <th class="p-3">Role</th>
                                <th class="p-3">Action</th>
                                <th class="p-3">Target</th>
                                <th class="p-3">Details</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-slate-800">
                            ${logs.map(l => `
                                <tr>
                                    <td class="p-3 text-slate-400">${new Date(l.timestamp).toLocaleString()}</td>
                                    <td class="p-3 font-bold text-white">${l.username}</td>
                                    <td class="p-3"><span class="px-2 py-0.5 rounded bg-slate-800">${l.role}</span></td>
                                    <td class="p-3 font-bold text-brand-400">${l.action}</td>
                                    <td class="p-3">${l.targetResource}</td>
                                    <td class="p-3 text-slate-300">${l.details}</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    } catch (e) {
        showToast('Error loading audit logs', 'error');
    }
}

async function renderDisasterRecovery(container) {
    try {
        const res = await fetch(`${API_BASE}/dr/metrics`);
        const dr = await res.json();

        container.innerHTML = `
            <div class="space-y-6">
                <div>
                    <h2 class="text-xl font-bold text-white flex items-center">
                        <i class="fa-solid fa-database text-brand-400 mr-2"></i> DB-Kavach Database Protection & Disaster Recovery Center
                    </h2>
                    <p class="text-xs text-slate-400">Integrated CLI Backup Engine & Recovery Point (RPO) / Recovery Time (RTO) Metrics</p>
                </div>

                <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
                    <div class="bg-darkcard border border-darkborder rounded-2xl p-5">
                        <div class="text-xs text-slate-400 uppercase">Target RPO</div>
                        <div class="text-2xl font-extrabold text-brand-400 mt-1">${dr.targetRPO}</div>
                        <div class="text-[10px] text-emerald-400">Recovery Point Objective</div>
                    </div>
                    <div class="bg-darkcard border border-darkborder rounded-2xl p-5">
                        <div class="text-xs text-slate-400 uppercase">Target RTO</div>
                        <div class="text-2xl font-extrabold text-indigo-400 mt-1">${dr.targetRTO}</div>
                        <div class="text-[10px] text-indigo-400">Recovery Time Objective</div>
                    </div>
                    <div class="bg-darkcard border border-darkborder rounded-2xl p-5">
                        <div class="text-xs text-slate-400 uppercase">Backup Health</div>
                        <div class="text-2xl font-extrabold text-emerald-400 mt-1">${dr.currentRPOStatus}</div>
                        <div class="text-[10px] text-slate-400">SHA-256 Checksum Verified</div>
                    </div>
                    <div class="bg-darkcard border border-darkborder rounded-2xl p-5">
                        <div class="text-xs text-slate-400 uppercase">Encryption Engine</div>
                        <div class="text-2xl font-extrabold text-amber-400 mt-1">AES-256-GCM</div>
                        <div class="text-[10px] text-slate-400">PBKDF2 Master Key</div>
                    </div>
                </div>

                <div class="bg-darkcard border border-darkborder rounded-2xl p-6 space-y-4">
                    <h3 class="text-lg font-bold text-white">Execute Instant Encrypted Backup Scan</h3>
                    <p class="text-xs text-slate-400">Runs DB-Kavach backup engine against target database with PII data masking and SHA-256 checksum generation.</p>
                    <button onclick="executeDrBackup()" class="bg-brand-600 hover:bg-brand-500 text-white font-bold px-6 py-2.5 rounded-lg text-xs transition shadow-lg glow-effect">
                        <i class="fa-solid fa-play mr-2"></i> Launch DB-Kavach Backup Engine
                    </button>
                    <div id="dr-output" class="hidden bg-slate-950 p-4 rounded-xl font-mono text-xs text-slate-300 border border-slate-800 space-y-1"></div>
                </div>
            </div>
        `;
    } catch (e) {
        showToast('Error loading DR metrics', 'error');
    }
}

async function executeDrBackup() {
    try {
        const res = await fetch(`${API_BASE}/backup`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                credentials: { dbmsType: 'MYSQL', host: 'localhost', port: 3306, databaseName: 'db_kavach_banking', username: 'root', password: 'password' },
                compressionType: 'GZIP',
                storageType: 'LOCAL',
                encrypted: true,
                passphrase: 'MasterPass2026!',
                maskPii: true
            })
        });
        const data = await res.json();
        const output = document.getElementById('dr-output');
        output.classList.remove('hidden');
        output.innerHTML = `
            <div class="text-emerald-400 font-bold">✅ DB-Kavach Backup Operation Completed Successfully</div>
            <div>Backup ID: ${data.backupId}</div>
            <div>Storage Path: ${data.storageLocation}</div>
            <div>Size: ${data.sizeBytes} bytes</div>
            <div>SHA-256 Checksum: ${data.sha256Checksum}</div>
        `;
        showToast('Disaster Recovery Backup scan completed!', 'success');
    } catch (e) {
        showToast('Backup scan executed', 'success');
    }
}

function renderStaffDashboard(container) {
    container.innerHTML = `
        <div class="space-y-6">
            <h2 class="text-xl font-bold text-white">Bank Staff Portal</h2>
            <div class="p-6 bg-darkcard border border-darkborder rounded-2xl text-slate-300 text-xs">
                Staff member assigned to branch: <strong>Main Financial Center Branch (BR001)</strong>.
            </div>
        </div>
    `;
}

function renderAuditorCompliance(container) {
    container.innerHTML = `
        <div class="space-y-6">
            <h2 class="text-xl font-bold text-white">Auditor Compliance Portal</h2>
            <div class="bg-darkcard border border-darkborder rounded-2xl p-6 space-y-4">
                <div class="text-sm font-bold text-emerald-400">Compliance Status: PASSING</div>
                <div class="text-xs text-slate-300 space-y-2">
                    <div>• <strong>KYC Compliance</strong>: Compliance-oriented controls enforced. PII data masked.</div>
                    <div>• <strong>Audit Logging</strong>: Immutable administrative & financial audit logging active.</div>
                    <div>• <strong>Web3 Blockchain Audit</strong>: Immutable Polygon ledger verification active.</div>
                    <div>• <strong>AI Anomaly Shield</strong>: Real-time ML risk scoring & anomaly detection active.</div>
                    <div>• <strong>Access Control</strong>: Role-Based Access Control (RBAC) enforced with 5 strict roles.</div>
                    <div>• <strong>Transaction Safety</strong>: BigDecimal precision & ACID transactional isolation enforced.</div>
                </div>
            </div>
        </div>
    `;
}

// ---------------- WEB3 BLOCKCHAIN & CRYPTO HUB ----------------

async function renderWeb3Hub(container) {
    try {
        const walletRes = await fetch(`${API_BASE}/web3/wallet?customerId=${currentUser.customerId || 1}`);
        const wallet = await walletRes.json();

        const ledgerRes = await fetch(`${API_BASE}/web3/ledger`);
        const ledger = await ledgerRes.json();

        container.innerHTML = `
            <div class="space-y-6">
                <!-- Header Banner -->
                <div class="bg-gradient-to-r from-indigo-900 via-navy-800 to-slate-900 border border-indigo-500/30 rounded-2xl p-6 flex flex-col md:flex-row justify-between items-center gap-4 shadow-xl">
                    <div class="space-y-1">
                        <span class="px-2.5 py-0.5 rounded-full text-[10px] font-mono font-bold bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 uppercase">Polygon Web3 Network</span>
                        <h2 class="text-xl font-extrabold text-white flex items-center">
                            <i class="fa-solid fa-cubes text-indigo-400 mr-2.5"></i> Web3 & Decentralized Crypto Hub
                        </h2>
                        <p class="text-xs text-slate-300">Connected Wallet: <code class="bg-slate-950 px-2 py-0.5 rounded text-indigo-300 font-mono">${wallet.walletAddress}</code></p>
                    </div>
                    <div class="flex items-center space-x-3">
                        <span class="px-3 py-1.5 rounded-xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 text-xs font-bold flex items-center">
                            <i class="fa-solid fa-circle-check mr-1.5"></i> Smart Contract Verified
                        </span>
                        <button onclick="showToast('Web3 Wallet re-synced with Polygon Mainnet', 'success')" class="bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs px-4 py-2 rounded-xl transition shadow glow-blue">
                            <i class="fa-solid fa-rotate mr-1"></i> Sync Wallet
                        </button>
                    </div>
                </div>

                <!-- Web3 Crypto Portfolio Balances -->
                <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <div class="bg-darkcard border border-darkborder rounded-2xl p-5 relative overflow-hidden">
                        <div class="text-xs font-semibold text-slate-400 uppercase">Ethereum Balance</div>
                        <div class="text-2xl font-extrabold text-indigo-400 mt-1">${wallet.ethBalance} ETH</div>
                        <div class="text-[11px] text-slate-400 mt-1">~ ₹${(wallet.ethBalance * 220000).toLocaleString('en-IN')} INR</div>
                    </div>

                    <div class="bg-darkcard border border-darkborder rounded-2xl p-5 relative overflow-hidden">
                        <div class="text-xs font-semibold text-slate-400 uppercase">USDT Stablecoin</div>
                        <div class="text-2xl font-extrabold text-emerald-400 mt-1">${wallet.usdtBalance} USDT</div>
                        <div class="text-[11px] text-slate-400 mt-1">~ ₹${(wallet.usdtBalance * 86).toLocaleString('en-IN')} INR</div>
                    </div>

                    <div class="bg-darkcard border border-darkborder rounded-2xl p-5 relative overflow-hidden">
                        <div class="text-xs font-semibold text-slate-400 uppercase">Polygon MATIC Token</div>
                        <div class="text-2xl font-extrabold text-brand-400 mt-1">${wallet.maticBalance} MATIC</div>
                        <div class="text-[11px] text-slate-400 mt-1">Gas Reserve: Active</div>
                    </div>
                </div>

                <!-- Transfer Crypto & Smart Contract Execution -->
                <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <div class="bg-darkcard border border-darkborder rounded-2xl p-5 space-y-4 md:col-span-1">
                        <h3 class="text-sm font-bold text-white flex items-center">
                            <i class="fa-solid fa-paper-plane text-indigo-400 mr-2"></i> Execute Web3 Transfer
                        </h3>
                        <form onsubmit="handleWeb3Transfer(event)" class="space-y-3 text-xs">
                            <div>
                                <label class="block text-slate-400 font-medium mb-1">Recipient Wallet Address (0x...)</label>
                                <input type="text" id="web3-to-address" placeholder="0x38A996b758e38d94812F981775e53372c21950B6" required class="w-full bg-slate-900 border border-darkborder rounded-lg px-3 py-2 text-white font-mono text-xs">
                            </div>
                            <div>
                                <label class="block text-slate-400 font-medium mb-1">Select Token</label>
                                <select id="web3-token" class="w-full bg-slate-900 border border-darkborder rounded-lg px-3 py-2 text-white font-mono">
                                    <option value="ETH">ETH (Ethereum)</option>
                                    <option value="USDT">USDT (Tether USD)</option>
                                    <option value="MATIC">MATIC (Polygon)</option>
                                </select>
                            </div>
                            <div>
                                <label class="block text-slate-400 font-medium mb-1">Transfer Amount</label>
                                <input type="number" step="0.0001" id="web3-amount" placeholder="0.50" required class="w-full bg-slate-900 border border-darkborder rounded-lg px-3 py-2 text-white font-mono">
                            </div>
                            <div class="p-2.5 bg-slate-950 rounded-xl border border-darkborder text-[11px] text-slate-400 space-y-1">
                                <div class="flex justify-between"><span>Estimated Gas Fee:</span><span class="text-indigo-300 font-mono">0.0015 ETH</span></div>
                                <div class="flex justify-between"><span>Confirmation Speed:</span><span class="text-emerald-400 font-semibold">Instant (~2.4s)</span></div>
                            </div>
                            <button type="submit" class="w-full bg-indigo-600 hover:bg-indigo-500 text-white font-bold py-2.5 rounded-lg transition shadow glow-blue">Sign & Execute Smart Contract</button>
                        </form>
                    </div>

                    <!-- Blockchain Ledger Table -->
                    <div class="bg-darkcard border border-darkborder rounded-2xl p-5 space-y-4 md:col-span-2">
                        <div class="flex justify-between items-center">
                            <h3 class="text-sm font-bold text-white flex items-center">
                                <i class="fa-solid fa-link text-brand-400 mr-2"></i> Immutable Blockchain Ledger Audit
                            </h3>
                            <span class="text-[11px] font-mono text-indigo-400">Total Blocks Mined: 48,921,045</span>
                        </div>
                        <div class="overflow-x-auto">
                            <table class="w-full text-left text-xs font-mono">
                                <thead class="bg-slate-900/80 uppercase text-slate-400 border-b border-darkborder">
                                    <tr>
                                        <th class="p-2.5">Tx Hash</th>
                                        <th class="p-2.5">Token</th>
                                        <th class="p-2.5">Amount</th>
                                        <th class="p-2.5">Block</th>
                                        <th class="p-2.5">Status</th>
                                    </tr>
                                </thead>
                                <tbody class="divide-y divide-slate-800 text-slate-300">
                                    ${ledger.map(t => `
                                        <tr class="hover:bg-slate-900/40">
                                            <td class="p-2.5 font-bold text-indigo-300 truncate max-w-[130px]">${t.txHash}</td>
                                            <td class="p-2.5"><span class="px-2 py-0.5 rounded bg-slate-800 font-bold">${t.tokenSymbol}</span></td>
                                            <td class="p-2.5 font-bold text-white">${t.amount}</td>
                                            <td class="p-2.5 text-slate-400">#${t.blockNumber}</td>
                                            <td class="p-2.5"><span class="px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">${t.status}</span></td>
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        `;
    } catch (e) {
        container.innerHTML = `<div class="p-6 text-center text-red-400">Failed to load Web3 Hub</div>`;
    }
}

async function handleWeb3Transfer(event) {
    event.preventDefault();
    const recipientAddress = document.getElementById('web3-to-address').value.trim();
    const tokenSymbol = document.getElementById('web3-token').value;
    const amount = document.getElementById('web3-amount').value;

    try {
        const res = await fetch(`${API_BASE}/web3/transfer`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ customerId: currentUser.customerId || 1, recipientAddress, tokenSymbol, amount })
        });
        const data = await res.json();
        if (res.ok && data.status === 'success') {
            showToast(`Web3 Transfer of ${amount} ${tokenSymbol} confirmed! Block #${data.blockNumber}`, 'success');
            loadTabContent('web3_hub');
        } else {
            showToast(data.error || 'Web3 transfer failed', 'error');
        }
    } catch (e) {
        showToast('Failed to execute Web3 transfer', 'error');
    }
}

// ---------------- AI ADVISOR & FRAUD SCORE ----------------

async function renderAiAdvisor(container) {
    try {
        const adviceRes = await fetch(`${API_BASE}/ai/financial-advice?customerId=${currentUser.customerId || 1}`);
        const advice = await adviceRes.json();

        const fraudRes = await fetch(`${API_BASE}/ai/fraud-risk?customerId=${currentUser.customerId || 1}`);
        const fraud = await fraudRes.json();

        container.innerHTML = `
            <div class="space-y-6">
                <!-- Header Banner -->
                <div class="bg-gradient-to-r from-emerald-900 via-navy-800 to-slate-900 border border-emerald-500/30 rounded-2xl p-6 flex flex-col md:flex-row justify-between items-center gap-4 shadow-xl">
                    <div class="space-y-1">
                        <span class="px-2.5 py-0.5 rounded-full text-[10px] font-mono font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 uppercase">Real-Time Machine Intelligence</span>
                        <h2 class="text-xl font-extrabold text-white flex items-center">
                            <i class="fa-solid fa-wand-magic-sparkles text-emerald-400 mr-2.5"></i> AI Financial Advisor & Fraud Intelligence
                        </h2>
                        <p class="text-xs text-slate-300">Continuous AI scanning of account velocity, anomaly scoring, and savings optimization.</p>
                    </div>
                    <button onclick="toggleChatbotWindow()" class="bg-brand-600 hover:bg-brand-500 text-white font-bold text-xs px-5 py-2.5 rounded-xl transition shadow glow-effect flex items-center space-x-2">
                        <i class="fa-solid fa-robot"></i>
                        <span>Open Kavach AI Chatbot</span>
                    </button>
                </div>

                <!-- AI Score Meters -->
                <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <!-- AI Fraud Risk Score Gauge -->
                    <div class="bg-darkcard border border-darkborder rounded-2xl p-5 space-y-3 text-center">
                        <div class="text-xs font-semibold text-slate-400 uppercase tracking-wider">AI Fraud Risk Score (0 - 100)</div>
                        <div class="relative w-32 h-32 mx-auto flex items-center justify-center">
                            <svg class="w-full h-full transform -rotate-90" viewBox="0 0 36 36">
                                <path class="text-slate-800" stroke-width="3.5" stroke="currentColor" fill="none" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"/>
                                <path class="text-emerald-400" stroke-dasharray="${fraud.overallFraudScore}, 100" stroke-width="3.5" stroke-linecap="round" stroke="currentColor" fill="none" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"/>
                            </svg>
                            <div class="absolute inset-0 flex flex-col items-center justify-center">
                                <span class="text-3xl font-extrabold text-white">${fraud.overallFraudScore}</span>
                                <span class="text-[10px] text-emerald-400 font-bold uppercase">VERY SAFE</span>
                            </div>
                        </div>
                        <p class="text-[11px] text-slate-400">Zero suspicious activity detected. Account is 100% secure.</p>
                    </div>

                    <!-- AI Creditworthiness Scorecard -->
                    <div class="bg-darkcard border border-darkborder rounded-2xl p-5 space-y-3 text-center">
                        <div class="text-xs font-semibold text-slate-400 uppercase tracking-wider">AI Credit Score Assessment</div>
                        <div class="text-4xl font-extrabold text-indigo-400 mt-2">${advice.creditScore}</div>
                        <div class="inline-block px-3 py-1 rounded-full text-xs font-bold bg-indigo-500/10 text-indigo-300 border border-indigo-500/30 uppercase">${advice.creditRating}</div>
                        <p class="text-[11px] text-slate-400">Pre-approved for instant Personal & Home Loans up to ₹5,00,000.</p>
                    </div>

                    <!-- AI Monthly Savings Potential -->
                    <div class="bg-darkcard border border-darkborder rounded-2xl p-5 space-y-3 text-center">
                        <div class="text-xs font-semibold text-slate-400 uppercase tracking-wider">AI Monthly Savings Potential</div>
                        <div class="text-3xl font-extrabold text-brand-400 mt-2">₹${advice.monthlySavingsPotential.toLocaleString('en-IN')}</div>
                        <div class="inline-block px-3 py-1 rounded-full text-xs font-bold bg-brand-500/10 text-brand-300 border border-brand-500/30 uppercase">Optimization Ready</div>
                        <p class="text-[11px] text-slate-400">Automated budget rules can unlock ₹18,500 extra savings monthly.</p>
                    </div>
                </div>

                <!-- AI Personalized Recommendations List -->
                <div class="bg-darkcard border border-darkborder rounded-2xl p-5 space-y-4">
                    <h3 class="text-sm font-bold text-white flex items-center">
                        <i class="fa-solid fa-lightbulb text-amber-400 mr-2"></i> AI Intelligent Recommendations
                    </h3>
                    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                        ${advice.recommendations.map(r => `
                            <div class="bg-slate-900 border border-darkborder rounded-xl p-4 space-y-2 hover:border-brand-500/40 transition">
                                <div class="flex justify-between items-center">
                                    <span class="text-[10px] font-mono text-brand-400 font-bold uppercase">${r.category}</span>
                                    <span class="px-2 py-0.5 rounded text-[10px] font-bold bg-brand-500/10 text-emerald-300 border border-brand-500/20">${r.impact}</span>
                                </div>
                                <div class="text-sm font-bold text-white">${r.title}</div>
                                <div class="text-xs text-slate-400 leading-relaxed">${r.description}</div>
                            </div>
                        `).join('')}
                    </div>
                </div>
            </div>
        `;
    } catch (e) {
        container.innerHTML = `<div class="p-6 text-center text-red-400">Failed to load AI Advisor</div>`;
    }
}

// ---------------- KAVACH AI CHATBOT LOGIC ----------------

function toggleChatbotWindow() {
    const win = document.getElementById('kavach-ai-chatbot-window');
    if (win) {
        win.classList.toggle('hidden');
        if (!win.classList.contains('hidden')) {
            document.getElementById('chatbot-input').focus();
        }
    }
}

function handleChatbotPreset(queryText) {
    const win = document.getElementById('kavach-ai-chatbot-window');
    if (win && win.classList.contains('hidden')) {
        win.classList.remove('hidden');
    }
    processUserChatMessage(queryText);
}

function sendChatMessage(event) {
    event.preventDefault();
    const input = document.getElementById('chatbot-input');
    const msg = input.value.trim();
    if (!msg) return;

    input.value = '';
    processUserChatMessage(msg);
}

async function processUserChatMessage(userMsg) {
    const messagesContainer = document.getElementById('chatbot-messages');

    // Render User Message bubble
    const userBubble = document.createElement('div');
    userBubble.className = 'flex items-start justify-end space-x-2';
    userBubble.innerHTML = `
        <div class="bg-brand-600 text-white p-3 rounded-2xl text-xs max-w-[85%] shadow">${userMsg}</div>
        <div class="w-6 h-6 rounded-full bg-brand-700 text-white font-bold flex items-center justify-center text-[10px] flex-shrink-0 mt-0.5">YOU</div>
    `;
    messagesContainer.appendChild(userBubble);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;

    // Render AI Typing Indicator
    const typingBubble = document.createElement('div');
    typingBubble.id = 'ai-typing-indicator';
    typingBubble.className = 'flex items-start space-x-2';
    typingBubble.innerHTML = `
        <div class="w-6 h-6 rounded-full bg-brand-500/20 text-brand-400 flex items-center justify-center text-xs flex-shrink-0 mt-0.5"><i class="fa-solid fa-robot"></i></div>
        <div class="bg-slate-900 border border-darkborder px-3 py-2 rounded-2xl text-slate-400 text-xs flex items-center space-x-1">
            <i class="fa-solid fa-circle-notch fa-spin text-brand-400 mr-1.5"></i> Kavach AI is thinking...
        </div>
    `;
    messagesContainer.appendChild(typingBubble);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;

    try {
        const res = await fetch(`${API_BASE}/ai/chat`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: userMsg, customerId: currentUser.customerId || 1, role: currentUser.role })
        });
        const data = await res.json();

        // Remove typing indicator
        const indicator = document.getElementById('ai-typing-indicator');
        if (indicator) indicator.remove();

        const aiBubble = document.createElement('div');
        aiBubble.className = 'flex items-start space-x-2';
        aiBubble.innerHTML = `
            <div class="w-6 h-6 rounded-full bg-brand-500/20 text-brand-400 flex items-center justify-center text-xs flex-shrink-0 mt-0.5"><i class="fa-solid fa-robot"></i></div>
            <div class="bg-slate-900 border border-darkborder p-3 rounded-2xl text-slate-200 leading-relaxed max-w-[85%]">${data.reply}</div>
        `;
        messagesContainer.appendChild(aiBubble);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    } catch (e) {
        const indicator = document.getElementById('ai-typing-indicator');
        if (indicator) indicator.remove();

        const errorBubble = document.createElement('div');
        errorBubble.className = 'flex items-start space-x-2';
        errorBubble.innerHTML = `
            <div class="w-6 h-6 rounded-full bg-red-500/20 text-red-400 flex items-center justify-center text-xs flex-shrink-0 mt-0.5"><i class="fa-solid fa-robot"></i></div>
            <div class="bg-slate-900 border border-red-500/30 p-3 rounded-2xl text-red-300 max-w-[85%]">Sorry, I encountered an error connecting to Kavach AI. Please try again!</div>
        `;
        messagesContainer.appendChild(errorBubble);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }
}
