// API Base URL
const API_BASE = '/api/v1';

// State
let currentFeatures = [];
let currentTab = 'features';
let selectedFeatureId = null;

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    refreshDashboard();
});

// Refresh dashboard
async function refreshDashboard() {
    showLoading(true);
    try {
        await loadFeatures();
        if (currentTab === 'audit') {
            await loadAuditLogs();
        }
    } catch (error) {
        console.error('Error refreshing dashboard:', error);
        showToast('Failed to refresh dashboard', 'error');
    } finally {
        showLoading(false);
    }
}

// Load all features
async function loadFeatures() {
    try {
        const response = await fetch(`${API_BASE}/features?includeRules=true`);
        const result = await response.json();

        if (result.success && result.data) {
            currentFeatures = result.data;
            updateSummaryCards();
            renderFeatures();
        }
    } catch (error) {
        console.error('Error loading features:', error);
        throw error;
    }
}

// Update summary cards
function updateSummaryCards() {
    const totalFeatures = currentFeatures.length;
    const enabledFeatures = currentFeatures.filter(f => f.enabled).length;
    const totalRules = currentFeatures.reduce((sum, f) => sum + (f.rules?.length || 0), 0);

    document.getElementById('total-features').textContent = totalFeatures;
    document.getElementById('enabled-features').textContent = enabledFeatures;
    document.getElementById('total-rules').textContent = totalRules;
}

// Render features list
function renderFeatures() {
    const container = document.getElementById('features-list');
    const emptyState = document.getElementById('features-empty');

    if (currentFeatures.length === 0) {
        container.style.display = 'none';
        emptyState.style.display = 'block';
        return;
    }

    container.style.display = 'grid';
    emptyState.style.display = 'none';

    container.innerHTML = currentFeatures.map(feature => createFeatureCard(feature)).join('');
}

// Create feature card HTML
function createFeatureCard(feature) {
    const statusClass = feature.enabled ? 'enabled' : 'disabled';
    const statusIcon = feature.enabled ? '✓' : '✗';

    return `
        <div class="feature-card">
            <div class="feature-header">
                <div class="feature-title">
                    <h3>${escapeHtml(feature.name)}</h3>
                    <span class="feature-key">${escapeHtml(feature.key)}</span>
                </div>
                <span class="feature-status ${statusClass}">
                    ${statusIcon} ${feature.enabled ? 'Enabled' : 'Disabled'}
                </span>
            </div>
            <p class="feature-description">${escapeHtml(feature.description || 'No description')}</p>
            <div class="feature-meta">
                <div class="feature-rules">
                    📏 ${feature.ruleCount || 0} rule(s)
                </div>
                <div class="feature-actions">
                    <button class="icon-btn" onclick="toggleFeature(${feature.id})" title="Toggle">
                        ${feature.enabled ? '⏸️' : '▶️'}
                    </button>
                    <button class="icon-btn" onclick="showRulesModal(${feature.id})" title="Manage Rules">📋</button>
                    <button class="icon-btn" onclick="editFeature(${feature.id})" title="Edit">✏️</button>
                    <button class="icon-btn" onclick="deleteFeature(${feature.id})" title="Delete">🗑️</button>
                </div>
            </div>
        </div>
    `;
}

// Filter features
function filterFeatures() {
    const searchText = document.getElementById('feature-search').value.toLowerCase();

    if (searchText.length === 0) {
        renderFeatures();
        return;
    }

    const filtered = currentFeatures.filter(feature =>
        feature.name.toLowerCase().includes(searchText) ||
        feature.key.toLowerCase().includes(searchText) ||
        (feature.description && feature.description.toLowerCase().includes(searchText))
    );

    const container = document.getElementById('features-list');
    const emptyState = document.getElementById('features-empty');

    if (filtered.length === 0) {
        container.style.display = 'none';
        emptyState.style.display = 'block';
        emptyState.querySelector('h2').textContent = 'No Features Found';
        emptyState.querySelector('p').textContent = 'No features match your search';
        return;
    }

    container.style.display = 'grid';
    emptyState.style.display = 'none';
    container.innerHTML = filtered.map(feature => createFeatureCard(feature)).join('');
}

// Switch tabs
function switchTab(tab) {
    currentTab = tab;

    // Update tab buttons
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    event.target.classList.add('active');

    // Show/hide tab content
    document.querySelectorAll('.tab-content').forEach(content => content.style.display = 'none');
    document.getElementById(`${tab}-tab`).style.display = 'block';

    // Load data for the selected tab
    if (tab === 'audit') {
        loadAuditLogs();
    }
}

// Show create feature modal
function showCreateFeatureModal() {
    document.getElementById('feature-modal-title').textContent = 'Create Feature Flag';
    document.getElementById('feature-form').reset();
    document.getElementById('feature-id').value = '';
    document.getElementById('feature-key').disabled = false;
    document.getElementById('feature-modal').style.display = 'block';
}

// Edit feature
function editFeature(id) {
    const feature = currentFeatures.find(f => f.id === id);
    if (!feature) return;

    document.getElementById('feature-modal-title').textContent = 'Edit Feature Flag';
    document.getElementById('feature-id').value = feature.id;
    document.getElementById('feature-key').value = feature.key;
    document.getElementById('feature-key').disabled = true;
    document.getElementById('feature-name').value = feature.name;
    document.getElementById('feature-description').value = feature.description || '';
    document.getElementById('feature-enabled').checked = feature.enabled;
    document.getElementById('feature-created-by').value = feature.updatedBy || '';

    document.getElementById('feature-modal').style.display = 'block';
}

// Close feature modal
function closeFeatureModal() {
    document.getElementById('feature-modal').style.display = 'none';
}

// Save feature
async function saveFeature(event) {
    event.preventDefault();

    const id = document.getElementById('feature-id').value;
    const isEdit = id !== '';

    const data = {
        key: document.getElementById('feature-key').value,
        name: document.getElementById('feature-name').value,
        description: document.getElementById('feature-description').value || null,
        enabled: document.getElementById('feature-enabled').checked,
        createdBy: document.getElementById('feature-created-by').value || 'web-ui'
    };

    try {
        const url = isEdit ? `${API_BASE}/features/${id}` : `${API_BASE}/features`;
        const method = isEdit ? 'PUT' : 'POST';

        // For PUT, adjust the payload
        const payload = isEdit ? {
            name: data.name,
            description: data.description,
            enabled: data.enabled,
            updatedBy: data.createdBy
        } : data;

        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const result = await response.json();

        if (result.success) {
            showToast(isEdit ? 'Feature updated successfully' : 'Feature created successfully', 'success');
            closeFeatureModal();
            await refreshDashboard();
        } else {
            showToast(result.error?.message || 'Failed to save feature', 'error');
        }
    } catch (error) {
        console.error('Error saving feature:', error);
        showToast('Failed to save feature', 'error');
    }
}

// Toggle feature
async function toggleFeature(id) {
    try {
        const response = await fetch(`${API_BASE}/features/${id}/toggle?changedBy=web-ui`, {
            method: 'POST'
        });

        const result = await response.json();

        if (result.success) {
            showToast('Feature toggled successfully', 'success');
            await refreshDashboard();
        } else {
            showToast(result.error?.message || 'Failed to toggle feature', 'error');
        }
    } catch (error) {
        console.error('Error toggling feature:', error);
        showToast('Failed to toggle feature', 'error');
    }
}

// Delete feature
async function deleteFeature(id) {
    const feature = currentFeatures.find(f => f.id === id);
    if (!feature) return;

    if (!confirm(`Are you sure you want to delete the feature "${feature.name}"?`)) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/features/${id}?deletedBy=web-ui`, {
            method: 'DELETE'
        });

        const result = await response.json();

        if (result.success) {
            showToast('Feature deleted successfully', 'success');
            await refreshDashboard();
        } else {
            showToast(result.error?.message || 'Failed to delete feature', 'error');
        }
    } catch (error) {
        console.error('Error deleting feature:', error);
        showToast('Failed to delete feature', 'error');
    }
}

// Show rules modal
async function showRulesModal(featureId) {
    selectedFeatureId = featureId;
    const feature = currentFeatures.find(f => f.id === featureId);

    if (!feature) return;

    document.getElementById('rules-modal-title').textContent = `Rules for ${feature.name}`;
    document.getElementById('rules-modal').style.display = 'block';

    await loadRules(featureId);
}

// Close rules modal
function closeRulesModal() {
    document.getElementById('rules-modal').style.display = 'none';
    selectedFeatureId = null;
    hideAddRuleForm();
}

// Load rules for a feature
async function loadRules(featureId) {
    try {
        const response = await fetch(`${API_BASE}/features/${featureId}/rules`);
        const result = await response.json();

        if (result.success && result.data) {
            renderRules(result.data);
        }
    } catch (error) {
        console.error('Error loading rules:', error);
        showToast('Failed to load rules', 'error');
    }
}

// Render rules
function renderRules(rules) {
    const container = document.getElementById('rules-list');

    if (rules.length === 0) {
        container.innerHTML = '<p class="text-muted">No rules configured. Add a rule to get started.</p>';
        return;
    }

    container.innerHTML = rules.map(rule => createRuleCard(rule)).join('');
}

// Create rule card HTML
function createRuleCard(rule) {
    const envBadge = `badge-${rule.environment}`;
    const disabledClass = rule.enabled ? '' : 'disabled';

    return `
        <div class="rule-card ${disabledClass}">
            <div class="rule-header">
                <div class="rule-title">${escapeHtml(rule.name)}</div>
                <div>
                    <button class="icon-btn" onclick="toggleRule(${rule.id})" title="Toggle">
                        ${rule.enabled ? '⏸️' : '▶️'}
                    </button>
                    <button class="icon-btn" onclick="deleteRule(${rule.id})" title="Delete">🗑️</button>
                </div>
            </div>
            <div class="rule-info">
                <div class="rule-info-item">
                    <div class="rule-info-label">Environment</div>
                    <div class="rule-info-value">
                        <span class="badge ${envBadge}">${rule.environment}</span>
                    </div>
                </div>
                <div class="rule-info-item">
                    <div class="rule-info-label">Rollout</div>
                    <div class="rule-info-value">${rule.rolloutPercentage}%</div>
                </div>
                <div class="rule-info-item">
                    <div class="rule-info-label">Priority</div>
                    <div class="rule-info-value">${rule.priority}</div>
                </div>
            </div>
        </div>
    `;
}

// Show add rule form
function showAddRuleForm() {
    document.getElementById('add-rule-form').style.display = 'block';
}

// Hide add rule form
function hideAddRuleForm() {
    document.getElementById('add-rule-form').style.display = 'none';
    document.querySelector('#add-rule-form form').reset();
}

// Save rule
async function saveRule(event) {
    event.preventDefault();

    if (!selectedFeatureId) return;

    const data = {
        name: document.getElementById('rule-name').value,
        environment: document.getElementById('rule-environment').value,
        rolloutPercentage: parseInt(document.getElementById('rule-rollout').value),
        priority: parseInt(document.getElementById('rule-priority').value),
        enabled: document.getElementById('rule-enabled').checked
    };

    try {
        const response = await fetch(`${API_BASE}/features/${selectedFeatureId}/rules?createdBy=web-ui`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        const result = await response.json();

        if (result.success) {
            showToast('Rule created successfully', 'success');
            hideAddRuleForm();
            await loadRules(selectedFeatureId);
            await refreshDashboard();
        } else {
            showToast(result.error?.message || 'Failed to create rule', 'error');
        }
    } catch (error) {
        console.error('Error creating rule:', error);
        showToast('Failed to create rule', 'error');
    }
}

// Toggle rule
async function toggleRule(ruleId) {
    try {
        const response = await fetch(`${API_BASE}/features/rules/${ruleId}/toggle?changedBy=web-ui`, {
            method: 'POST'
        });

        const result = await response.json();

        if (result.success) {
            showToast('Rule toggled successfully', 'success');
            await loadRules(selectedFeatureId);
            await refreshDashboard();
        } else {
            showToast(result.error?.message || 'Failed to toggle rule', 'error');
        }
    } catch (error) {
        console.error('Error toggling rule:', error);
        showToast('Failed to toggle rule', 'error');
    }
}

// Delete rule
async function deleteRule(ruleId) {
    if (!confirm('Are you sure you want to delete this rule?')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/features/rules/${ruleId}?deletedBy=web-ui`, {
            method: 'DELETE'
        });

        const result = await response.json();

        if (result.success) {
            showToast('Rule deleted successfully', 'success');
            await loadRules(selectedFeatureId);
            await refreshDashboard();
        } else {
            showToast(result.error?.message || 'Failed to delete rule', 'error');
        }
    } catch (error) {
        console.error('Error deleting rule:', error);
        showToast('Failed to delete rule', 'error');
    }
}

// Load audit logs
async function loadAuditLogs() {
    try {
        const response = await fetch(`${API_BASE}/audit`);
        const result = await response.json();

        if (result.success && result.data) {
            renderAuditLogs(result.data);
        }
    } catch (error) {
        console.error('Error loading audit logs:', error);
        showToast('Failed to load audit logs', 'error');
    }
}

// Render audit logs
function renderAuditLogs(logs) {
    const tbody = document.getElementById('audit-tbody');

    if (logs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-muted" style="text-align: center;">No audit logs found</td></tr>';
        return;
    }

    tbody.innerHTML = logs.map(log => `
        <tr>
            <td>${formatTimestamp(log.timestamp)}</td>
            <td><strong>${escapeHtml(log.featureName)}</strong><br><small>${escapeHtml(log.featureKey)}</small></td>
            <td><span class="action-badge ${log.action.toLowerCase()}">${log.action.replace(/_/g, ' ')}</span></td>
            <td>${escapeHtml(log.changedBy)}</td>
            <td>${escapeHtml(log.reason || '-')}</td>
        </tr>
    `).join('');
}

// Show/hide loading
function showLoading(show) {
    document.getElementById('loading').style.display = show ? 'block' : 'none';
}

// Show toast notification
function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = `toast ${type} show`;

    setTimeout(() => {
        toast.className = toast.className.replace('show', '');
    }, 3000);
}

// Utility functions
function formatTimestamp(timestamp) {
    const date = new Date(timestamp);
    return date.toLocaleString('en-US', {
        year: 'numeric',
        month: 'short',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Close modals when clicking outside
window.onclick = function(event) {
    const featureModal = document.getElementById('feature-modal');
    const rulesModal = document.getElementById('rules-modal');

    if (event.target == featureModal) {
        closeFeatureModal();
    }
    if (event.target == rulesModal) {
        closeRulesModal();
    }
}
