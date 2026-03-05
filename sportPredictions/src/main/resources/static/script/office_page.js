function formatDate(date) {
  return date.toISOString().split('T')[0];
}

async function logout() {
  await fetch('/api/v0/auth/logout', { method: 'POST' });
  localStorage.removeItem('userName');
  window.location.href = '/host-page';
}

function formatDateToEnglish(dateStr) {
  const [day, month] = dateStr.split('/');
  const monthNames = t('months');
  return `${day} ${monthNames[parseInt(month, 10) - 1]}`;
}

function formatDateForDisplay(isoDate) {
  const [year, month, day] = isoDate.split('-');
  const monthNames = t('months');
  return `${day} ${monthNames[parseInt(month, 10) - 1]}`;
}

function showStatistics() {
  toggleMenu();
  window.location.href = '/statistic-page';
}

function showRules() {
  toggleMenu();
  window.location.href = '/rules';
}

async function toggleMyGroups() {
  const groupsContainer = document.getElementById('my-groups-container');
  const groupsList = document.getElementById('my-groups-list');

  if (groupsContainer.style.display === 'none' || groupsContainer.style.display === '') {
    groupsContainer.style.display = 'block';

    initializeGroupSearch();

    if (groupsList.innerHTML === '') {
      await loadUserGroups();
    }
  } else {
    groupsContainer.style.display = 'none';
  }
}

async function loadUserGroups() {
  const groupsList = document.getElementById('my-groups-list');
  const userName = localStorage.getItem('userName');

  if (!userName) {
    groupsList.innerHTML = `<div class="no-groups-message">${t('msg.please_login')}</div>`;
    return;
  }

  groupsList.innerHTML = `<div class="groups-loading">${t('msg.loading_groups')}</div>`;

  try {
    const response = await fetch(`/api/v0/user-group/all-users-group`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    });

    if (!response.ok) {
      throw new Error(`HTTP error! Status: ${response.status}`);
    }

    const groups = await response.json();
    console.log('✅ User groups loaded:', groups);

    groupsList.innerHTML = '';

    if (!groups || groups.length === 0) {
      groupsList.innerHTML = `<div class="no-groups-message">${t('msg.no_groups')}</div>`;
    } else {
      groups.forEach(group => {
        const groupItem = document.createElement('div');
        groupItem.className = 'group-item';
        groupItem.textContent = group.userGroupName;

        groupItem.onclick = () => openGroup(group.userGroupName);
        groupItem.style.cursor = 'pointer';

        groupsList.appendChild(groupItem);
      });
    }
  } catch (error) {
    console.error('❌ Error fetching user groups:', error);
    groupsList.innerHTML = `<div class="error-message">${t('msg.fail_load_groups')}</div>`;
  }
}

function initializeGroupSearch() {
  const searchInput = document.getElementById('group-search-input');
  const searchBtn = document.getElementById('group-search-btn');

  if (searchInput.dataset.initialized === 'true') {
    return;
  }

  searchInput.dataset.initialized = 'true';

  searchInput.addEventListener('input', () => {
    if (searchInput.value.trim().length > 0) {
      searchBtn.style.display = 'block';
    } else {
      searchBtn.style.display = 'none';
      const searchResult = document.getElementById('group-search-result');
      if (searchResult) {
        searchResult.style.display = 'none';
      }
    }
  });

  searchBtn.addEventListener('click', () => {
    searchGroup();
  });

  searchInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter' && searchInput.value.trim().length > 0) {
      searchGroup();
    }
  });
}

async function searchGroup() {
  const searchInput = document.getElementById('group-search-input');
  const searchResult = document.getElementById('group-search-result');
  const groupName = searchInput.value.trim();

  if (!groupName) return;

  searchResult.style.display = 'block';
  searchResult.innerHTML = `<div class="groups-loading">${t('msg.searching')}</div>`;

  try {
    const response = await fetch(`/api/v0/user-group/find-group/${encodeURIComponent(groupName)}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    });

    if (!response.ok) {
      const errorText = await response.text();
      searchResult.innerHTML = `<div class="no-groups-message">${errorText || t('msg.group_not_found')}</div>`;
      return;
    }

    const group = await response.json();
    console.log('✅ Group found:', group);

    searchResult.innerHTML = `
      <div class="found-group">
        <div class="found-group-info">
          <div class="found-group-name">${group.userGroupName}</div>
          <div class="found-group-leader">${t('msg.leader')}${group.userGroupLeaderName}</div>
        </div>
        <button class="apply-group-btn" onclick="applyToGroup('${group.userGroupName}')">${t('msg.apply_btn')}</button>
      </div>
    `;

  } catch (error) {
    console.error('❌ Error searching group:', error);
    searchResult.innerHTML = `<div class="error-message">${t('msg.fail_search')}</div>`;
  }
}

async function applyToGroup(groupName) {
  console.log('Applying to group:', groupName);

  const userName = localStorage.getItem('userName');

  if (!userName) {
    alert(t('msg.user_not_found'));
    return;
  }

  try {
    const response = await fetch('/api/v0/user-group/join-request', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        groupName: groupName
      })
    });

    if (response.ok) {
      const message = await response.text();
      alert(t('msg.apply_success'));
      console.log('Join request sent:', message);
    } else {
      const error = await response.text();
      alert('Error: ' + error);
      console.error('Error sending join request:', error);
    }
  } catch (error) {
    console.error('Error applying to group:', error);
    alert(t('msg.apply_fail'));
  }
}

function toggleCreateGroup() {
  const container = document.getElementById('create-group-container');
  container.style.display = container.style.display === 'none' ? 'block' : 'none';
}

async function createGroup() {
  const input = document.getElementById('create-group-input');
  const result = document.getElementById('create-group-result');

  const groupName = input.value.trim();
  const userName = localStorage.getItem('userName');

  if (!groupName || !userName) return;

  result.textContent = t('msg.creating');
  result.style.color = 'black';

  try {
    const response = await fetch('/api/v0/user-group/create', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userGroupName: groupName
      })
    });

    if (!response.ok) {
      const errorText = await response.text();
      result.textContent = errorText || t('msg.group_exists');
      result.style.color = 'red';
      return;
    }

    const createdGroup = await response.json();

    result.textContent = t('msg.group_created');
    result.style.color = 'green';

    addGroupToList(createdGroup.userGroupName);

    input.value = '';

  } catch (e) {
    result.textContent = t('msg.fail_create_group');
    result.style.color = 'red';
  }
}

function openGroup(groupName) {
  window.location.href = `/group/${encodeURIComponent(groupName)}`;
}

function addGroupToList(groupName) {
  const groupsList = document.getElementById('my-groups-list');

  const groupItem = document.createElement('div');
  groupItem.className = 'group-item';
  groupItem.textContent = groupName;

  groupItem.onclick = () => openGroup(groupName);
  groupItem.style.cursor = 'pointer';

  groupsList.appendChild(groupItem);
}

async function getUserName() {
  const spinner = document.getElementById('spinner');
  const greetingText = document.getElementById('greeting-text');
  spinner.style.display = 'block';
  const userName = await new Promise((resolve) => {
    setTimeout(() => {
      const storedUserName = localStorage.getItem('userName');
      resolve(storedUserName);
    }, 1000);
  });
  if (!userName) {
    console.error('User name not found in localStorage.');
    greetingText.textContent = t('msg.hello') + 'User!';
  } else {
    greetingText.textContent = t('msg.hello') + `${userName}!`;
  }
  spinner.style.display = 'none';
}

async function getMatchResult() {
  const spinner = document.getElementById('spinner');
  const resultsContainer = document.getElementById('resultsContainer');
  spinner.style.display = 'block';
  resultsContainer.innerHTML = '';

  const userName = localStorage.getItem('userName');
  if (!userName) {
    spinner.style.display = 'none';
    return;
  }

  for (let daysBack = 1; daysBack <= 3; daysBack++) {
    const date = new Date();
    date.setDate(date.getDate() - daysBack);
    const formattedDate = formatDate(date);

    const cacheKey = `match-status_${formattedDate}_${userName}`;
    let cached = null;
    try { cached = await SportCache.get(cacheKey); } catch (e) {}
    if (cached) {
      renderPastDaySection(resultsContainer, formattedDate, cached.matches, cached.predictions, daysBack === 1);
      continue;
    }

    try {
      const [matchesResponse, predictionsResponse] = await Promise.all([
        fetch(`/api/v0/predictions/match-status?date=${formattedDate}`),
        fetch(`/api/v0/predictions?date=${formattedDate}`)
      ]);

      let matches = null;
      if (matchesResponse.ok) {
        matches = await matchesResponse.json();
      }

      let predictions = 'no_content';
      if (predictionsResponse.status === 200) {
        const predJson = await predictionsResponse.json();
        predictions = predJson.predictions;
      }

      await SportCache.set(cacheKey, { matches, predictions });
      renderPastDaySection(resultsContainer, formattedDate, matches, predictions, daysBack === 1);
    } catch (error) {
      console.error(`Error fetching data for ${formattedDate}:`, error);
      renderPastDaySection(resultsContainer, formattedDate, null, 'no_content', daysBack === 1);
    }
  }

  spinner.style.display = 'none';
}

function renderPastDaySection(resultsContainer, formattedDate, matches, predictions, expandedByDefault) {
  const dateGroup = document.createElement('div');
  dateGroup.className = 'date-group';

  const dateHeader = document.createElement('h2');
  const label = expandedByDefault ? `${t('msg.results_for')}${formatDateForDisplay(formattedDate)}${t('msg.yesterday')}` : `${t('msg.results_for')}${formatDateForDisplay(formattedDate)}`;
  dateHeader.innerHTML = `
    <span style="display:flex; justify-content:space-between; align-items:center;">
      ${label}
      <span class="toggle-icon">${expandedByDefault ? '▲' : '▼'}</span>
    </span>
  `;
  dateHeader.style.cursor = 'pointer';
  dateGroup.appendChild(dateHeader);

  const contentContainer = document.createElement('div');
  contentContainer.className = 'collapsible-content';
  contentContainer.style.display = expandedByDefault ? 'block' : 'none';

  dateHeader.addEventListener('click', () => {
    const isOpen = contentContainer.style.display !== 'none';
    contentContainer.style.display = isOpen ? 'none' : 'block';
    dateHeader.querySelector('.toggle-icon').textContent = isOpen ? '▼' : '▲';
  });

  if (!matches) {
    contentContainer.innerHTML = `<p style="text-align:center; color:#999; padding:16px;">${t('msg.no_data_for')}${formatDateForDisplay(formattedDate)}</p>`;
  } else {
    buildMatchContent(contentContainer, matches, predictions, formattedDate);
  }

  dateGroup.appendChild(contentContainer);
  resultsContainer.appendChild(dateGroup);
}

function buildMatchContent(container, matches, predictions, formattedDate) {
  const matchData = Array.isArray(matches) && matches.length > 0 && Array.isArray(matches[0])
    ? matches[0]
    : matches;

  if (!matchData || matchData.length === 0) {
    container.innerHTML = `<p style="text-align:center; color:#999; padding:16px;">${t('msg.no_matches_predictions')}${formatDateForDisplay(formattedDate)}</p>`;
    return;
  }

  const matchesContainer = document.createElement('div');
  matchesContainer.className = 'matches-container';

  const predictionMap = new Map();
  const predictionData = Array.isArray(predictions) && predictions.length > 0 && Array.isArray(predictions[0])
    ? predictions[0]
    : predictions;

  if (predictionData && predictionData !== 'no_content' && Array.isArray(predictionData)) {
    predictionData.forEach(item => {
      if (Array.isArray(item) && item.length === 2) {
        const homeTeam = item[0].split(' ').slice(0, -1).join(' ');
        const awayTeam = item[1].split(' ').slice(0, -1).join(' ');
        predictionMap.set(`${homeTeam}_${awayTeam}`, item);
        predictionMap.set(homeTeam, item);
      }
    });
  }

  if (Array.isArray(matchData)) {
    matchData.forEach(item => {
      if (!('tournament' in item && 'matches' in item)) return;
      if (!Array.isArray(item.matches) || item.matches.length === 0) return;

      const competitionDiv = document.createElement('div');
      const competitionHeader = document.createElement('h3');
      competitionHeader.textContent = item.tournament || 'Unknown Tournament';
      competitionDiv.appendChild(competitionHeader);
      matchesContainer.appendChild(competitionDiv);

      item.matches.forEach(matchObj => {
        const matchPair = matchObj.match;
        const isCorrect = matchObj.predictedCorrectly;
        if (!Array.isArray(matchPair) || matchPair.length !== 2) return;

        const matchDiv = document.createElement('div');
        matchDiv.className = 'match' + (isCorrect ? ' correct-prediction' : '');

        const parseTeamAndScore = (matchText) => {
          if (!matchText) return { teamName: '', fullScore: '?' };
          if (matchText.includes('winner')) {
            let processedText = matchText.replace('winner', '').trim();
            const scoreMatch = processedText.match(/(\d+)(?:\s*)$/);
            let teamName = processedText;
            let score = '?';
            if (scoreMatch) { score = scoreMatch[1]; teamName = processedText.replace(scoreMatch[0], '').trim(); }
            return { teamName: `${teamName} (go next)`, fullScore: score };
          }
          if (!matchText.includes('(')) {
            const parts = matchText.trim().split(' ');
            const score = parts.pop() || '?';
            return { teamName: parts.join(' '), fullScore: score };
          }
          const bracketIndex = matchText.indexOf('(');
          const beforeBrackets = matchText.substring(0, bracketIndex).trim().split(' ');
          const mainScore = beforeBrackets.pop() || '?';
          return { teamName: beforeBrackets.join(' '), fullScore: `${mainScore} ${matchText.substring(bracketIndex)}` };
        };

        const team1Info = parseTeamAndScore(matchPair[0]);
        const team2Info = parseTeamAndScore(matchPair[1]);

        let prediction = predictionMap.get(`${team1Info.teamName}_${team2Info.teamName}`)
          || predictionMap.get(team1Info.teamName);

        if (!prediction && Array.isArray(predictionData) && predictionData !== 'no_content') {
          prediction = predictionData.find(pred =>
            Array.isArray(pred) && pred.length === 2 &&
            (pred[0].toLowerCase().includes(team1Info.teamName.toLowerCase()) ||
             pred[1].toLowerCase().includes(team2Info.teamName.toLowerCase()))
          );
        }

        const getPredScore = (predItem, idx) => {
          if (!predItem || !Array.isArray(predItem)) return '(-)';
          const parts = predItem[idx].split(' ');
          const s = parts[parts.length - 1];
          if (!isNaN(s)) return `(${s})`;
          const m = predItem[idx].match(/\d+/);
          return m ? `(${m[0]})` : '(-)';
        };

        [
          { info: team1Info, predIdx: 0 },
          { info: team2Info, predIdx: 1 }
        ].forEach(({ info, predIdx }) => {
          const rowDiv = document.createElement('div');
          rowDiv.className = 'team-score';
          const teamSpan = document.createElement('span');
          teamSpan.className = 'team';
          teamSpan.textContent = info.teamName;
          const scoreSpan = document.createElement('span');
          scoreSpan.className = 'score';
          scoreSpan.textContent = predIdx === 1 && info.fullScore === '?' ? '' : `${info.fullScore} ${getPredScore(prediction, predIdx)}`;
          rowDiv.appendChild(teamSpan);
          rowDiv.appendChild(scoreSpan);
          matchDiv.appendChild(rowDiv);
        });

        competitionDiv.appendChild(matchDiv);
      });
    });
  }

  container.appendChild(matchesContainer);
}

async function getFutureMatches() {
  const spinner = document.getElementById('spinner');
  spinner.style.display = 'block';
  const userName = localStorage.getItem('userName');
  if (!userName) {
    spinner.style.display = 'none';
    return;
  }
  const results = [];
  const dates = [];
  const predictionsByDate = {};
  for (let i = 0; i <= 3; i++) {
    const date = new Date();
    date.setDate(date.getDate() + i);
    const formattedDate = formatDate(date);
    dates.push(formattedDate);

    const cacheKey = `future-matches_${formattedDate}`;
    let cached = null;
    try { cached = await SportCache.get(cacheKey); } catch (e) {}
    if (cached) {
      results.push(cached);
    } else {
      try {
        const response = await fetch(`/api/v0/predictions/future-matches?date=${formattedDate}`);
        if (!response.ok) {
          throw new Error(`HTTP error! Status: ${response.status}`);
        }
        const data = await response.json();
        const innerData = Array.isArray(data) && data.length === 1 ? data[0] : data;
        const hasMatches = innerData && Array.isArray(innerData) && innerData.length > 0;
        if (hasMatches) {
          await SportCache.set(cacheKey, data);
        }
        results.push(data);
      } catch (error) {
        console.error('Error fetching data:', error);
      }
    }

    try {
      const predResp = await fetch(`/api/v0/predictions?date=${formattedDate}`);
      if (predResp.status === 200) {
        const predJson = await predResp.json();
        predictionsByDate[formattedDate] = predJson.predictions;
      }
    } catch (e) {}
  }
  console.log(results);
  displayFutureMatches(results, dates, predictionsByDate);
  spinner.style.display = 'none';
}

function displayFutureMatches(results, dates, predictionsByDate = {}) {
  const resultsContainer = document.getElementById('match-container');
  resultsContainer.innerHTML = '';
  const currentDate = new Date();
  const formattedCurrentDate = formatDate(currentDate);
  const userName = localStorage.getItem('userName');

  results.forEach((result, index) => {
    const dateGroup = document.createElement('div');
    dateGroup.className = 'date-group';

    const dateHeader = document.createElement('h2');
    const matchDate = dates[index];
    dateHeader.innerHTML = `
      <span style="display: flex; justify-content: space-between; align-items: center;">
        ${t('msg.matches_for')}${formatDateForDisplay(dates[index])}
        <span class="toggle-icon">▼</span>
      </span>
    `;
    dateHeader.style.cursor = 'pointer';
    dateGroup.appendChild(dateHeader);

    const contentContainer = document.createElement('div');
    contentContainer.className = 'collapsible-content';
    contentContainer.style.display = 'none';

    dateHeader.addEventListener('click', function() {
      if (contentContainer.style.display === 'none') {
        contentContainer.style.display = 'block';
        dateHeader.querySelector('.toggle-icon').textContent = '▲';
      } else {
        contentContainer.style.display = 'none';
        dateHeader.querySelector('.toggle-icon').textContent = '▼';
      }
    });

    const isCurrentDate = matchDate === formattedCurrentDate;
    const [predYear, predMonth, predDay] = dates[index].split('-');
    const predictions = [{ date: formatDateToEnglish(`${predDay}/${predMonth}`) }];

    console.log("📊 Result structure:", result);

    let tournamentData = [];
    if (Array.isArray(result) && result.length > 0 && Array.isArray(result[0])) {
      tournamentData = result[0];
    } else if (Array.isArray(result)) {
      tournamentData = result;
    }

    console.log("📊 Tournament data:", tournamentData);

    if (!tournamentData || tournamentData.length === 0) {
      const message = document.createElement('div');
      message.className = 'message no-matches';
      const [year, month, day] = dates[index].split('-');
      const dateForEnglish = `${day}/${month}`;
      message.innerHTML = `${formatDateToEnglish(dateForEnglish)} ${t('msg.no_matches_day')}`;
      message.style.textAlign = 'center';
      contentContainer.appendChild(message);
    } else {
      let competitionContainer = document.createElement('div');
      competitionContainer.className = 'competition-container';
      const matchesData = [];

      tournamentData.forEach(tournamentObj => {
        console.log("🔍 Processing tournament object:", tournamentObj);

        if (tournamentObj.tournament && tournamentObj.country && Array.isArray(tournamentObj.match)) {
          const currentCompetition = {
            tournament: tournamentObj.tournament,
            country: tournamentObj.country
          };
          predictions.push(currentCompetition);

          const competitionDiv = document.createElement('div');
          const competitionHeader = document.createElement('h3');
          competitionHeader.textContent = `${tournamentObj.country}: ${tournamentObj.tournament}`;
          competitionDiv.appendChild(competitionHeader);
          competitionContainer.appendChild(competitionDiv);

          tournamentObj.match.forEach(matchPair => {
            console.log("⚽ Processing match:", matchPair);

            if (!Array.isArray(matchPair) || matchPair.length !== 2) {
              console.warn("⚠️ Invalid match format:", matchPair);
              return;
            }

            const matchDiv = document.createElement('div');
            matchDiv.className = 'match';

            const matchData = {
              div: matchDiv,
              teams: {},
              inputs: {}
            };

            const team1Div = document.createElement('div');
            team1Div.className = 'team-score';
            const team1 = document.createElement('span');
            team1.className = 'team';

            const team1Name = matchPair[0].replace(' null', '').replace(' ?', '').replace(/ 0$/, '').trim();
            team1.textContent = team1Name;
            matchData.teams.team1 = team1Name;

            const score1 = document.createElement('input');
            score1.className = 'score';
            score1.type = 'number';
            score1.placeholder = '';
            score1.style.width = '20px';
            score1.style.height = '20px';
            score1.style.textAlign = 'center';
            score1.disabled = isCurrentDate;
            matchData.inputs.score1 = score1;

            team1Div.appendChild(team1);
            team1Div.appendChild(score1);

            const team2Div = document.createElement('div');
            team2Div.className = 'team-score';
            const team2 = document.createElement('span');
            team2.className = 'team';

            const team2Name = matchPair[1].replace(' null', '').replace(' ?', '').replace(/ 0$/, '').trim();
            team2.textContent = team2Name;
            matchData.teams.team2 = team2Name;

            const score2 = document.createElement('input');
            score2.className = 'score';
            score2.type = 'number';
            score2.placeholder = '';
            score2.style.width = '20px';
            score2.style.height = '20px';
            score2.style.textAlign = 'center';
            score2.disabled = isCurrentDate;
            matchData.inputs.score2 = score2;

            team2Div.appendChild(team2);
            team2Div.appendChild(score2);

            matchDiv.appendChild(team1Div);
            matchDiv.appendChild(team2Div);
            competitionDiv.appendChild(matchDiv);

            matchesData.push(matchData);
            predictions.push([`${team1Name} ${score1.value}`, `${team2Name} ${score2.value}`]);

            const separator = document.createElement('div');
            separator.style.borderTop = '1px solid rgba(0, 0, 0, 0.8)';
            separator.style.margin = '10px 0';
            competitionDiv.appendChild(separator);
          });
        } else {
          console.warn("⚠️ Tournament object missing required fields:", tournamentObj);
        }
      });

      let hasPrefilled = false;
      const prefilledSet = new Set();
      const existingPreds = predictionsByDate[matchDate];
      if (existingPreds && Array.isArray(existingPreds)) {
        const normalizedPreds = existingPreds.length > 0 && Array.isArray(existingPreds[0])
          ? existingPreds[0] : existingPreds;
        const predMap = {};
        normalizedPreds.forEach(item => {
          if (!Array.isArray(item) || item.length !== 2) return;
          const homeFull = String(item[0]);
          const awayFull = String(item[1]);
          const homeTeam = homeFull.split(' ').slice(0, -1).join(' ');
          const awayTeam = awayFull.split(' ').slice(0, -1).join(' ');
          const homeScore = homeFull.split(' ').slice(-1)[0];
          const awayScore = awayFull.split(' ').slice(-1)[0];
          predMap[homeTeam + '|' + awayTeam] = { homeScore, awayScore };
        });
        matchesData.forEach(md => {
          const key = md.teams.team1 + '|' + md.teams.team2;
          if (predMap[key]) {
            md.inputs.score1.value = predMap[key].homeScore;
            md.inputs.score2.value = predMap[key].awayScore;
            prefilledSet.add(key);
            hasPrefilled = true;
          }
        });
      }

      const errorMessage = document.createElement('div');
      errorMessage.className = 'message error-message';
      errorMessage.textContent = t('msg.please_fill');
      errorMessage.style.display = 'none';
      competitionContainer.appendChild(errorMessage);

      const submitButton = document.createElement('button');
      submitButton.className = 'submit-button';
      submitButton.textContent = t('msg.submit');
      submitButton.disabled = isCurrentDate;
      competitionContainer.appendChild(submitButton);

      submitButton.addEventListener('click', () => {
        const userName = localStorage.getItem('userName');
        let allFilled = true;
        const successMessage = competitionContainer.querySelector('.success-message');
        if (successMessage) {
          successMessage.remove();
        }

        predictions.splice(1);
        const actualPredictions = [];

        matchesData.forEach((matchData) => {
          const score1 = matchData.inputs.score1.value.trim();
          const score2 = matchData.inputs.score2.value.trim();

          const matchPrediction = {
            team1: matchData.teams.team1,
            team2: matchData.teams.team2,
            score1: score1,
            score2: score2
          };
          actualPredictions.push(matchPrediction);

          if (!score1) {
            matchData.inputs.score1.style.border = '2px solid red';
            allFilled = false;
          } else {
            matchData.inputs.score1.style.border = '';
          }

          if (!score2) {
            matchData.inputs.score2.style.border = '2px solid red';
            allFilled = false;
          } else {
            matchData.inputs.score2.style.border = '';
          }

          if (score1 && score2) {
            predictions.push([`${matchData.teams.team1} ${score1}`, `${matchData.teams.team2} ${score2}`]);
          }
        });

        if (!allFilled) {
          errorMessage.style.display = 'block';
        } else {
          errorMessage.style.display = 'none';
          const request = {
            userName: userName,
            predictions: predictions,
            matchDate: dates[index]
          };

          fetch('/api/v0/predictions', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json'
            },
            body: JSON.stringify(request)
          })
            .then(response => {
              if (response.ok) {
                return response.text();
              } else {
                throw new Error('Network response was not ok.');
              }
            })
            .then(() => {
              const successMessage = document.createElement('div');
              successMessage.className = 'message success-message';
              successMessage.textContent = t('msg.prediction_saved');
              competitionContainer.appendChild(successMessage);

              matchesData.forEach((matchData, idx) => {
                const prediction = actualPredictions[idx];
                if (prediction && prediction.score1 && prediction.score2) {
                  const team1Div = matchData.div.querySelector('.team-score:first-child');
                  const score1Input = team1Div.querySelector('.score');
                  team1Div.removeChild(score1Input);

                  const score1Text = document.createElement('span');
                  score1Text.className = 'score-prediction';
                  score1Text.textContent = prediction.score1;
                  score1Text.style.marginLeft = '5px';
                  score1Text.style.fontWeight = 'bold';
                  team1Div.appendChild(score1Text);

                  const team2Div = matchData.div.querySelector('.team-score:last-child');
                  const score2Input = team2Div.querySelector('.score');
                  team2Div.removeChild(score2Input);

                  const score2Text = document.createElement('span');
                  score2Text.className = 'score-prediction';
                  score2Text.textContent = prediction.score2;
                  score2Text.style.marginLeft = '5px';
                  score2Text.style.fontWeight = 'bold';
                  team2Div.appendChild(score2Text);
                }
              });

              submitButton.disabled = true;
              submitButton.textContent = t('msg.prediction_saved_btn');
              const matchDate = dates[index];
              SportCache.invalidate(`future-matches_${matchDate}`);
              SportCache.invalidate(`match-status_${matchDate}_${userName}`);
              Object.keys(localStorage).forEach(key => {
                if (key.startsWith('sp_cache_group-matches_') && key.includes(`_${matchDate}_`)) {
                  localStorage.removeItem(key);
                }
              });
            })
            .catch(error => {
              console.error('There was a problem with the fetch operation:', error);
            });
        }
      });

      if (hasPrefilled) {
        matchesData.forEach(md => {
          const key = md.teams.team1 + '|' + md.teams.team2;
          if (!prefilledSet.has(key)) return;
          const s1 = md.inputs.score1.value;
          const s2 = md.inputs.score2.value;
          if (s1 && s2) {
            const team1Div = md.div.querySelector('.team-score:first-child');
            const score1Input = team1Div.querySelector('.score');
            if (score1Input) {
              team1Div.removeChild(score1Input);
              const s1Text = document.createElement('span');
              s1Text.className = 'score-prediction';
              s1Text.textContent = s1;
              s1Text.style.marginLeft = '5px';
              s1Text.style.fontWeight = 'bold';
              team1Div.appendChild(s1Text);
            }
            const team2Div = md.div.querySelector('.team-score:last-child');
            const score2Input = team2Div.querySelector('.score');
            if (score2Input) {
              team2Div.removeChild(score2Input);
              const s2Text = document.createElement('span');
              s2Text.className = 'score-prediction';
              s2Text.textContent = s2;
              s2Text.style.marginLeft = '5px';
              s2Text.style.fontWeight = 'bold';
              team2Div.appendChild(s2Text);
            }
          }
        });
        if (prefilledSet.size === matchesData.length) {
          submitButton.disabled = true;
          submitButton.textContent = t('msg.prediction_saved_btn');
          const successMsg = document.createElement('div');
          successMsg.className = 'message success-message';
          successMsg.textContent = t('msg.prediction_saved');
          competitionContainer.appendChild(successMsg);
        }
      }

      contentContainer.appendChild(competitionContainer);
    }

    dateGroup.appendChild(contentContainer);
    resultsContainer.appendChild(dateGroup);
  });

  const style = document.createElement('style');
  style.textContent = `
    .date-group h2 {
      background-color: #f5f5f5;
      padding: 10px;
      margin: 5px 0;
      border-radius: 5px;
      transition: background-color 0.3s;
    }

    .date-group h2:hover {
      background-color: #e5e5e5;
    }

    .toggle-icon {
      font-size: 12px;
      transition: transform 0.3s;
    }

    .collapsible-content {
      padding: 10px;
      border: 1px solid #e5e5e5;
      border-top: none;
      border-radius: 0 0 5px 5px;
      margin-bottom: 10px;
    }

    .score-prediction {
      display: inline-block;
      width: 20px;
      height: 20px;
      text-align: center;
      color: #3366cc;
    }

    .success-message {
      background-color: #d4edda;
      color: #155724;
      padding: 10px;
      margin-top: 10px;
      border-radius: 4px;
    }
  `;
  document.head.appendChild(style);
}

function getMenuOpenLeft() {
  return document.getElementById('user-name').getBoundingClientRect().left;
}

function toggleMenu() {
  const submenu = document.getElementById('submenu');
  const overlay = document.getElementById('overlay');
  const contentLeft = getMenuOpenLeft();

  if (submenu.style.display === 'flex') {
    submenu.style.left = (contentLeft - 300) + 'px';
    overlay.classList.remove('active');
    setTimeout(() => {
      submenu.style.display = 'none';
      overlay.style.display = 'none';
    }, 500);
  } else {
    submenu.style.left = (contentLeft - 300) + 'px';
    submenu.style.display = 'flex';
    overlay.style.display = 'block';
    setTimeout(() => {
      overlay.classList.add('active');
      submenu.style.left = contentLeft + 'px';
    }, 10);
  }
}

function closeMenuOutsideClick() {
  const overlay = document.getElementById('overlay');
  overlay.addEventListener('click', toggleMenu);
}

function closeMenu() {
  const submenu = document.getElementById('submenu');
  const overlay = document.getElementById('overlay');
  const contentLeft = getMenuOpenLeft();

  submenu.style.left = (contentLeft - 300) + 'px';
  overlay.classList.remove('active');

  setTimeout(() => {
    submenu.style.display = 'none';
    overlay.style.display = 'none';
  }, 500);
}

async function getCompetitions() {
  try {
    const response = await fetch('/api/v0/competitions/list');
    if (!response.ok) {
      throw new Error('Failed to fetch competitions');
    }
    const competitionsData = await response.json();
    console.log('🔍 RAW /competitions/list ->', competitionsData);

    const userName = localStorage.getItem('userName');
    if (userName) {
      const userCompCacheKey = `user-competitions_${userName}`;
      let userCompetitions = null;
      try { userCompetitions = await SportCache.get(userCompCacheKey); } catch (e) {}
      if (!userCompetitions) {
        const userCompResponse = await fetch('/api/v0/competitions/user-competitions');
        if (userCompResponse.ok) {
          userCompetitions = await userCompResponse.json();
          await SportCache.set(userCompCacheKey, userCompetitions);
        } else {
          console.warn('❌ User competitions request not OK', userCompResponse.status);
          userCompetitions = [];
        }
      }
      displayCompetitions(competitionsData, userCompetitions);
    } else {
      console.warn('❌ No userName in localStorage');
      displayCompetitions(competitionsData, []);
    }
  } catch (error) {
    console.error('❌ Error fetching competitions:', error);
  }
}

function displayCompetitions(competitionsData, userCompetitions = []) {
  const container = document.getElementById('competitions-list');
  if (!container) return;

  container.innerHTML = '';

  const header = document.createElement('div');
  header.className = 'competitions-header';
  header.innerHTML = `<span>${t('msg.your_competitions')}</span>`;
  container.appendChild(header);

  const listContainer = document.createElement('div');
  listContainer.className = 'competitions-items';

  try {
    const selectedCompetitions = {};

    const createCompetitionItem = (country, competition, code = null) => {
      const itemDiv = document.createElement('div');
      itemDiv.className = 'competition-item';
      itemDiv.style.cssText = `
        display: flex !important;
        flex-direction: row !important;
        align-items: center !important;
        justify-content: space-between !important;
        padding: 12px 15px !important;
        border-bottom: 1px solid #eee !important;
        transition: background-color 0.2s !important;
        cursor: pointer !important;
      `;

      const competitionText = document.createElement('div');
      competitionText.className = 'competition-text';
      competitionText.style.cssText = `
        flex: 1 !important;
        display: flex !important;
        flex-direction: column !important;
        gap: 4px !important;
      `;

      const countrySpan = document.createElement('span');
      countrySpan.style.cssText = `
        font-weight: bold !important;
        color: #333 !important;
        font-size: 14px !important;
        display: block !important;
      `;
      countrySpan.textContent = country;

      const compSpan = document.createElement('span');
      compSpan.style.cssText = `
        color: #666 !important;
        font-size: 13px !important;
        display: block !important;
      `;
      compSpan.textContent = competition;

      competitionText.appendChild(countrySpan);
      competitionText.appendChild(compSpan);

      const selectIcon = document.createElement('span');
      selectIcon.className = 'select-icon';
      selectIcon.innerHTML = '📌';
      selectIcon.style.cssText = `
        cursor: pointer !important;
        font-size: 18px !important;
        margin-left: 10px !important;
        flex-shrink: 0 !important;
      `;

      console.log('🔍 Checking competition:', { country, competition, code });
      console.log('🔍 Against userCompetitions:', userCompetitions);

      const isSelected = userCompetitions.some(comp => {
        console.log('🔍 Comparing comp:', comp);
        console.log('🔍 comp.country:', comp.country, 'vs', country);
        console.log('🔍 comp.name:', comp.name, 'vs', competition);

        const countryMatch = comp.country === country;
        const nameMatch = comp.name === competition;
        const result = countryMatch && nameMatch;

        console.log('🔍 Match result:', result);
        return result;
      });

      console.log('🔍 Final isSelected for', country, competition, ':', isSelected);

      const key = `${country}:${competition}`;

      if (isSelected) {
        console.log('✅ Setting as SELECTED:', key);
        selectIcon.style.opacity = '1';
        selectIcon.title = 'Click to unselect';
        itemDiv.classList.add('selected-competition');
        itemDiv.style.backgroundColor = '#e8f5e9';
        selectedCompetitions[key] = code
          ? { country: country, name: competition, code: code }
          : { [country]: competition };
      } else {
        console.log('❌ Setting as NOT selected:', key);
        selectIcon.style.opacity = '0.3';
        selectIcon.title = 'Click to select';
      }

      const toggleSelection = () => {
        if (selectedCompetitions[key]) {
          delete selectedCompetitions[key];
          selectIcon.style.opacity = '0.3';
          selectIcon.title = 'Click to select';
          itemDiv.classList.remove('selected-competition');
          itemDiv.style.backgroundColor = '';
        } else {
          selectedCompetitions[key] = code
            ? { country: country, name: competition, code: code }
            : { [country]: competition };
          selectIcon.style.opacity = '1';
          selectIcon.title = 'Click to unselect';
          itemDiv.classList.add('selected-competition');
          itemDiv.style.backgroundColor = '#e8f5e9';
        }
      };

      itemDiv.addEventListener('click', toggleSelection);

      itemDiv.appendChild(competitionText);
      itemDiv.appendChild(selectIcon);

      itemDiv.addEventListener('mouseenter', () => {
        if (!itemDiv.classList.contains('selected-competition')) {
          itemDiv.style.backgroundColor = '#f5f5f5';
        }
      });

      itemDiv.addEventListener('mouseleave', () => {
        if (!itemDiv.classList.contains('selected-competition')) {
          itemDiv.style.backgroundColor = '';
        }
      });

      return itemDiv;
    };

    competitionsData.forEach(compObj => {
      if (compObj.country && compObj.name) {
        const itemDiv = createCompetitionItem(compObj.country, compObj.name, compObj.code);
        listContainer.appendChild(itemDiv);
      } else if (typeof compObj === 'object') {
        for (const country in compObj) {
          const competition = compObj[country];
          const itemDiv = createCompetitionItem(country, competition, null);
          listContainer.appendChild(itemDiv);
        }
      }
    });

    container.appendChild(listContainer);

    const saveButtonContainer = document.createElement('div');
    saveButtonContainer.className = 'save-button-container';
    saveButtonContainer.style.cssText = `
      padding: 15px !important;
      text-align: center !important;
      border-top: 1px solid #ddd !important;
      background-color: #fafafa !important;
      flex-shrink: 0 !important;
    `;

    const saveButton = document.createElement('button');
    saveButton.id = 'save-competitions-btn';
    saveButton.className = 'save-competitions-btn';
    saveButton.textContent = 'SAVE';
    saveButton.style.cssText = `
      background-color: #4CAF50 !important;
      color: white !important;
      padding: 15px 20px !important;
      font-size: 16px !important;
      font-weight: bold !important;
      border: none !important;
      border-radius: 8px !important;
      cursor: pointer !important;
      width: 100% !important;
      max-width: 250px !important;
      margin: 0 auto !important;
      display: block !important;
      transition: all 0.3s ease !important;
      box-shadow: 0 2px 5px rgba(0,0,0,0.2) !important;
    `;

    saveButton.addEventListener('click', async () => {
      const userName = localStorage.getItem('userName');
      if (!userName) {
        console.error('Username not found in localStorage');
        alert('Помилка: не знайдено ім\'я користувача');
        return;
      }

      const competitionsToSave = {
        userName: userName,
        competitions: Object.values(selectedCompetitions)
      };

      console.log('💾 Saving competitions:', competitionsToSave);

      saveButton.textContent = 'Saving...';
      saveButton.disabled = true;
      saveButton.style.backgroundColor = '#45a049';

      try {
        const response = await fetch('/api/v0/competitions/save-user-competitions', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(competitionsToSave)
        });

        if (response.ok) {
          saveButton.textContent = 'SAVED ✓';
          saveButton.style.backgroundColor = '#4CAF50';
          console.log('✅ Competitions saved successfully');

          SportCache.invalidate(`user-competitions_${userName}`);

          setTimeout(() => {
            saveButton.textContent = 'SAVE';
            saveButton.disabled = false;
            saveButton.style.backgroundColor = '#4CAF50';
          }, 3000);
        } else {
          console.error('❌ Server response not ok:', response.status);
          saveButton.textContent = 'ERROR!';
          saveButton.style.backgroundColor = '#F44336';

          setTimeout(() => {
            saveButton.textContent = 'SAVE';
            saveButton.style.backgroundColor = '#4CAF50';
            saveButton.disabled = false;
          }, 3000);
        }
      } catch (error) {
        console.error('❌ Error saving competitions:', error);
        saveButton.textContent = 'ERROR!';
        saveButton.style.backgroundColor = '#F44336';

        setTimeout(() => {
          saveButton.textContent = 'SAVE';
          saveButton.style.backgroundColor = '#4CAF50';
          saveButton.disabled = false;
        }, 3000);
      }
    });

    saveButton.addEventListener('mouseenter', () => {
      if (!saveButton.disabled) {
        saveButton.style.backgroundColor = '#45a049';
        saveButton.style.transform = 'translateY(-1px)';
        saveButton.style.boxShadow = '0 4px 8px rgba(0,0,0,0.3)';
      }
    });

    saveButton.addEventListener('mouseleave', () => {
      if (!saveButton.disabled) {
        saveButton.style.backgroundColor = '#4CAF50';
        saveButton.style.transform = 'translateY(0)';
        saveButton.style.boxShadow = '0 2px 5px rgba(0,0,0,0.2)';
      }
    });

    saveButtonContainer.appendChild(saveButton);
    container.appendChild(saveButtonContainer);

  } catch (error) {
    console.error('❌ Error parsing competitions data:', error);
    container.innerHTML += '<div class="error">Failed to load competitions</div>';
  }
}

let tgConnected = false;
let tgPollInterval = null;

async function initTelegramButton() {
  try {
    const resp = await fetch('/api/v0/telegram/status');
    if (!resp.ok) return;
    const data = await resp.json();
    tgConnected = data.connected;
    updateTelegramButton();
  } catch (e) {
    console.warn('Failed to check Telegram status:', e);
  }
}

function updateTelegramButton() {
  const btn = document.getElementById('tg-bot-btn');
  if (!btn) return;
  if (tgConnected) {
    btn.classList.add('connected');
    btn.textContent = t('tg.connected_btn');
  } else {
    btn.classList.remove('connected');
    btn.textContent = t('tg.connect_btn');
  }
}

async function handleTelegramBtn() {
  if (tgConnected) {
    if (!confirm(t('tg.disconnect_confirm'))) return;
    try {
      await fetch('/api/v0/telegram/disconnect', { method: 'DELETE' });
      tgConnected = false;
      updateTelegramButton();
    } catch (e) {
      console.error('Failed to disconnect Telegram:', e);
    }
  } else {
    try {
      const resp = await fetch('/api/v0/telegram/link-token');
      if (!resp.ok) return;
      const data = await resp.json();
      window.open(`https://t.me/${data.botUsername}?start=${data.token}`, '_blank');
      startTelegramConnectionPolling();
    } catch (e) {
      console.error('Failed to get Telegram link token:', e);
    }
  }
}

function startTelegramConnectionPolling() {
  if (tgPollInterval) clearInterval(tgPollInterval);
  let attempts = 0;
  tgPollInterval = setInterval(async () => {
    attempts++;
    if (attempts > 20) {
      clearInterval(tgPollInterval);
      return;
    }
    try {
      const resp = await fetch('/api/v0/telegram/status');
      const data = await resp.json();
      if (data.connected) {
        tgConnected = true;
        updateTelegramButton();
        clearInterval(tgPollInterval);
      }
    } catch (e) {}
  }, 6000);
}

window.onI18nChange = function() {
  const userName = localStorage.getItem('userName');
  const greetingEl = document.getElementById('greeting-text');
  if (greetingEl) {
    greetingEl.textContent = t('msg.hello') + (userName ? `${userName}!` : 'User!');
  }
  document.getElementById('resultsContainer').innerHTML = '';
  document.getElementById('match-container').innerHTML = '';
  getMatchResult();
  getFutureMatches();
  updateTelegramButton();
};

document.addEventListener('DOMContentLoaded', () => {
  getUserName();
  getFutureMatches();
  getMatchResult();
  closeMenuOutsideClick();
  getCompetitions();
  initTelegramButton();
});
