document.addEventListener('DOMContentLoaded', function() {
    const yearFilter = document.getElementById('yearFilter');
    const yearValue = document.getElementById('yearValue');

    if (yearFilter && yearValue) {
        // Initial värde
        yearValue.textContent = yearFilter.value;

        // Uppdatera när användaren ändrar skjutreglaget
        yearFilter.addEventListener('input', function() {
            yearValue.textContent = yearFilter.value;
        });
    }
});