// DB Kavach Dashboard Application Logic

const API_BASE = '/api';

document.addEventListener('DOMContentLoaded', () => {
    checkBackendStatus();
    loadHistory();
    runQuickAudit();
});

async function checkBackendStatus() {
    const textEl = document.getElementById('api-status-text');
    const dotEl = document.getElementById('api-status-dot');
    const pillEl = document.getElementById('api-status-pill');

    try {
        const res = await fetch(`${API_BASE}/history`);
        if (res.ok) {
            if (textEl) textEl.textContent = 'API Active';
            if (dotEl) dotEl.className = 'w-2 h-2 rounded-full bg-emerald-400 animate-ping mr-2';
            if (pillEl) pillEl.className = 'inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20';
        } else {
            throw new Error('API return non-ok status');
        }
    } catch (e) {
        if (textEl) textEl.textContent = 'Backend Disconnected';
        if (dotEl) dotEl.className = 'w-2 h-2 rounded-full bg-amber-400 mr-2';
        if (pillEl) pillEl.className = 'inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-amber-500/10 text-amber-400 border border-amber-500/20';
    }
}

function switchTab(tabId) {
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.remove('text-brand-500', 'bg-brand-500/10', 'border', 'border-brand-500/20');
        btn.classList.add('text-slate-400');
    });

    const activeBtn = document.getElementById(`tab-${tabId}`);
    if (activeBtn) {
        activeBtn.classList.remove('text-slate-400');
        activeBtn.classList.add('text-brand-500', 'bg-brand-500/10', 'border', 'border-brand-500/20');
    }

    const views = ['dashboard', 'security', 'backup', 'restore', 'vault', 'history'];
    views.forEach(v => {
        const el = document.getElementById(`view-${v}`);
        if (el) el.classList.add('hidden');
    });

    const activeView = document.getElementById(`view-${tabId}`);
    if (activeView) activeView.classList.remove('hidden');

    if (tabId === 'history') loadHistory();
}

function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    const toastMsg = document.getElementById('toast-message');
    const toastIcon = document.getElementById('toast-icon');

    toast.classList.remove('hidden', 'bg-emerald-500/10', 'border-emerald-500/30', 'text-emerald-300', 'bg-red-500/10', 'border-red-500/30', 'text-red-300');

    if (type === 'success') {
        toast.classList.add('bg-emerald-500/10', 'border-emerald-500/30', 'text-emerald-300');
        toastIcon.className = 'fa-solid fa-circle-check text-emerald-400';
    } else {
        toast.classList.add('bg-red-500/10', 'border-red-500/30', 'text-red-300');
        toastIcon.className = 'fa-solid fa-circle-exclamation text-red-400';
    }

    toastMsg.textContent = message;
}

function hideToast() {
    document.getElementById('toast').classList.add('hidden');
}

async function runQuickAudit() {
    try {
        const payload = {
            dbmsType: 'MYSQL',
            host: 'localhost',
            port: 3306,
            databaseName: 'backup_database_java',
            username: 'db_backup_user',
            password: 'SecuredBackupPass2026!#',
            connectionUri: 'jdbc:mysql://localhost:3306/backup_database_java?useSSL=true'
        };
        const res = await fetch(`${API_BASE}/security/audit`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (res.ok) {
            const data = await res.json();
            document.getElementById('stat-score').textContent = `${data.score}/100`;
            document.getElementById('stat-rating').textContent = data.rating;
        }
    } catch (e) {
        console.warn('Initial audit preview skipped:', e);
    }
}

async function handleAuditSubmit(e) {
    e.preventDefault();
    const payload = {
        dbmsType: document.getElementById('audit-dbms').value,
        host: document.getElementById('audit-host').value,
        port: parseInt(document.getElementById('audit-port').value),
        databaseName: document.getElementById('audit-database').value,
        username: document.getElementById('audit-username').value,
        password: document.getElementById('audit-password').value,
        connectionUri: document.getElementById('audit-uri').value
    };

    try {
        const res = await fetch(`${API_BASE}/security/audit`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await res.json();

        if (res.ok) {
            document.getElementById('audit-result-container').classList.remove('hidden');
            document.getElementById('res-target').textContent = `${data.dbmsType} [${data.databaseName}]`;
            document.getElementById('res-timestamp').textContent = `Audited on ${new Date(data.timestamp).toLocaleString()}`;
            document.getElementById('res-score').textContent = `${data.score} / 100`;
            document.getElementById('res-rating').textContent = data.rating;

            const findingsContainer = document.getElementById('res-findings');
            findingsContainer.innerHTML = '';

            if (data.findings.length === 0) {
                findingsContainer.innerHTML = '<div class="p-4 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-emerald-300 text-sm">✅ No security vulnerabilities detected!</div>';
            } else {
                data.findings.forEach((f, idx) => {
                    const sevColor = f.severity === 'HIGH' ? 'border-red-500/30 bg-red-500/5 text-red-400' :
                                    f.severity === 'MEDIUM' ? 'border-amber-500/30 bg-amber-500/5 text-amber-400' : 'border-slate-700 bg-slate-900 text-slate-300';

                    const item = document.createElement('div');
                    item.className = `p-4 border rounded-xl space-y-1 ${sevColor}`;
                    item.innerHTML = `
                        <div class="flex justify-between items-center text-xs font-bold uppercase">
                            <span>[${idx + 1}] Category: ${f.category}</span>
                            <span class="px-2 py-0.5 rounded bg-slate-800">${f.severity}</span>
                        </div>
                        <h5 class="text-sm font-bold text-white">${f.title}</h5>
                        <p class="text-xs text-slate-300">${f.description}</p>
                        <p class="text-xs text-brand-400 font-mono mt-1"><strong>Remediation:</strong> ${f.recommendation}</p>
                    `;
                    findingsContainer.appendChild(item);
                });
            }
            showToast(`Security Audit completed! Rating: ${data.rating} (${data.score}/100)`, 'success');
        } else {
            showToast(data.error || 'Audit failed.', 'error');
        }
    } catch (err) {
        showToast('Error connecting to security audit API.', 'error');
    }
}

async function handleBackupSubmit(e) {
    e.preventDefault();
    const payload = {
        credentials: {
            dbmsType: document.getElementById('bk-dbms').value,
            host: document.getElementById('bk-host').value,
            port: parseInt(document.getElementById('bk-port').value),
            databaseName: document.getElementById('bk-database').value,
            username: document.getElementById('bk-username').value,
            password: document.getElementById('bk-password').value
        },
        compressionType: document.getElementById('bk-compression').value,
        storageType: document.getElementById('bk-storage').value,
        encrypted: document.getElementById('bk-encrypt').checked,
        passphrase: document.getElementById('bk-passphrase').value,
        maskPii: document.getElementById('bk-mask').checked
    };

    try {
        const res = await fetch(`${API_BASE}/backup`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await res.json();

        if (res.ok) {
            const output = document.getElementById('backup-output');
            output.classList.remove('hidden');
            document.getElementById('backup-output-details').innerHTML = `
                <div><strong>Backup ID:</strong> ${data.backupId}</div>
                <div><strong>DBMS:</strong> ${data.dbmsType} [${data.databaseName}]</div>
                <div><strong>Compression:</strong> ${data.compressionType}</div>
                <div><strong>Storage Location:</strong> ${data.storageLocation}</div>
                <div><strong>Size:</strong> ${data.sizeBytes} bytes</div>
                <div><strong>SHA-256 Checksum:</strong> ${data.sha256Checksum}</div>
            `;
            showToast(`Backup [${data.backupId}] completed successfully!`, 'success');
            loadHistory();
        } else {
            showToast(data.error || 'Backup execution failed.', 'error');
        }
    } catch (err) {
        console.error('Backup API Error:', err);
        showToast(err.message ? `Backup API Error: ${err.message}` : 'Error connecting to backup API. Make sure java -jar target/db-backup-cli-1.0.0.jar is running on port 8080.', 'error');
    }
}

async function handleRestoreSubmit(e) {
    e.preventDefault();
    const payload = {
        targetCredentials: {
            dbmsType: document.getElementById('rst-dbms').value,
            host: document.getElementById('rst-host').value,
            port: parseInt(document.getElementById('rst-port').value),
            databaseName: document.getElementById('rst-database').value,
            username: document.getElementById('rst-username').value,
            password: document.getElementById('rst-password').value
        },
        backupSourcePath: document.getElementById('rst-file').value,
        passphrase: document.getElementById('rst-passphrase').value
    };

    try {
        const res = await fetch(`${API_BASE}/restore`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await res.json();

        if (res.ok) {
            showToast('Restore operation completed successfully!', 'success');
            loadHistory();
        } else {
            showToast(data.error || 'Restore failed.', 'error');
        }
    } catch (err) {
        showToast('Error connecting to restore API.', 'error');
    }
}

async function loadVaultProfiles() {
    const pass = document.getElementById('vault-master-pass').value;
    if (!pass) {
        showToast('Master password required.', 'error');
        return;
    }
    try {
        const res = await fetch(`${API_BASE}/security/vault/list?masterPassword=${encodeURIComponent(pass)}`, {
            method: 'POST'
        });
        const data = await res.json();
        const container = document.getElementById('vault-profiles-container');
        container.innerHTML = '';

        if (res.ok && data.profiles) {
            if (data.profiles.length === 0) {
                container.innerHTML = '<p class="text-xs text-slate-400">Vault unlocked. No saved profiles found.</p>';
            } else {
                data.profiles.forEach(p => {
                    const item = document.createElement('div');
                    item.className = 'p-3 bg-slate-900 border border-darkborder rounded-xl flex justify-between items-center text-xs font-mono text-white';
                    item.innerHTML = `
                        <div class="flex items-center space-x-2">
                            <i class="fa-solid fa-key text-amber-400"></i>
                            <span class="font-bold">${p}</span>
                        </div>
                        <span class="text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/20">AES-256 Encrypted</span>
                    `;
                    container.appendChild(item);
                });
            }
            showToast('Vault unlocked successfully!', 'success');
        } else {
            showToast(data.error || 'Incorrect master password.', 'error');
        }
    } catch (err) {
        showToast('Error connecting to Vault API.', 'error');
    }
}

async function loadHistory() {
    try {
        const res = await fetch(`${API_BASE}/history`);
        if (res.ok) {
            const data = await res.json();
            document.getElementById('stat-total-backups').textContent = data.length;

            const tbody = document.getElementById('history-tbody');
            tbody.innerHTML = '';

            if (data.length === 0) {
                tbody.innerHTML = '<tr><td colspan="8" class="p-4 text-center text-slate-500">No backup activity logged yet.</td></tr>';
                return;
            }

            data.forEach(r => {
                const tr = document.createElement('tr');
                tr.className = 'hover:bg-slate-900/50 transition';
                const statusBadge = r.status === 'SUCCESS' ? '<span class="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">SUCCESS</span>' :
                                                            '<span class="px-2 py-0.5 rounded bg-red-500/20 text-red-400 border border-red-500/30">FAILED</span>';

                tr.innerHTML = `
                    <td class="p-3 font-bold text-white">${r.backupId}</td>
                    <td class="p-3 text-slate-300">${r.operation}</td>
                    <td class="p-3 text-slate-300">${r.dbmsType}</td>
                    <td class="p-3 text-slate-300">${r.databaseName}</td>
                    <td class="p-3">${statusBadge}</td>
                    <td class="p-3 text-slate-400">${r.durationMs}ms</td>
                    <td class="p-3 text-slate-400">${r.sizeBytes || 0} bytes</td>
                    <td class="p-3 text-slate-400">${new Date(r.startTime).toLocaleString()}</td>
                `;
                tbody.appendChild(tr);
            });
        }
    } catch (err) {
        console.warn('Failed to load history:', err);
    }
}
