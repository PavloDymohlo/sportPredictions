let selectedTournamentId = null;

function showToast(message, type = 'info', duration = 3500) {
  const icons = { success: '✓', error: '✕', warning: '⚠', info: 'ℹ' };
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `
    <span class="toast-icon">${icons[type] || 'ℹ'}</span>
    <span class="toast-text">${message}</span>
    <button class="toast-close" onclick="this.parentElement.remove()">×</button>
  `;
  container.appendChild(toast);
  setTimeout(() => {
    toast.classList.add('toast-hide');
    setTimeout(() => toast.remove(), 300);
  }, duration);
}

function showConfirm(title, message) {
  return new Promise((resolve) => {
    const modal = document.getElementById('confirm-modal');
    document.getElementById('confirm-title').textContent = title;
    document.getElementById('confirm-message').textContent = message;
    modal.style.display = 'flex';

    const okBtn = document.getElementById('confirm-ok-btn');
    const cancelBtn = document.getElementById('confirm-cancel-btn');

    const close = (result) => {
      modal.style.display = 'none';
      okBtn.replaceWith(okBtn.cloneNode(true));
      cancelBtn.replaceWith(cancelBtn.cloneNode(true));
      resolve(result);
    };

    document.getElementById('confirm-ok-btn').addEventListener('click', () => close(true));
    document.getElementById('confirm-cancel-btn').addEventListener('click', () => close(false));
    modal.addEventListener('click', (e) => { if (e.target === modal) close(false); }, { once: true });
  });
}

function updateSlotsCounter(tournaments) {
  const counter = document.getElementById('tournament-slots-count');
  if (!counter) return;
  const used = (tournaments || []).filter(t => t.status === 'ACTIVE' || t.status === 'NOT_STARTED').length;
  const free = 3 - used;
  counter.textContent = `${free} / 3 ${t('msg.free_label')}`;
  counter.className = 'slots-count' + (free === 0 ? ' slots-full' : '');
}

function goBack() {
  window.location.href = '/office-page';
}

function getGroupNameFromUrl() {
  const pathParts = window.location.pathname.split('/');
  return decodeURIComponent(pathParts[pathParts.length - 1]);
}

function checkIfLeader(groupLeaderName) {
  const currentUser = localStorage.getItem('userName');
  return currentUser?.trim() === groupLeaderName?.trim();
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
  document.getElementById('selected-count').textContent = `${count} ${t('msg.selected_suffix')}`;

  const selectedItems = document.getElementById('selected-items');
  if (!selectedItems) return;
  selectedItems.innerHTML = '';

  if (count === 0) {
    selectedItems.innerHTML = `<span class="no-selection">${t('msg.no_competitions_selected')}</span>`;
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
    competitionsList.innerHTML = `<p class="placeholder-text">${t('msg.no_competitions_text')}</p>`;
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
    showToast(t('msg.please_dates'), 'warning');
    return;
  }

  if (checkboxes.length === 0) {
    showToast(t('msg.please_competition'), 'warning');
    return;
  }

  const selectedCompetitions = Array.from(checkboxes).map(cb => cb.value);
  const groupName = getGroupNameFromUrl();

  const data = {
    groupName: groupName,
    startDate: startDate,
    endDate: endDate,
    competitions: selectedCompetitions
  };

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

    toggleCompetitionsDropdown();
    selectedTournamentId = null;
    SportCache.invalidate(`group-tournaments_${groupName}`);
    loadGroupTournaments(groupName);
    loadGroupMembers(groupName);

    showToast(t('msg.tournament_created'), 'success');

    document.getElementById('start-date').value = '';
    document.getElementById('end-date').value = '';
    checkboxes.forEach(cb => cb.checked = false);
    updateSelectedCount();

  } catch (error) {
    console.error('Error creating group competitions:', error);
    showToast(error.message || t('msg.fail_create_tournament'), 'error');
  }
}

async function loadCompetitionsForGroup() {
  const dropdownList = document.getElementById('competitions-list-dropdown');

  try {
    const response = await fetch('/api/v0/competitions/list');
    if (!response.ok) {
      throw new Error('Failed to fetch competitions');
    }

    const competitionsData = await response.json();
    console.log('Competitions loaded:', competitionsData);

    dropdownList.innerHTML = '';

    competitionsData.forEach(compObj => {
      const label = document.createElement('label');
      label.className = 'competition-checkbox-item';

      const checkbox = document.createElement('input');
      checkbox.type = 'checkbox';

      let country, name, code, value;
      if (compObj.country && compObj.name) {
        country = compObj.country;
        name = compObj.name;
        code = compObj.code || '';
        value = `${country}:${name}:${code}`;
      } else if (typeof compObj === 'object') {
        country = Object.keys(compObj)[0];
        name = compObj[country];
        code = '';
        value = `${country}:${name}:${code}`;
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
    dropdownList.innerHTML = `<p class="error-text">${t('msg.fail_load_competitions')}</p>`;
  }
}

async function loadGroupTournaments(groupName) {
  try {
    const cacheKey = `group-tournaments_${groupName}`;
    let tournaments = await SportCache.get(cacheKey);
    if (!tournaments) {
      const response = await fetch(`/api/v0/user-group/tournaments/${encodeURIComponent(groupName)}`);

      if (!response.ok) {
        console.log('Failed to load group tournaments');
        return;
      }

      tournaments = await response.json();
      await SportCache.set(cacheKey, tournaments);
    }
    console.log('Group tournaments loaded:', tournaments);

    const competitionsList = document.getElementById('competitions-list');
    competitionsList.innerHTML = '';

    updateSlotsCounter(tournaments);

    if (!tournaments || tournaments.length === 0) {
      competitionsList.innerHTML = `<p class="placeholder-text">${t('msg.no_tournaments')}</p>`;
      return;
    }

    if (!selectedTournamentId) {
      const active = tournaments.find(t => t.status === 'ACTIVE');
      if (active) {
        selectedTournamentId = active.id;
      } else {
        const finished = tournaments.filter(t => t.status === 'FINISHED');
        if (finished.length > 0) {
          selectedTournamentId = finished[finished.length - 1].id;
        }
      }
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return '?';
      const date = new Date(dateStr);
      return date.toLocaleDateString(t('locale'), { month: 'short', day: 'numeric', year: 'numeric' });
    };

    const currentUser = localStorage.getItem('userName');
    const groupLeaderElement = document.querySelector('.group-info-header strong:last-child');
    const isLeader = groupLeaderElement && currentUser === groupLeaderElement.textContent;
    const today = new Date().toISOString().split('T')[0];

    tournaments.forEach(tournament => {
      const card = document.createElement('div');
      const isSelected = selectedTournamentId === tournament.id;
      card.className = 'tournament-card' + (isSelected ? ' tournament-card-selected' : '');
      card.onclick = (e) => {
        if (e.target.closest('.edit-dates-form') || e.target.closest('.edit-dates-toggle')) return;
        selectTournament(tournament.id, groupName);
      };

      const statusClass = tournament.status === 'ACTIVE' ? 'status-active'
        : tournament.status === 'FINISHED' ? 'status-finished' : 'status-not-started';

      const competitionText = (tournament.competitions || [])
        .map(c => c.replace(':', ': '))
        .join(', ');

      const canEditEnd = tournament.endDate >= today;
      const canEditStart = tournament.status === 'NOT_STARTED';
      const showEditButton = isLeader && tournament.status !== 'FINISHED';

      let editSection = '';
      if (showEditButton) {
        editSection = `
          <div class="edit-dates-toggle">
            <button class="edit-dates-btn" onclick="toggleEditDates(event, ${tournament.id})">${t('msg.edit_dates_btn')}</button>
          </div>
          <div id="edit-dates-${tournament.id}" class="edit-dates-form" style="display:none;" onclick="event.stopPropagation()">
            <div class="edit-dates-row">
              <div class="edit-date-field">
                <label>${t('msg.start_label')}</label>
                <input type="date" id="edit-start-${tournament.id}"
                       value="${tournament.startDate || ''}"
                       min="${today}"
                       ${!canEditStart ? 'disabled title="Start date cannot be changed — tournament already started"' : ''}>
              </div>
              <div class="edit-date-field">
                <label>${t('msg.end_label')}</label>
                <input type="date" id="edit-end-${tournament.id}"
                       value="${tournament.endDate || ''}"
                       ${!canEditEnd ? 'disabled title="End date cannot be changed — last day already calculated"' : ''}>
              </div>
            </div>
            <div class="edit-dates-actions">
              <button class="edit-cancel-btn" onclick="toggleEditDates(event, ${tournament.id})">${t('msg.cancel_edit')}</button>
              <button class="edit-save-btn" onclick="saveTournamentDates(event, ${tournament.id})">${t('msg.save_edit')}</button>
            </div>
          </div>
        `;
      }

      const deleteBtn = isLeader
        ? `<button class="delete-tournament-btn" title="Delete tournament"
               onclick="confirmDeleteTournament(event, ${tournament.id}, '${groupName}')">
               <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24"
                    fill="none" stroke="currentColor" stroke-width="2.2"
                    stroke-linecap="round" stroke-linejoin="round">
                 <polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/>
                 <path d="M10 11v6"/><path d="M14 11v6"/>
                 <path d="M9 6V4h6v2"/>
               </svg>
           </button>`
        : '';

      card.innerHTML = `
        <div class="tournament-header-row">
          <span class="status-badge ${statusClass}">${t('status.' + tournament.status) || tournament.status}</span>
          <span class="tournament-dates">${formatDate(tournament.startDate)} — ${formatDate(tournament.endDate)}</span>
          ${deleteBtn}
        </div>
        <div class="tournament-competitions-text">${competitionText || t('msg.no_competitions')}</div>
        ${tournament.winner ? `<div class="tournament-winner-text">&#127942; ${t('msg.winner_label')}${tournament.winner} &#127942;</div>` : ''}
        ${editSection}
      `;

      competitionsList.appendChild(card);
    });

  } catch (error) {
    console.error('Error loading group tournaments:', error);
  }
}

function selectTournament(tournamentId, groupName) {
  selectedTournamentId = tournamentId;
  switchView('statistics');
  loadGroupTournaments(groupName);
  loadGroupMembers(groupName);
}

function toggleEditDates(event, tournamentId) {
  event.stopPropagation();
  const form = document.getElementById(`edit-dates-${tournamentId}`);
  form.style.display = form.style.display === 'none' ? 'block' : 'none';
}

async function saveTournamentDates(event, tournamentId) {
  event.stopPropagation();
  const startInput = document.getElementById(`edit-start-${tournamentId}`);
  const endInput = document.getElementById(`edit-end-${tournamentId}`);
  const groupName = getGroupNameFromUrl();

  const data = {
    tournamentId: tournamentId,
    startDate: (!startInput.disabled && startInput.value) ? startInput.value : null,
    endDate: (!endInput.disabled && endInput.value) ? endInput.value : null
  };

  try {
    const response = await fetch('/api/v0/user-group/tournament/update-dates', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || 'Failed to update dates');
    }

    SportCache.invalidate(`group-tournaments_${groupName}`);
    loadGroupTournaments(groupName);
  } catch (error) {
    showToast(error.message || t('msg.fail_update_dates'), 'error');
  }
}

async function confirmDeleteTournament(event, tournamentId, groupName) {
  event.stopPropagation();
  const confirmed = await showConfirm(
    t('msg.delete_tournament_title'),
    t('msg.delete_tournament_msg')
  );
  if (!confirmed) return;

  try {
    const response = await fetch(`/api/v0/user-group/tournament/${tournamentId}`, {
      method: 'DELETE'
    });
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || 'Failed to delete tournament');
    }
    if (selectedTournamentId === tournamentId) selectedTournamentId = null;
    SportCache.invalidate(`group-tournaments_${groupName}`);
    loadGroupTournaments(groupName);
    loadGroupMembers(groupName);
    showToast(t('msg.tournament_deleted'), 'success');
  } catch (error) {
    showToast(error.message || t('msg.fail_delete_tournament'), 'error');
  }
}

async function confirmDeleteGroup() {
  const groupName = getGroupNameFromUrl();
  const confirmed = await showConfirm(
    t('msg.delete_group_title'),
    t('msg.delete_group_msg_prefix') + groupName + t('msg.delete_group_msg_suffix')
  );
  if (!confirmed) return;

  try {
    const response = await fetch(`/api/v0/user-group/${encodeURIComponent(groupName)}`, {
      method: 'DELETE'
    });
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || 'Failed to delete group');
    }
    showToast(t('msg.group_deleted'), 'success', 1500);
    setTimeout(() => { window.location.href = '/office-page'; }, 1500);
  } catch (error) {
    showToast(error.message || t('msg.fail_delete_group'), 'error');
  }
}

async function loadGroupCompetitionsData(groupName) {
  return loadGroupTournaments(groupName);
}

async function loadJoinRequests(groupName) {
  try {
    const response = await fetch(`/api/v0/user-group/join-requests/${encodeURIComponent(groupName)}`);

    if (!response.ok) {
      console.log('Failed to load join requests');
      return;
    }

    const requests = await response.json();
    console.log('Join requests loaded:', requests);

    const requestsPanel = document.getElementById('join-requests-panel');
    const requestsList = document.getElementById('join-requests-list');

    if (!requests || requests.length === 0) {
      requestsPanel.style.display = 'none';
      return;
    }

    requestsPanel.style.display = 'block';
    requestsList.innerHTML = '';

    requests.forEach(request => {
      const item = document.createElement('div');
      item.className = 'join-request-item';
      item.innerHTML = `
        <span class="join-request-username">${request.userName}</span>
        <div class="join-request-buttons">
          <button class="accept-button" onclick="approveRequest('${request.userName}')">${t('msg.accept_btn')}</button>
          <button class="reject-button" onclick="rejectRequest('${request.userName}')">${t('msg.reject_btn')}</button>
        </div>
      `;
      requestsList.appendChild(item);
    });

  } catch (error) {
    console.error('Error loading join requests:', error);
  }
}

async function approveRequest(userName) {
  const groupName = getGroupNameFromUrl();

  const data = {
    groupName: groupName,
    userName: userName,
    action: 'ACCEPT'
  };

  try {
    const response = await fetch('/api/v0/user-group/process-join-request', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(data)
    });

    if (!response.ok) {
      throw new Error('Failed to approve request');
    }

    console.log('Request approved for:', userName);
    loadJoinRequests(groupName);
    loadGroupMembers(groupName);

  } catch (error) {
    console.error('Error approving request:', error);
    showToast(t('msg.fail_approve'), 'error');
  }
}

async function rejectRequest(userName) {
  const groupName = getGroupNameFromUrl();

  const data = {
    groupName: groupName,
    userName: userName,
    action: 'REJECT'
  };

  try {
    const response = await fetch('/api/v0/user-group/process-join-request', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(data)
    });

    if (!response.ok) {
      throw new Error('Failed to reject request');
    }

    console.log('Request rejected for:', userName);
    loadJoinRequests(groupName);

  } catch (error) {
    console.error('Error rejecting request:', error);
    showToast(t('msg.fail_reject'), 'error');
  }
}

function addUserToMembersTable(userName) {
  const groupName = getGroupNameFromUrl();
  loadGroupMembers(groupName);
}

async function loadGroupMembers(groupName) {
  try {
    const cacheKey = `group-ranking_${groupName}_${selectedTournamentId || 'all'}`;
    let ranking = await SportCache.get(cacheKey);
    if (!ranking) {
      let url = `/api/v0/user-group/ranking/${encodeURIComponent(groupName)}`;
      if (selectedTournamentId) {
        url += `?tournamentId=${selectedTournamentId}`;
      }
      const response = await fetch(url);

      if (!response.ok) {
        console.log('Failed to load group ranking');
        return;
      }

      ranking = await response.json();
      await SportCache.set(cacheKey, ranking);
    }
    console.log('Group ranking loaded:', ranking);

    const membersTable = document.querySelector('#statistics-view table tbody');

    if (!ranking || ranking.length === 0) {
      membersTable.innerHTML = `<tr><td colspan="5" class="placeholder-text">${t('msg.no_members')}</td></tr>`;
      return;
    }

    membersTable.innerHTML = '';

    ranking.forEach(member => {
      const row = document.createElement('tr');
      row.innerHTML = `
        <td>${member.rankingPosition}</td>
        <td>${member.userName}</td>
        <td>${member.correctPredictions}</td>
        <td>${member.predictionCount}</td>
        <td>${member.accuracyPercent}%</td>
      `;
      membersTable.appendChild(row);
    });

  } catch (error) {
    console.error('Error loading group ranking:', error);
  }
}

function switchView(viewType) {
  const statisticsView = document.getElementById('statistics-view');
  const matchesView = document.getElementById('matches-view');

  const statisticsButton = document.querySelector('.tab-button[onclick="switchView(\'statistics\')"]');
  const matchesButton = document.querySelector('.tab-button[onclick="switchView(\'matches\')"]');

  if (viewType === 'statistics') {
    statisticsView.style.display = 'block';
    matchesView.style.display = 'none';

    statisticsButton.classList.add('active');
    matchesButton.classList.remove('active');

  } else if (viewType === 'matches') {
    statisticsView.style.display = 'none';
    matchesView.style.display = 'block';

    statisticsButton.classList.remove('active');
    matchesButton.classList.add('active');

    const dateInput = document.getElementById('matches-date');
    if (!dateInput.value) {
      const yesterday = new Date();
      yesterday.setDate(yesterday.getDate() - 1);
      dateInput.value = yesterday.toISOString().split('T')[0];
    }
    loadMatchesView();
  }
}

async function loadMatchesView() {
  const dateInput = document.getElementById('matches-date');
  const date = dateInput.value;

  if (!date) {
    document.getElementById('matches-content').innerHTML =
      `<p class="placeholder-text">${t('msg.please_select_date')}</p>`;
    return;
  }

  const groupName = getGroupNameFromUrl();

  try {
    document.getElementById('spinner').style.display = 'block';

    const cacheKey = `group-matches_${groupName}_${date}_${selectedTournamentId || 'all'}`;
    const today = new Date().toISOString().split('T')[0];
    const isPastDate = date < today;
    let data = isPastDate ? await SportCache.get(cacheKey) : null;
    if (!data) {
      let url = `/api/v0/user-group/matches-with-predictions/${encodeURIComponent(groupName)}?date=${date}`;
      if (selectedTournamentId) {
        url += `&tournamentId=${selectedTournamentId}`;
      }
      const response = await fetch(url);

      if (!response.ok) {
        throw new Error('Failed to load matches');
      }

      data = await response.json();
      if (isPastDate) {
        await SportCache.set(cacheKey, data);
      }
    }
    displayMatchesView(data);

  } catch (error) {
    console.error('Error loading matches:', error);
    document.getElementById('matches-content').innerHTML =
      `<p class="placeholder-text">${t('msg.fail_load_matches')}</p>`;
  } finally {
    document.getElementById('spinner').style.display = 'none';
  }
}

function displayMatchesView(data) {
  const matchesContent = document.getElementById('matches-content');

  if (!data || !data.competitions || data.competitions.length === 0) {
    matchesContent.innerHTML = `<p class="placeholder-text">${t('msg.no_matches_date')}</p>`;
    return;
  }

  let tableHTML = '<table class="matches-table">';

  tableHTML += '<thead><tr>';
  tableHTML += `<th style="min-width: 300px; text-align: left;">${t('msg.match_col')}</th>`;

  data.members.forEach(member => {
    tableHTML += `<th class="vertical-text">${member}</th>`;
  });

  tableHTML += '</tr></thead>';

  tableHTML += '<tbody>';

  data.competitions.forEach(competition => {
    const competitionName = competition.country
      ? `${competition.country}: ${competition.tournament}`
      : competition.tournament;

    tableHTML += `<tr><td colspan="${data.members.length + 1}" class="competition-header">${competitionName}</td></tr>`;

    competition.matches.forEach(match => {
      tableHTML += '<tr>';

      tableHTML += '<td class="match-combined-cell">';
      tableHTML += '<div class="match-info">';
      tableHTML += `<span class="match-teams-text">${match.homeTeam} - ${match.awayTeam}</span>`;
      tableHTML += `<span class="match-score-text">${match.homeScore}:${match.awayScore}</span>`;
      tableHTML += '</div>';
      tableHTML += '</td>';

      data.members.forEach(member => {
        const prediction = match.predictions[member];

        if (!prediction || prediction.homePrediction === '-') {
          tableHTML += '<td class="prediction-none">-</td>';
        } else {
          const predictionText = `${prediction.homePrediction}:${prediction.awayPrediction}`;

          const isCorrect = (prediction.homePrediction === match.homeScore &&
            prediction.awayPrediction === match.awayScore);

          const cssClass = isCorrect ? 'prediction-correct' : 'prediction-wrong';
          tableHTML += `<td class="${cssClass}">${predictionText}</td>`;
        }
      });

      tableHTML += '</tr>';
    });
  });

  tableHTML += '</tbody></table>';

  matchesContent.innerHTML = tableHTML;
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

  loadGroupTournaments(groupName);

  loadGroupMembers(groupName);

  if (typeof GROUP_LEADER_NAME !== 'undefined' && GROUP_LEADER_NAME !== null) {
    if (checkIfLeader(GROUP_LEADER_NAME)) {
      document.getElementById('leader-panel').style.display = 'block';
      document.getElementById('delete-group-btn').style.display = 'block';
      console.log('Leader panel enabled for:', GROUP_LEADER_NAME);

      const today = new Date().toISOString().split('T')[0];
      document.getElementById('start-date').min = today;
      document.getElementById('end-date').min = today;

      loadCompetitionsForGroup();
      loadJoinRequests(groupName);
    } else {
      console.log('User is not the leader. Leader:', GROUP_LEADER_NAME, '| Current:', localStorage.getItem('userName'));
    }
  }
});
