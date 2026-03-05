async function fetchUsers() {
  const spinner = document.getElementById('spinner');
  spinner.style.display = 'block';
  try {
    const cached = await SportCache.get('global-stats');
    if (cached) {
      displayUsers(cached);
      return;
    }
    const response = await fetch('/api/v0/statistic/general');
    if (!response.ok) {
      throw new Error('Failed to fetch users');
    }
    const users = await response.json();
    await SportCache.set('global-stats', users);
    displayUsers(users);
  } catch (error) {
    console.error('Error:', error);
  } finally {
    spinner.style.display = 'none';
  }
}

function displayUsers(users) {
  const usersStatisticDiv = document.getElementById('users-statistic');
  usersStatisticDiv.innerHTML = '';
  const table = document.createElement('table');
  table.className = 'table table-striped';
  table.innerHTML = `
    <thead>
      <tr>
        <th>${t('stats.number')}</th>
        <th>${t('stats.username')}</th>
        <th>${t('stats.correct_predictions')}</th>
        <th>${t('stats.predictions_count')}</th>
        <th>${t('stats.accuracy')}</th>
      </tr>
    </thead>
    <tbody></tbody>
  `;
  const tableBody = table.querySelector('tbody');
  const currentUserName = localStorage.getItem('userName');
  users.sort((a, b) => a.rankingPosition - b.rankingPosition);
  users.forEach(user => {
    const row = document.createElement('tr');
    if (user.userName === currentUserName) {
      row.classList.add('highlighted-user');
    }
    row.innerHTML = `
      <td>${user.rankingPosition}</td>
      <td>${user.userName}</td>
      <td>${user.totalScore}</td>
      <td>${user.predictionCount}</td>
      <td>${user.percentGuessedMatches}%</td>
    `;
    tableBody.appendChild(row);
  });
  usersStatisticDiv.appendChild(table);
}

function returnBack() {
  window.location.href = '/office-page';
}

document.addEventListener('DOMContentLoaded', fetchUsers);
