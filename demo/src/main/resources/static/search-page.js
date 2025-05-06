// search-page.js - Lägg till sökladdningsindikator för huvudsöksidan

document.addEventListener('DOMContentLoaded', function() {
    const searchForm = document.querySelector('form[action="/search"]');

    if (searchForm) {
        // Skapa ett element för laddningsindikatorn som vi kan lägga till senare
        const loadingElement = document.createElement('div');
        loadingElement.className = 'search-loading mt-4';
        loadingElement.innerHTML = `
            <div class="spinner"></div>
            <p>Searching for books...</p>
        `;

        // Lägg till en dold laddningsindikator efter sökformuläret
        const formContainer = searchForm.closest('.container');
        if (formContainer) {
            loadingElement.style.display = 'none'; // Gör den dold från början
            formContainer.appendChild(loadingElement);
        }

        searchForm.addEventListener('submit', function(event) {
            const query = this.querySelector('input[name="query"]').value.trim();

            if (query) {
                // Visa laddningsindikatorn
                if (loadingElement) {
                    loadingElement.style.display = 'block';
                }

                // Viktigt: Låt formuläret skickas normalt
                // Vi förhindrar INTE standardbeteendet här

                // Detta är bara för visuell effekt, formuläret kommer att skickas och sidan laddas om
            }
        });
    }
});