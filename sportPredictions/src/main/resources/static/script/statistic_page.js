async function fetchUsers() {
    const spinner = document.getElementById('spinner');
    spinner.style.display = 'block';
    try {
        const response = await fetch('/user/users');
        if (!response.ok) {
            throw new Error('Помилка при отриманні користувачів');
        }
        const users = await response.json();
        displayUsers(users[0]);
    } catch (error) {
        console.error('Помилка:', error);
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
                <th>Місце у рейтингу</th>
                <th>Ім'я користувача</th>
                <th>Загальний рахунок</th>
                <th>Місячний рахунок</th>
                <th>Кількість трофеїв</th>
                <th>Кількість прогнозів</th>
                <th>% вгаданих матчів</th>
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
            <td>${user.monthlyScore}</td>
            <td>${user.trophyCount}</td>
            <td>${user.predictionCount}</td>
            <td>${user.percentGuessedMatches}%</td>
        `;
        tableBody.appendChild(row);
    });
    usersStatisticDiv.appendChild(table);
}

function toggleMenu() {
    const submenu = document.getElementById('submenu');
    const burgerButton = document.getElementById('burgerButton');
    submenu.classList.toggle('open');
    if (submenu.style.display === 'block') {
        submenu.style.display = 'none';
        burgerButton.style.display = 'block';
    } else {
        submenu.style.display = 'block';
        burgerButton.style.display = 'none';
    }
}

function returnBack() {
    event.preventDefault();
    window.location.href = '/office-page';
}

document.addEventListener('DOMContentLoaded', fetchUsers);