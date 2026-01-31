function goBack() {
    window.location.href = '/office-page';
}

function getGroupNameFromUrl() {
    const pathParts = window.location.pathname.split('/');
    return pathParts[pathParts.length - 1];
}

function checkIfLeader(groupLeaderName) {
    const currentUser = localStorage.getItem('userName');
    return currentUser === groupLeaderName;
}

function toggleCompetitionsDropdown() {
    const dropdown = document.getElementById('competitions-dropdown');
    const button = document.querySelector('.dropdown-button');

    if (dropdown.classList.contains('show')) {
        dropdown.classList.remove('show');
    } else {
        const buttonRect = button.getBoundingClientRect();
        dropdown.style.top = `${buttonRect.bottom + 5}px`;
        dropdown.style.left = `${buttonRect.left}px`;
        dropdown.style.width = `${buttonRect.width}px`;

        dropdown.classList.add('show');
    }
}

function filterCompetitions() {
    const searchInput = document.getElementById('competition-search');
    const filter = searchInput.value.toLowerCase();
    const items = document.querySelectorAll('.competition-checkbox-item');

    items.forEach(item => {
        const text = item.textContent.toLowerCase();
        if (text.includes(filter)) {
            item.style.display = 'flex';
        } else {
            item.style.display = 'none';
        }
    });
}

function updateSelectedCount() {
    const checkboxes = document.querySelectorAll('.competition-checkbox-item input[type="checkbox"]:checked');
    const count = checkboxes.length;
    document.getElementById('selected-count').textContent = `${count} selected`;

    const selectedItems = document.getElementById('selected-items');
    selectedItems.innerHTML = '';

    if (count === 0) {
        selectedItems.innerHTML = '<span class="no-selection">No competitions selected</span>';
    } else {
        checkboxes.forEach(checkbox => {
            const tag = document.createElement('div');
            tag.className = 'selected-item-tag';
            tag.innerHTML = `
                <span>${checkbox.value}</span>
                <span class="remove-tag" onclick="removeCompetition('${checkbox.value}')">×</span>
            `;
            selectedItems.appendChild(tag);
        });
    }
}

function removeCompetition(value) {
    const checkbox = document.querySelector(`.competition-checkbox-item input[value="${value}"]`);
    if (checkbox) {
        checkbox.checked = false;
        updateSelectedCount();
    }
}

function displayGroupCompetitions(competitions) {
    const competitionsList = document.getElementById('competitions-list');
    competitionsList.innerHTML = '';

    if (!competitions || competitions.length === 0) {
        competitionsList.innerHTML = '<p class="placeholder-text">No competitions yet</p>';
        return;
    }

    competitions.forEach(comp => {
        const item = document.createElement('div');
        item.className = 'competition-item';
        item.textContent = comp;
        competitionsList.appendChild(item);
    });
}

function displayCompetitionPeriod(startDate, endDate) {
    const periodDiv = document.getElementById('competition-period');
    const periodText = document.getElementById('period-text');

    if (!startDate || !endDate) {
        periodDiv.style.display = 'none';
        return;
    }

    const formatDate = (dateStr) => {
        const date = new Date(dateStr);
        const options = { day: 'numeric', month: 'long', year: 'numeric' };
        return date.toLocaleDateString('en-US', options);
    };

    periodText.textContent = `Competition period: from ${formatDate(startDate)} to ${formatDate(endDate)}`;
    periodDiv.style.display = 'block';
}

async function createGroupCompetitions() {
    const startDate = document.getElementById('start-date').value;
    const endDate = document.getElementById('end-date').value;
    const checkboxes = document.querySelectorAll('.competition-checkbox-item input[type="checkbox"]:checked');

    if (!startDate || !endDate) {
        alert('Please select both start and end dates');
        return;
    }

    if (checkboxes.length === 0) {
        alert('Please select at least one competition');
        return;
    }

    const selectedCompetitions = Array.from(checkboxes).map(cb => cb.value);
    const groupName = getGroupNameFromUrl();
    const leaderName = localStorage.getItem('userName');

    const data = {
        groupName: groupName,
        leaderName: leaderName,
        startDate: startDate,
        endDate: endDate,
        competitions: selectedCompetitions
    };

    console.log('Creating group competitions:', data);

    try {
        const response = await fetch('/api/v0/user-group/set-competitions', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || 'Failed to create group competitions');
        }

        const result = await response.text();
        console.log('Success:', result);

        const formattedCompetitions = selectedCompetitions.map(comp => {
            const [country, name] = comp.split(':');
            return `${country}: ${name}`;
        });

        displayGroupCompetitions(formattedCompetitions);
        displayCompetitionPeriod(startDate, endDate);
        toggleCompetitionsDropdown();

        alert('Group competitions created successfully!');

        document.getElementById('start-date').value = '';
        document.getElementById('end-date').value = '';
        checkboxes.forEach(cb => cb.checked = false);
        updateSelectedCount();

    } catch (error) {
        console.error('Error creating group competitions:', error);
        alert('Error: ' + error.message);
    }
}

async function loadCompetitionsForGroup() {
    const dropdownList = document.getElementById('competitions-list-dropdown');

    try {
        const response = await fetch('/api/v0/competitions/list');
        if (!response.ok) {
            throw new Error('Failed to fetch competitions');
        }

        const competitions = await response.json();
        console.log('Competitions loaded:', competitions);

        dropdownList.innerHTML = '';

        let competitionsData = competitions[0];
        if (typeof competitionsData === 'string') {
            competitionsData = JSON.parse(competitionsData);
        }

        competitionsData.forEach(compObj => {
            const label = document.createElement('label');
            label.className = 'competition-checkbox-item';

            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';

            let country, name, value;
            if (compObj.country && compObj.name) {
                country = compObj.country;
                name = compObj.name;
                value = `${country}:${name}`;
            } else if (typeof compObj === 'object') {
                country = Object.keys(compObj)[0];
                name = compObj[country];
                value = `${country}:${name}`;
            }

            checkbox.value = value;

            const span = document.createElement('span');
            span.textContent = `${country}: ${name}`;

            label.appendChild(checkbox);
            label.appendChild(span);
            dropdownList.appendChild(label);
        });

    } catch (error) {
        console.error('Error loading competitions:', error);
        dropdownList.innerHTML = '<p class="error-text">Failed to load competitions</p>';
    }
}

async function loadGroupCompetitionsData(groupName) {
    try {
        const response = await fetch(`/api/v0/user-group/get-competitions/${groupName}`);

        if (!response.ok) {
            console.log('No competitions data yet');
            return;
        }

        const data = await response.json();
        console.log('Group competitions data:', data);

        if (data.competitions && data.competitions.length > 0) {
            const formattedCompetitions = data.competitions.map(comp => {
                const [country, name] = comp.split(':');
                return `${country}: ${name}`;
            });
            displayGroupCompetitions(formattedCompetitions);
        }

        if (data.startDate && data.endDate) {
            displayCompetitionPeriod(data.startDate, data.endDate);
        }

    } catch (error) {
        console.error('Error loading group competitions:', error);
    }
}

async function loadJoinRequests(groupName) {
    try {
        const response = await fetch(`/api/v0/user-group/join-requests/${groupName}`);

        if (!response.ok) {
            console.log('No join requests or error loading');
            return;
        }

        const requests = await response.json();
        console.log('Join requests loaded:', requests);

        const joinRequestsPanel = document.getElementById('join-requests-panel');
        const joinRequestsList = document.getElementById('join-requests-list');

        if (requests.length === 0) {
            joinRequestsPanel.style.display = 'none';
            return;
        }

        joinRequestsPanel.style.display = 'block';
        joinRequestsList.innerHTML = '';

        requests.forEach(request => {
            const item = document.createElement('div');
            item.className = 'join-request-item';
            item.id = `request-${request.userName}`;

            item.innerHTML = `
                <div class="join-request-username">${request.userName}</div>
                <div class="join-request-buttons">
                    <button class="accept-button" onclick="acceptRequest('${request.userName}')">Accept</button>
                    <button class="reject-button" onclick="rejectRequest('${request.userName}')">Reject</button>
                </div>
            `;

            joinRequestsList.appendChild(item);
        });

    } catch (error) {
        console.error('Error loading join requests:', error);
    }
}

async function acceptRequest(userName) {
    const groupName = getGroupNameFromUrl();
    const leaderName = localStorage.getItem('userName');

    try {
        const response = await fetch('/api/v0/user-group/process-join-request', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                userName: userName,
                groupName: groupName,
                leaderName: leaderName,
                action: 'ACCEPT'
            })
        });

        if (response.ok) {
            console.log('User accepted:', userName);

            const requestItem = document.getElementById(`request-${userName}`);
            if (requestItem) {
                requestItem.remove();
            }

            const joinRequestsList = document.getElementById('join-requests-list');
            if (joinRequestsList.children.length === 0) {
                document.getElementById('join-requests-panel').style.display = 'none';
            }

            addUserToMembersTable(userName);

            alert(`User ${userName} has been added to the group!`);

        } else {
            const error = await response.text();
            alert('Error: ' + error);
        }
    } catch (error) {
        console.error('Error accepting request:', error);
        alert('Failed to accept request. Please try again.');
    }
}

async function rejectRequest(userName) {
    const groupName = getGroupNameFromUrl();
    const leaderName = localStorage.getItem('userName');

    try {
        const response = await fetch('/api/v0/user-group/process-join-request', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                userName: userName,
                groupName: groupName,
                leaderName: leaderName,
                action: 'REJECT'
            })
        });

        if (response.ok) {
            console.log('User rejected:', userName);

            const requestItem = document.getElementById(`request-${userName}`);
            if (requestItem) {
                requestItem.remove();
            }

            const joinRequestsList = document.getElementById('join-requests-list');
            if (joinRequestsList.children.length === 0) {
                document.getElementById('join-requests-panel').style.display = 'none';
            }

            alert(`User ${userName} has been rejected.`);

        } else {
            const error = await response.text();
            alert('Error: ' + error);
        }
    } catch (error) {
        console.error('Error rejecting request:', error);
        alert('Failed to reject request. Please try again.');
    }
}

function addUserToMembersTable(userName) {
    const membersTable = document.querySelector('#members-statistics table tbody');

    if (!membersTable) {
        console.error('Members table not found');
        return;
    }

    const placeholder = membersTable.querySelector('.placeholder-text');
    if (placeholder) {
        placeholder.parentElement.remove();
    }

    const row = document.createElement('tr');
    row.innerHTML = `
        <td>-</td>
        <td>${userName}</td>
        <td>0</td>
        <td>0</td>
        <td>0%</td>
    `;
    membersTable.appendChild(row);
}

async function loadGroupMembers(groupName) {
    try {
        const response = await fetch(`/api/v0/user-group/members/${groupName}`);

        if (!response.ok) {
            console.log('Failed to load group members');
            return;
        }

        const members = await response.json();
        console.log('Group members loaded:', members);

        const membersTable = document.querySelector('#members-statistics table tbody');

        if (!members || members.length === 0) {
            membersTable.innerHTML = '<tr><td colspan="5" class="placeholder-text">No members data yet</td></tr>';
            return;
        }

        membersTable.innerHTML = '';

        members.forEach(userName => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>-</td>
                <td>${userName}</td>
                <td>0</td>
                <td>0</td>
                <td>0%</td>
            `;
            membersTable.appendChild(row);
        });

    } catch (error) {
        console.error('Error loading group members:', error);
    }
}

document.addEventListener('click', function(event) {
    const dropdown = document.getElementById('competitions-dropdown');
    const button = document.querySelector('.dropdown-button');

    if (dropdown && button &&
    !dropdown.contains(event.target) &&
    !button.contains(event.target)) {
        dropdown.classList.remove('show');
    }
});


document.addEventListener('change', function(event) {
    if (event.target.matches('.competition-checkbox-item input[type="checkbox"]')) {
        updateSelectedCount();
    }
});

document.addEventListener('DOMContentLoaded', () => {
    const groupName = getGroupNameFromUrl();
    console.log('Current group:', groupName);

    loadGroupCompetitionsData(groupName);

    loadGroupMembers(groupName);

    const groupLeaderElement = document.querySelector('.group-info-header strong:last-child');
    if (groupLeaderElement) {
        const groupLeaderName = groupLeaderElement.textContent;

        if (checkIfLeader(groupLeaderName)) {
            document.getElementById('leader-panel').style.display = 'block';
            console.log('Leader panel enabled');

            loadCompetitionsForGroup();
            loadJoinRequests(groupName);
        } else {
            console.log('User is not the leader');
        }
    }
});