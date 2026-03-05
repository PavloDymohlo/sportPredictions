function goBack() {
  const params = new URLSearchParams(window.location.search);
  const from = params.get('from');
  if (from === 'host') {
    window.location.href = '/host-page';
  } else {
    window.location.href = '/office-page';
  }
}
