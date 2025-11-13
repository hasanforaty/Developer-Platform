// API Base URL
const API_BASE = '/api/v1';

// State
let allServices = [];
let isRefreshing = false;

// Initialize dashboard on page load
document.addEventListener('DOMContentLoaded', () => {
    loadDashboard();
    // Auto-refresh every 30 seconds
    setInterval(loadDashboard, 30000);
});

// Load dashboard data
async function loadDashboard() {
    try {
        showLoading(true);
        await Promise.all([
            loadSummary(),
            loadServices()
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
        const response = await fetch(`${API_BASE}/services/summary`);
        const result = await response.json();

        if (result.success && result.data) {
            const data = result.data;
            document.getElementById('total-services').textContent = data.totalServices;
            document.getElementById('up-services').textContent = data.upServices;
            document.getElementById('degraded-services').textContent = data.degradedServices;
            document.getElementById('down-services').textContent = data.downServices;
        }
    } catch (error) {
        console.error('Error loading summary:', error);
    }
}

// Load services
async function loadServices() {
    try {
        const response = await fetch(`${API_BASE}/services`);
        const result = await response.json();

        if (result.success && result.data) {
            allServices = result.data;
            renderServices(allServices);

            // Show empty state if no services
            if (allServices.length === 0) {
                document.getElementById('empty-state').style.display = 'block';
                document.getElementById('services-grid').style.display = 'none';
            } else {
                document.getElementById('empty-state').style.display = 'none';
                document.getElementById('services-grid').style.display = 'grid';
            }
        }
    } catch (error) {
        console.error('Error loading services:', error);
        throw error;
    }
}

// Render services grid
function renderServices(services) {
    const grid = document.getElementById('services-grid');
    grid.innerHTML = '';

    services.forEach(service => {
        const card = createServiceCard(service);
        grid.appendChild(card);
    });
}

// Create service card element
function createServiceCard(service) {
    const card = document.createElement('div');
    card.className = 'service-card';
    card.onclick = () => showServiceDetails(service);

    const statusClass = service.status.toLowerCase();
    const healthyPercentage = service.instanceCount > 0
        ? (service.healthyInstanceCount / service.instanceCount) * 100
        : 0;

    card.innerHTML = `
        <div class="service-header">
            <div>
                <div class="service-name">${service.name}</div>
                ${service.version ? `<div style="font-size: 12px; color: #a0aec0;">v${service.version}</div>` : ''}
            </div>
            <span class="status-badge ${statusClass}">${service.status}</span>
        </div>
        <div class="service-description">
            ${service.description || 'No description provided'}
        </div>
        <div class="instances-info">
            <div style="display: flex; justify-content: space-between; font-size: 13px; color: #4a5568;">
                <span>Instances: ${service.instanceCount}</span>
                <span style="color: #48bb78; font-weight: 600;">${service.healthyInstanceCount} healthy</span>
            </div>
            <div class="instances-bar">
                ${healthyPercentage > 0 ? `<div class="instances-bar-segment healthy" style="width: ${healthyPercentage}%"></div>` : ''}
                ${healthyPercentage < 100 ? `<div class="instances-bar-segment down" style="width: ${100 - healthyPercentage}%"></div>` : ''}
            </div>
        </div>
        <div class="service-meta">
            <div class="meta-item">
                <span>🔗</span>
                <span>${new URL(service.baseUrl).hostname}</span>
            </div>
            <div class="meta-item">
                <span>📅</span>
                <span>${formatDate(service.createdAt)}</span>
            </div>
        </div>
    `;

    return card;
}

// Show service details (future enhancement)
function showServiceDetails(service) {
    console.log('Service details:', service);
    showToast(`Service: ${service.name}`, 'success');
    // TODO: Implement detailed modal
}

// Search services
function searchServices() {
    const searchTerm = document.getElementById('search-input').value.toLowerCase();

    if (!searchTerm) {
        renderServices(allServices);
        return;
    }

    const filtered = allServices.filter(service =>
        service.name.toLowerCase().includes(searchTerm) ||
        (service.description && service.description.toLowerCase().includes(searchTerm))
    );

    renderServices(filtered);
}

// Refresh dashboard
function refreshDashboard() {
    if (isRefreshing) return;

    isRefreshing = true;
    const icon = document.getElementById('refresh-icon');
    icon.style.animation = 'spin 1s linear infinite';

    loadDashboard().finally(() => {
        setTimeout(() => {
            icon.style.animation = '';
            isRefreshing = false;
            showToast('Dashboard refreshed', 'success');
        }, 1000);
    });
}

// Show/hide loading
function showLoading(show) {
    document.getElementById('loading').style.display = show ? 'block' : 'none';
}

// Modal functions
function showRegisterServiceModal() {
    document.getElementById('register-modal').style.display = 'block';
    document.getElementById('register-form').reset();
}

function closeRegisterModal() {
    document.getElementById('register-modal').style.display = 'none';
}

// Close modal when clicking outside
window.onclick = function(event) {
    const modal = document.getElementById('register-modal');
    if (event.target == modal) {
        closeRegisterModal();
    }
}

// Register new service
async function registerService(event) {
    event.preventDefault();

    const form = event.target;
    const formData = new FormData(form);
    const data = {
        name: formData.get('name'),
        description: formData.get('description') || null,
        baseUrl: formData.get('baseUrl'),
        version: formData.get('version') || null
    };

    try {
        const response = await fetch(`${API_BASE}/services`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data)
        });

        const result = await response.json();

        if (result.success) {
            showToast('Service registered successfully', 'success');
            closeRegisterModal();
            loadDashboard();
        } else {
            showToast(result.error?.message || 'Failed to register service', 'error');
        }
    } catch (error) {
        console.error('Error registering service:', error);
        showToast('Failed to register service', 'error');
    }
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

// Format date helper
function formatDate(dateString) {
    const date = new Date(dateString);
    const now = new Date();
    const diff = now - date;
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));

    if (days === 0) {
        return 'Today';
    } else if (days === 1) {
        return 'Yesterday';
    } else if (days < 7) {
        return `${days} days ago`;
    } else {
        return date.toLocaleDateString();
    }
}
