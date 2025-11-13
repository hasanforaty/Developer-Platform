// API Base URL
const API_BASE = '/api/v1/logs';

// State
let currentPage = 0;
let totalPages = 1;
let currentFilters = {
    serviceName: null,
    level: null,
    searchText: null,
    traceId: null
};
let services = [];

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    loadDashboard();
    // Auto-refresh every 30 seconds
    setInterval(() => loadLogs(), 30000);
});

// Load dashboard data
async function loadDashboard() {
    try {
        showLoading(true);
        await Promise.all([
            loadSummary(),
            loadLogs()
        ]);
        showLoading(false);
    } catch (error) {
        console.error('Error loading dashboard:', error);
        showToast('Failed to load dashboard', 'error');
        showLoading(false);
    }
}

// Load summary statistics
async function loadSummary() {
    try {
        const response = await fetch(`${API_BASE}/search/summary`);
        const result = await response.json();

        if (result.success && result.data) {
            const data = result.data;
            document.getElementById('total-logs').textContent = formatNumber(data.totalLogs);
            document.getElementById('error-logs').textContent = formatNumber(data.errorCount);
            document.getElementById('warn-logs').textContent = formatNumber(data.warnCount);
            document.getElementById('info-logs').textContent = formatNumber(data.infoCount);

            // Populate services dropdown
            services = data.services || [];
            populateServicesDropdown();
        }
    } catch (error) {
        console.error('Error loading summary:', error);
    }
}

// Populate services dropdown
function populateServicesDropdown() {
    const select = document.getElementById('service-filter');
    const currentValue = select.value;

    // Clear existing options except "All Services"
    select.innerHTML = '<option value="">All Services</option>';

    services.forEach(service => {
        const option = document.createElement('option');
        option.value = service.serviceName;
        option.textContent = `${service.serviceName} (${formatNumber(service.totalLogs)})`;
        select.appendChild(option);
    });

    // Restore selection
    select.value = currentValue;
}

// Load logs with current filters
async function loadLogs() {
    try {
        const params = new URLSearchParams({
            page: currentPage,
            size: 50
        });

        if (currentFilters.serviceName) params.append('serviceName', currentFilters.serviceName);
        if (currentFilters.level) params.append('level', currentFilters.level);
        if (currentFilters.searchText) params.append('searchText', currentFilters.searchText);
        if (currentFilters.traceId) params.append('traceId', currentFilters.traceId);

        const response = await fetch(`${API_BASE}/search?${params}`);
        const result = await response.json();

        if (result.success && result.data) {
            renderLogs(result.data.logs);
            updatePagination(result.data);

            // Show/hide empty state
            if (result.data.logs.length === 0) {
                document.getElementById('empty-state').style.display = 'block';
                document.getElementById('logs-table').style.display = 'none';
            } else {
                document.getElementById('empty-state').style.display = 'none';
                document.getElementById('logs-table').style.display = 'table';
            }
        }
    } catch (error) {
        console.error('Error loading logs:', error);
        throw error;
    }
}

// Render logs table
function renderLogs(logs) {
    const tbody = document.getElementById('logs-tbody');
    tbody.innerHTML = '';

    logs.forEach(log => {
        const row = createLogRow(log);
        tbody.appendChild(row);
    });
}

// Create log row element
function createLogRow(log) {
    const tr = document.createElement('tr');
    tr.onclick = () => showLogDetails(log);

    const messageClass = log.level === 'ERROR' || log.level === 'FATAL' ? 'error' : '';

    tr.innerHTML = `
        <td class="timestamp">${formatTimestamp(log.timestamp)}</td>
        <td><strong>${log.serviceName}</strong></td>
        <td><span class="log-level ${log.level}">${log.level}</span></td>
        <td class="log-message ${messageClass}">${escapeHtml(log.message)}</td>
        <td class="trace-id" onclick="event.stopPropagation(); filterByTraceId('${log.traceId || ''}')">${log.traceId ? log.traceId.substring(0, 8) + '...' : '-'}</td>
    `;

    return tr;
}

// Show log details modal
function showLogDetails(log) {
    const modal = document.getElementById('log-details-modal');
    const content = document.getElementById('log-details-content');

    content.innerHTML = `
        <div class="detail-row">
            <div class="detail-label">Timestamp:</div>
            <div class="detail-value">${formatTimestamp(log.timestamp)}</div>
        </div>
        <div class="detail-row">
            <div class="detail-label">Service:</div>
            <div class="detail-value"><strong>${log.serviceName}</strong></div>
        </div>
        <div class="detail-row">
            <div class="detail-label">Level:</div>
            <div class="detail-value"><span class="log-level ${log.level}">${log.level}</span></div>
        </div>
        ${log.loggerName ? `
        <div class="detail-row">
            <div class="detail-label">Logger:</div>
            <div class="detail-value">${escapeHtml(log.loggerName)}</div>
        </div>
        ` : ''}
        ${log.threadName ? `
        <div class="detail-row">
            <div class="detail-label">Thread:</div>
            <div class="detail-value">${escapeHtml(log.threadName)}</div>
        </div>
        ` : ''}
        <div class="detail-row">
            <div class="detail-label">Message:</div>
            <div class="detail-value">${escapeHtml(log.message)}</div>
        </div>
        ${log.exceptionClass ? `
        <div class="detail-row">
            <div class="detail-label">Exception:</div>
            <div class="detail-value">${escapeHtml(log.exceptionClass)}</div>
        </div>
        ` : ''}
        ${log.stackTrace ? `
        <div class="detail-row">
            <div class="detail-label">Stack Trace:</div>
            <div class="detail-value"><pre>${escapeHtml(log.stackTrace)}</pre></div>
        </div>
        ` : ''}
        ${log.traceId ? `
        <div class="detail-row">
            <div class="detail-label">Trace ID:</div>
            <div class="detail-value"><code>${log.traceId}</code></div>
        </div>
        ` : ''}
        ${log.spanId ? `
        <div class="detail-row">
            <div class="detail-label">Span ID:</div>
            <div class="detail-value"><code>${log.spanId}</code></div>
        </div>
        ` : ''}
        ${log.metadata ? `
        <div class="detail-row">
            <div class="detail-label">Metadata:</div>
            <div class="detail-value"><pre>${JSON.stringify(log.metadata, null, 2)}</pre></div>
        </div>
        ` : ''}
    `;

    modal.style.display = 'block';
}

// Close log details modal
function closeLogDetailsModal() {
    document.getElementById('log-details-modal').style.display = 'none';
}

// Close modal when clicking outside
window.onclick = function(event) {
    const modal = document.getElementById('log-details-modal');
    if (event.target == modal) {
        closeLogDetailsModal();
    }
}

// Apply filters
function applyFilters() {
    currentFilters.serviceName = document.getElementById('service-filter').value || null;
    currentFilters.level = document.getElementById('level-filter').value || null;
    currentFilters.searchText = document.getElementById('search-text').value || null;
    currentFilters.traceId = document.getElementById('trace-id').value || null;

    currentPage = 0;
    loadLogs();
}

// Handle search keyup
function handleSearchKeyup(event) {
    if (event.key === 'Enter') {
        applyFilters();
    }
}

// Filter by trace ID
function filterByTraceId(traceId) {
    if (traceId) {
        document.getElementById('trace-id').value = traceId;
        applyFilters();
    }
}

// Clear all filters
function clearFilters() {
    document.getElementById('service-filter').value = '';
    document.getElementById('level-filter').value = '';
    document.getElementById('search-text').value = '';
    document.getElementById('trace-id').value = '';

    currentFilters = {
        serviceName: null,
        level: null,
        searchText: null,
        traceId: null
    };

    currentPage = 0;
    loadLogs();
    showToast('Filters cleared', 'success');
}

// Update pagination
function updatePagination(data) {
    totalPages = data.totalPages;
    currentPage = data.page;

    document.getElementById('page-info').textContent =
        `Page ${currentPage + 1} of ${totalPages} (${formatNumber(data.totalCount)} logs)`;

    document.getElementById('prev-btn').disabled = currentPage === 0;
    document.getElementById('next-btn').disabled = currentPage >= totalPages - 1;
}

// Pagination functions
function previousPage() {
    if (currentPage > 0) {
        currentPage--;
        loadLogs();
    }
}

function nextPage() {
    if (currentPage < totalPages - 1) {
        currentPage++;
        loadLogs();
    }
}

// Refresh logs
function refreshLogs() {
    const icon = document.getElementById('refresh-icon');
    icon.style.animation = 'spin 1s linear infinite';

    loadDashboard().finally(() => {
        setTimeout(() => {
            icon.style.animation = '';
            showToast('Logs refreshed', 'success');
        }, 1000);
    });
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
        minute: '2-digit',
        second: '2-digit'
    });
}

function formatNumber(num) {
    if (num >= 1000000) {
        return (num / 1000000).toFixed(1) + 'M';
    } else if (num >= 1000) {
        return (num / 1000).toFixed(1) + 'K';
    }
    return num.toString();
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
