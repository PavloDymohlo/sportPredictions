function formatDate(date) {
    return date.toISOString().split('T')[0];
}

function formatDateToEnglish(dateStr) {
    const [day, month] = dateStr.split('/');
    const monthNames = [
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    ];
    return `${day} ${monthNames[parseInt(month, 10) - 1]}`;
}

function formatDateForDisplay(isoDate) {
    const [year, month, day] = isoDate.split('-');
    const monthNames = [
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    ];
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
        groupsList.innerHTML = '<div class="no-groups-message">Please log in to see your groups.</div>';
        return;
    }

    groupsList.innerHTML = '<div class="groups-loading">Loading groups...</div>';

    try {
        const response = await fetch(`/api/v0/user-group/all-users-group/${userName}`, {
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
            groupsList.innerHTML = '<div class="no-groups-message">You don\'t have any groups yet.</div>';
        } else {
            groups.forEach(group => {
                const groupItem = document.createElement('div');
                groupItem.className = 'group-item';
                groupItem.textContent = group.userGroupName;
                groupsList.appendChild(groupItem);
            });
        }
    } catch (error) {
        console.error('❌ Error fetching user groups:', error);
        groupsList.innerHTML = '<div class="error-message">Failed to load groups. Please try again.</div>';
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
    searchResult.innerHTML = '<div class="groups-loading">Searching...</div>';

    try {
        const response = await fetch(`/api/v0/user-group/find-group/${groupName}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            const errorText = await response.text();
            searchResult.innerHTML = `<div class="no-groups-message">${errorText || 'Group not found'}</div>`;
            return;
        }

        const group = await response.json();
        console.log('✅ Group found:', group);

        searchResult.innerHTML = `
            <div class="found-group">
                <div class="found-group-info">
                    <div class="found-group-name">${group.userGroupName}</div>
                    <div class="found-group-leader">Leader: ${group.userGroupLeaderName}</div>
                </div>
                <button class="apply-group-btn" onclick="applyToGroup('${group.userGroupName}')">Apply</button>
            </div>
        `;

    } catch (error) {
        console.error('❌ Error searching group:', error);
        searchResult.innerHTML = '<div class="error-message">Failed to search group. Please try again.</div>';
    }
}

function applyToGroup(groupName) {
    console.log('Applying to group:', groupName);
    // TODO: Implement join group logic when you have the endpoint
    alert(`Application to join "${groupName}" will be implemented soon!`);
}

async function getUserName() {
    const spinner = document.getElementById('spinner');
    const userNameDiv = document.getElementById('user-name');
    spinner.style.display = 'block';
    const userName = await new Promise((resolve) => {
        setTimeout(() => {
            const storedUserName = localStorage.getItem('userName');
            resolve(storedUserName);
        }, 1000);
    });
    if (!userName) {
        console.error('User name not found in localStorage.');
        userNameDiv.textContent = 'Hello, User!';
    } else {
        userNameDiv.textContent = `Hello, ${userName}!`;
    }
    spinner.style.display = 'none';
}

async function getMatchResult() {
    const spinner = document.getElementById('spinner');
    const resultsContainer = document.getElementById('resultsContainer');
    spinner.style.display = 'block';
    const userName = localStorage.getItem('userName');
    if (!userName) {
        console.error('User name not found in localStorage.');
        spinner.style.display = 'none';
        return;
    }
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const formattedDate = formatDate(yesterday);
    try {
        const [matchesResponse, predictionsResponse] = await Promise.all([
            fetch(`/api/v0/competitions/match-status?date=${formattedDate}`, {
                method: 'GET',
                headers: {
                    'userName': userName,
                },
            }),
            fetch(`/api/v0/competitions/get-predictions?date=${formattedDate}`, {
                method: 'GET',
                headers: {
                    'userName': userName,
                },
            })
        ]);

        if (matchesResponse.status === 404) {
            const formattedDateEnglish = formatDateToEnglish(formattedDate);
            const message = `${formattedDateEnglish} Matches did not take place.<br>Please, wait next results.`;
            const container = document.createElement('div');
            container.className = 'date-group';
            container.innerHTML = message;
            container.style.textAlign = 'center';
            resultsContainer.appendChild(container);
            return;
        }

        const matches = await matchesResponse.json();
        console.log("✅ MATCHES RECEIVED:", matches);

        let predictions;
        if (predictionsResponse.status === 204) {
            predictions = { predictions: 'no_content' };
        } else {
            predictions = await predictionsResponse.json();
        }
        console.log("✅ PREDICTIONS RECEIVED:", predictions);

        console.log("📢 CALLING displayMatchResult");
        displayMatchResult(matches, predictions.predictions);
    } catch (error) {
        console.error('Error fetching match status or predictions:', error);
    } finally {
        spinner.style.display = 'none';
    }
}

async function displayMatchResult(matches, predictions) {
    console.log("Matches data:", matches);
    console.log("Predictions data:", predictions);

    const resultsContainer = document.getElementById('resultsContainer');
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const formattedDate = formatDate(yesterday);
    resultsContainer.innerHTML = '';

    const container = document.createElement('div');
    container.className = 'date-group';

    const header = document.createElement('h2');
    header.innerHTML = `
         <span style="line-height: 1.2;">Результати за ${formatDateToEnglish(formattedDate)}</span><br>
        <div style="display: flex; flex-direction: column; align-items: center; max-width: 100%;">
          <p style="font-size: 14px; margin: 5px 0; text-align: center;">Matches that you correctly predicted will be displayed on a green background.</p>
          <p style="font-size: 14px; margin: 5px 0; text-align: center;">To the right of the result, your prediction is displayed in parentheses.</p>
        </div>
    `;
    container.appendChild(header);

    const matchData = Array.isArray(matches) && matches.length > 0 && Array.isArray(matches[0])
    ? matches[0]
    : matches;

    console.log("📊 Processed match data:", matchData);

    if (!matchData || matchData.length === 0) {
        const noMatches = document.createElement('p');
        noMatches.textContent = 'There are no matches with predictions.';
        container.appendChild(noMatches);
        resultsContainer.appendChild(container);
        return;
    }

    let currentCompetition = '';
    let competitionDiv;
    const predictionMap = new Map();
    const matchesContainer = document.createElement('div');
    matchesContainer.className = 'matches-container';

    const predictionData = Array.isArray(predictions) && predictions.length > 0 && Array.isArray(predictions[0])
    ? predictions[0]
    : predictions;

    if (predictionData && predictionData !== 'no_content') {
        console.log("Processing predictions data:", predictionData);
        predictionData.forEach(item => {
            if (Array.isArray(item) && item.length === 2 && typeof item[0] === 'string' && typeof item[1] === 'string') {
                const homeTeam = item[0].split(' ').slice(0, -1).join(' ');
                const awayTeam = item[1].split(' ').slice(0, -1).join(' ');
                const key1 = `${homeTeam}_${awayTeam}`;
                const key2 = `${homeTeam.trim()}_${awayTeam.trim()}`;
                console.log(`Creating prediction map entry: ${key1} -> ${item[0]} vs ${item[1]}`);
                predictionMap.set(key1, item);
                predictionMap.set(key2, item);
                predictionMap.set(homeTeam, item);
                predictionMap.set(awayTeam, item);
            }
        });
    }

    console.log("Final prediction map size:", predictionMap.size);

    if (Array.isArray(matchData)) {
        console.log("Processing match data array with length:", matchData.length);

        matchData.forEach(item => {
            if ('tournament' in item && 'matches' in item) {
                const tournamentName = item.tournament || 'Unknown Tournament';

                if (Array.isArray(item.matches) && item.matches.length > 0) {
                    currentCompetition = tournamentName;
                    competitionDiv = document.createElement('div');
                    const competitionHeader = document.createElement('h3');
                    competitionHeader.textContent = tournamentName;
                    competitionDiv.appendChild(competitionHeader);
                    matchesContainer.appendChild(competitionDiv);

                    item.matches.forEach(matchObj => {
                        const matchPair = matchObj.match;
                        const isCorrect = matchObj.predictedCorrectly;

                        console.log("⚽ Processing match:", matchPair, "- Correct:", isCorrect);

                        if (!Array.isArray(matchPair) || matchPair.length !== 2) return;

                        const matchDiv = document.createElement('div');
                        matchDiv.className = 'match';

                        if (isCorrect) {
                            matchDiv.classList.add('correct-prediction');
                            console.log("✅ Adding green background for correct prediction");
                        }

                        const parseTeamAndScore = (matchText) => {
                            if (!matchText) return { teamName: '', score: '?', fullScore: '?' };

                            if (matchText.includes('winner')) {
                                let processedText = matchText.replace('winner', '').trim();


                                const scoreMatch = processedText.match(/(\d+)(?:\s*)$/);
                                let teamName = processedText;
                                let score = '?';

                                if (scoreMatch) {
                                    score = scoreMatch[1];
                                    teamName = processedText.replace(scoreMatch[0], '').trim();
                                }

                                teamName = `${teamName}  (go next)`;

                                return {
                                    teamName: teamName,
                                    score: score,
                                    fullScore: score
                                };
                            }

                            const hasBrackets = matchText.includes('(');

                            if (!hasBrackets) {
                                const parts = matchText.trim().split(' ');
                                const score = parts.pop() || '?';
                                const teamName = parts.join(' ');
                                return {
                                    teamName: teamName,
                                    score: score,
                                    fullScore: score
                                };
                            }

                            const bracketIndex = matchText.indexOf('(');
                            const beforeBrackets = matchText.substring(0, bracketIndex).trim();
                            const parts = beforeBrackets.split(' ');
                            const mainScore = parts.pop() || '?';
                            const teamName = parts.join(' ');
                            const extraInfo = matchText.substring(bracketIndex);

                            return {
                                teamName: teamName,
                                score: mainScore.trim(),
                                fullScore: `${mainScore} ${extraInfo}`
                            };
                        };

                        const team1Info = parseTeamAndScore(matchPair[0]);
                        const team2Info = parseTeamAndScore(matchPair[1]);

                        console.log(`Match teams: ${team1Info.teamName} vs ${team2Info.teamName}`);

                        const predictionKey = `${team1Info.teamName}_${team2Info.teamName}`;
                        const fallbackKey = team1Info.teamName;

                        console.log(`Looking up prediction for key: ${predictionKey}`);

                        let prediction = predictionMap.get(predictionKey);
                        if (!prediction) {
                            prediction = predictionMap.get(fallbackKey);
                            console.log(`Using fallback key: ${fallbackKey}`);
                        }

                        if (!prediction && Array.isArray(predictionData) && predictionData !== 'no_content') {

                            prediction = predictionData.find(pred => {
                                if (Array.isArray(pred) && pred.length === 2) {
                                    const homeTeamText = pred[0].toLowerCase();
                                    const awayTeamText = pred[1].toLowerCase();
                                    return homeTeamText.includes(team1Info.teamName.toLowerCase()) ||
                                    awayTeamText.includes(team2Info.teamName.toLowerCase());
                                }
                                return false;
                            });
                        }

                        console.log(`Found prediction:`, prediction);

                        const team1Div = document.createElement('div');
                        team1Div.className = 'team-score';

                        const team1 = document.createElement('span');
                        team1.className = 'team';
                        team1.textContent = team1Info.teamName;

                        const score1 = document.createElement('span');
                        score1.className = 'score';

                        if (team1Info.score === "Match not played") {
                            score1.classList.add('canceled-match');
                            score1.textContent = team1Info.score;
                        } else {
                            let predictionText = '(-)';

                            if (prediction) {
                                let predScore = '';

                                if (Array.isArray(prediction) && prediction.length === 2) {
                                    const parts = prediction[0].split(' ');
                                    predScore = parts[parts.length - 1];

                                    if (isNaN(predScore)) {
                                        const match = prediction[0].match(/\d+/);
                                        if (match) {
                                            predScore = match[0];
                                        }
                                    }
                                }

                                if (predScore && !isNaN(predScore)) {
                                    predictionText = `(${predScore})`;
                                    console.log(`Using prediction score for team 1: ${predScore}`);
                                }
                            }

                            score1.textContent = `${team1Info.fullScore} ${predictionText}`;
                        }

                        team1Div.appendChild(team1);
                        team1Div.appendChild(score1);

                        const team2Div = document.createElement('div');
                        team2Div.className = 'team-score';

                        const team2 = document.createElement('span');
                        team2.className = 'team';
                        team2.textContent = team2Info.teamName;

                        const score2 = document.createElement('span');
                        score2.className = 'score';

                        if (team2Info.score === "?") {
                            score2.textContent = "";
                        } else {
                            let predictionText = '(-)';

                            if (prediction) {
                                let predScore = '';

                                if (Array.isArray(prediction) && prediction.length === 2) {
                                    const parts = prediction[1].split(' ');
                                    predScore = parts[parts.length - 1];

                                    if (isNaN(predScore)) {
                                        const match = prediction[1].match(/\d+/);
                                        if (match) {
                                            predScore = match[0];
                                        }
                                    }
                                }

                                if (predScore && !isNaN(predScore)) {
                                    predictionText = `(${predScore})`;
                                    console.log(`Using prediction score for team 2: ${predScore}`);
                                }
                            }

                            score2.textContent = `${team2Info.fullScore} ${predictionText}`;
                        }

                        team2Div.appendChild(team2);
                        team2Div.appendChild(score2);

                        matchDiv.appendChild(team1Div);
                        matchDiv.appendChild(team2Div);
                        competitionDiv.appendChild(matchDiv);
                    });
                } else {
                    console.log(`Skipping tournament without matches: ${tournamentName}`);
                }
            }
        });
    }

    container.appendChild(matchesContainer);
    resultsContainer.appendChild(container);
}


async function getFutureMatches() {
    const spinner = document.getElementById('spinner');
    spinner.style.display = 'block';
    const userName = localStorage.getItem('userName');
    if (!userName) {
        console.error('User name not found in localStorage.');
        spinner.style.display = 'none';
        return;
    }
    const results = [];
    const dates = [];
    for (let i = 0; i <= 3; i++) {
        const date = new Date();
        date.setDate(date.getDate() + i);
        const formattedDate = formatDate(date);
        dates.push(formattedDate);
        try {
            const response = await fetch(`/api/v0/competitions/event?date=${formattedDate}`, {
                method: 'GET',
                headers: {
                    'userName': userName,
                },
            });
            if (!response.ok) {
                throw new Error(`HTTP error! Status: ${response.status}`);
            }
            const data = await response.json();
            results.push(data);
        } catch (error) {
            console.error('Error fetching data:', error);
        }
    }
    console.log(results);
    displayFutureMatches(results, dates);
    spinner.style.display = 'none';
}


function displayFutureMatches(results, dates) {
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
                Matches for ${formatDateForDisplay(dates[index])}
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
        const predictions = [{ date: formatDateToEnglish(dates[index]) }];

        const storageKey = `userPredictions_${matchDate}_${userName}`;
        const savedPredictions = localStorage.getItem(storageKey);

        if (savedPredictions) {
            contentContainer.innerHTML = savedPredictions;
        } else {
            console.log("📊 Result structure:", result);

            let tournamentData = [];
            if (Array.isArray(result) && result.length > 0 && Array.isArray(result[0])) {
                tournamentData = result[0]; // Беремо перший елемент, який є масивом турнірів
            } else if (Array.isArray(result)) {
                tournamentData = result;
            }

            console.log("📊 Tournament data:", tournamentData);

            if (!tournamentData || tournamentData.length === 0) {
                const message = document.createElement('div');
                message.className = 'message no-matches';
                const [year, month, day] = dates[index].split('-');
                const dateForEnglish = `${day}/${month}`;
                message.innerHTML = `${formatDateToEnglish(dateForEnglish)} No matches available.<br>Please wait for upcoming events.`;
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

                const errorMessage = document.createElement('div');
                errorMessage.className = 'message error-message';
                errorMessage.textContent = 'Please fill in all fields!';
                errorMessage.style.display = 'none';
                competitionContainer.appendChild(errorMessage);

                const submitButton = document.createElement('button');
                submitButton.className = 'submit-button';
                submitButton.textContent = 'Submit';
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

                        fetch('/api/v0/competitions/send-user-predictions', {
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
                            successMessage.textContent = 'Prediction saved successfully!';
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
                            submitButton.textContent = 'Prediction saved';
                            localStorage.setItem(storageKey, contentContainer.innerHTML);
                        })
                            .catch(error => {
                            console.error('There was a problem with the fetch operation:', error);
                        });
                    }
                });

                contentContainer.appendChild(competitionContainer);
            }
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

function toggleMenu() {
    const submenu = document.getElementById('submenu');
    const burgerButton = document.getElementById('burgerButton');
    const overlay = document.getElementById('overlay');

    submenu.classList.toggle('open');

    if (submenu.style.display === 'block') {
        overlay.classList.remove('active');

        setTimeout(() => {
            submenu.style.display = 'none';
            overlay.style.display = 'none';

            setTimeout(() => {
                burgerButton.style.opacity = '1';
                burgerButton.style.visibility = 'visible';
            }, 100);
        }, 500);
    } else {
        burgerButton.style.opacity = '0';
        burgerButton.style.visibility = 'hidden';

        submenu.style.display = 'block';
        overlay.style.display = 'block';

        setTimeout(() => {
            overlay.classList.add('active');
        }, 10);
    }
}

function closeMenuOutsideClick() {
    const overlay = document.getElementById('overlay');
    overlay.addEventListener('click', toggleMenu);
}

function closeMenu() {
    const submenu = document.getElementById('submenu');
    const burgerButton = document.getElementById('burgerButton');
    const overlay = document.getElementById('overlay');

    submenu.classList.remove('open');
    overlay.classList.remove('active');

    setTimeout(() => {
        submenu.style.display = 'none';
        overlay.style.display = 'none';

        setTimeout(() => {
            burgerButton.style.opacity = '1';
            burgerButton.style.visibility = 'visible';
        }, 100);
    }, 500);
}

async function getCompetitions() {
    try {
        const response = await fetch('/api/v0/competitions/list');
        if (!response.ok) {
            throw new Error('Failed to fetch competitions');
        }
        const competitions = await response.json();
        console.log('🔍 RAW /competitions/list ->', competitions);

        const userName = localStorage.getItem('userName');
        if (userName) {
            const userCompResponse = await fetch(`/api/v0/competitions/user-competitions/${userName}`);
            if (userCompResponse.ok) {
                const userCompetitions = await userCompResponse.json();
                console.log('🔍 USER COMPETITIONS FROM SERVER:', userCompetitions);
                console.log('🔍 Type of userCompetitions:', typeof userCompetitions);
                console.log('🔍 Is Array:', Array.isArray(userCompetitions));
                if (Array.isArray(userCompetitions) && userCompetitions.length > 0) {
                    console.log('🔍 First item structure:', userCompetitions[0]);
                }
                displayCompetitions(competitions, userCompetitions);
            } else {
                console.warn('❌ User competitions request not OK', userCompResponse.status);
                displayCompetitions(competitions, []);
            }
        } else {
            console.warn('❌ No userName in localStorage');
            displayCompetitions(competitions, []);
        }
    } catch (error) {
        console.error('❌ Error fetching competitions:', error);
    }
}

function displayCompetitions(competitions, userCompetitions = []) {
    const container = document.getElementById('competitions-list');
    if (!container) return;

    container.innerHTML = '';

    const header = document.createElement('div');
    header.className = 'competitions-header';
    header.innerHTML = '<span>YOUR COMPETITIONS</span>';
    container.appendChild(header);

    const listContainer = document.createElement('div');
    listContainer.className = 'competitions-items';

    try {
        let competitionsData = competitions[0];
        if (typeof competitionsData === 'string') {
            competitionsData = JSON.parse(competitionsData);
        }

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
            }
            else if (typeof compObj === 'object') {
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
            margin-top: 10px !important;
            background-color: #fafafa !important;
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

document.addEventListener('DOMContentLoaded', () => {
    getUserName();
    getFutureMatches();
    getMatchResult();
    closeMenuOutsideClick();
    getCompetitions();
});;