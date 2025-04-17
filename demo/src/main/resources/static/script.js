document.addEventListener('DOMContentLoaded', function() {
    const menuToggle = document.getElementById('menuToggle');
    const sidebar = document.getElementById('sidebar');

    menuToggle.addEventListener('click', function() {
        sidebar.classList.toggle('sidebar-hidden');
        menuToggle.classList.toggle('active');
    });
});

//Javascript för att slidern med årtalen i advanced filters ska uppdateras.
const slider = document.getElementById('yearFilter');
const yearDisplay = document.getElementById('yearValue');

if (slider && yearDisplay) {
    yearDisplay.textContent = slider.value;

    slider.addEventListener('input', () => {
        yearDisplay.textContent = slider.value;
    });
}