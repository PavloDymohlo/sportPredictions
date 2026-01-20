document.getElementById('registrationForm').addEventListener('submit', function(event) {
    event.preventDefault();
    const userName = document.getElementById('register-login').value;
    const password = document.getElementById('register-password').value;
    const confirmPassword = document.getElementById('register-confirm-password').value;
    const messageDiv = document.getElementById('message');
    if (password !== confirmPassword) {
        messageDiv.innerText = 'Passwords do not match!';
        messageDiv.style.color = 'red';
        messageDiv.style.display = 'block';
        return;
    }
    const registrationData = {
        userName: userName,
        password: password
    };
    fetch('/api/v0/auth/register', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(registrationData)
    })
        .then(response => {
        const messageDiv = document.getElementById('message');
        if (response.redirected) {
            localStorage.setItem('userName', userName);
            window.location.href = response.url;
        } else if (response.ok) {
            return response.json().then(data => {
                localStorage.setItem('userName', data.userName);
            });
        } else {
            return response.text().then(errorMessage => {
                messageDiv.innerText = errorMessage;
                messageDiv.style.color = 'red';
                messageDiv.style.display = 'block';
                throw new Error(errorMessage);
            });
        }
    })
        .catch(error => {
        console.error('Registration failed: ', error.message);
    });
});

document.getElementById('loginForm').addEventListener('submit', function(event) {
    event.preventDefault();
    const userName = document.getElementById('login-login').value;
    const password = document.getElementById('login-password').value;
    const loginInData = {
        userName: userName,
        password: password
    };
    fetch('/api/v0/auth/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(loginInData)
    })
        .then(response => {
        const messageDiv = document.getElementById('message');
        if (response.redirected) {
            localStorage.setItem('userName', userName);
            window.location.href = response.url;
        } else if (response.ok) {
            return response.json().then(data => {
                localStorage.setItem('userName', data.userName);
            });
        } else {
            return response.text().then(errorMessage => {
                messageDiv.innerText = errorMessage;
                messageDiv.style.color = 'red';
                messageDiv.style.display = 'block';
                throw new Error(errorMessage);
            });
        }
    })
        .catch(error => {
        console.error('LoginIn failed: ', error.message);
    });
});