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