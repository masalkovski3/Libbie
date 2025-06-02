// Function to scroll books in a bookshelf
function scrollBooks(shelfId, amount) {
    document.getElementById('books-' + shelfId).scrollBy({
        left: amount,
        behavior: 'smooth'
    });
}

// Function to scroll recommendations
function scrollRecommendations(amount) {
    document.getElementById('recommendation-container').scrollBy({
        left: amount,
        behavior: 'smooth'
    });
}

// Function to create a new bookshelf
const saveShelfButton = document.getElementById('saveShelfButton');
if (saveShelfButton) {
    saveShelfButton.addEventListener('click', function () {
        const shelfName = document.getElementById('shelfName').value.trim();
        const shelfDescription = document.getElementById('shelfDescription').value.trim();

        if (shelfName) {
            const body = `name=${encodeURIComponent(shelfName)}&description=${encodeURIComponent(shelfDescription)}`;

            fetch('/profile/bookshelves', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: body
            })
                .then(response => {
                    if (!response.ok) {
                        return response.json().then(errorData => {
                            throw new Error(errorData.errorMessage || 'An error occurred');
                        });
                    }
                    return response.json();
                })
                .then(data => {
                    const modal = bootstrap.Modal.getInstance(document.getElementById('addShelfModal'));
                    modal.hide();
                    showError(`Bookshelf created: ${shelfName}`);
                    setTimeout(() => {
                        location.reload();
                    }, 2000);
                })
                .catch(error => {
                    console.error('Error:', error);
                    showError(error.message);
                });
        } else {
            showError('Please enter a name for the bookshelf');
        }
    });
}

// Function to show the modal for adding a book
function showAddBookModal(bookshelfId) {
    document.getElementById('selectedBookshelfId').value = bookshelfId;
    document.getElementById('bookSearch').value = '';
    document.getElementById('searchResults').innerHTML = '';

    const modal = new bootstrap.Modal(document.getElementById('addBookModal'));
    modal.show();
}

// Search for books using the API
const searchBookButton = document.getElementById('searchBookButton');
if (searchBookButton) {
    searchBookButton.addEventListener('click', function () {
        const query = document.getElementById('bookSearch').value.trim();
        const resultsContainer = document.getElementById('searchResults');

        if (query) {
            // Visa laddningsindikator
            resultsContainer.innerHTML = `
            <div class="search-loading">
                <div class="spinner"></div>
                <p>Searching for books...</p>
            </div>
        `;
            //resultsContainer.innerHTML = '<p>Searching...</p>';

            // Use JSON API for book search
            fetch(`/api/books/search?query=${encodeURIComponent(query)}`)
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Network response was not ok: ' + response.statusText);
                    }
                    return response.json();
                })
                .then(data => {
                    console.log("Search results:", data); // For debugging
                    resultsContainer.innerHTML = '';

                    if (!data || data.length === 0) {
                        resultsContainer.innerHTML = '<p>No books found</p>';
                        return;
                    }

                    data.forEach(book => {
                        const resultItem = document.createElement('div');
                        resultItem.className = 'book-result-item';
                        resultItem.onclick = function () {
                            addBookToShelf(document.getElementById('selectedBookshelfId').value, book.workId);
                        };

                        const coverUrl = book.coverUrl ? book.coverUrl : '/images/no_cover.jpeg';

                        resultItem.innerHTML = `
                        <img src="${coverUrl}" alt="Book cover" class="book-result-cover">
                        <div class="book-result-info">
                            <h5 class="book-result-title">${book.title}</h5>
                            <p class="book-result-author">${book.author}</p>
                        </div>
                    `;

                        resultsContainer.appendChild(resultItem);
                    });
                })
                .catch(error => {
                    console.error('Error:', error);
                    resultsContainer.innerHTML = '<p>An error occurred during the search. Please try again.</p>';
                });
        } else {
            showError('Please enter a search term');
        }
    });
}

// Function to add a book to a bookshelf
function addBookToShelf(bookshelfId, workId) {
    fetch(`/profile/bookshelves/${bookshelfId}/books`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: `workId=${encodeURIComponent(workId)}`
    })
        .then(response => response.json())
        .then(data => {
            if (data.error) {
                showError('Error: ' + data.error);
            } else {
                // Close the modal
                const modal = bootstrap.Modal.getInstance(document.getElementById('addBookModal'));
                modal.hide();

                showError('Book added to bookshelf');

                // Reload the page to show the new book
                setTimeout(() => {
                    location.reload();
                }, 2000);
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showError('An error occurred: ' + error.message);
        });
}

// Function to remove a book from a bookshelf
function removeBookFromShelf(bookshelfId, workId) {
    console.log("Removing book:", workId, "from shelf:", bookshelfId);

    showConfirm('Are you sure you want to remove this book from the bookshelf?', function() {
        fetch(`/profile/bookshelves/${bookshelfId}/books/${workId}`, {
            method: 'DELETE'
        })
            .then(response => {
                if (!response.ok) {
                    return response.text().then(text => {
                        throw new Error(`Server responded with status ${response.status}: ${text}`);
                    });
                }
                return response.json();
            })
            .then(data => {
                if (data.error) {
                    showError('Error: ' + data.error);
                } else {
                    // Visa bekräftelsemeddelande (inte error men samma funktion)
                    showError('Book successfully removed from bookshelf');

                    // Efter en kort paus, ta bort boken från DOM
                    setTimeout(() => {
                        const bookElement = document.getElementById(`book-${bookshelfId}-${workId.replace(/\//g, '-')}`);
                        if (bookElement) {
                            bookElement.remove();
                        }

                        const booksContainer = document.getElementById(`books-${bookshelfId}`);
                        if (booksContainer.querySelectorAll('.book-item').length === 0) {
                            booksContainer.innerHTML = `
                            <div class="empty-shelf-message">
                                <p>This bookshelf is empty. Use the "Add Book" button in the ✏️ menu to add a book! .</p>
                            </div>
                        `;
                        }
                    }, 1000);
                }
            })
            .catch(error => {
                console.error('Error:', error);
                showError('An error occurred: ' + error);
            });
    });
}

function updateDescription(event) {
    // Förhindra standardbeteendet (formulärskickning)
    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }

    const shelfId = document.getElementById("editShelfId").value;
    const description = document.getElementById("editShelfDescription").value;

    fetch('/profile/bookshelves/updateDescription', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams({
            shelfId: shelfId,
            description: description
        })
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.text();
        })
        .then(data => {
            // Stäng modalen
            const modal = bootstrap.Modal.getInstance(document.getElementById('editDescriptionModal'));
            if (modal) {
                modal.hide();
            }

            // Visa framgångsmeddelande
            if (typeof showError === 'function') {
                showError('Description updated successfully!');
            } else {
                alert(data);
            }

            // Uppdatera beskrivningen i DOM utan att ladda om hela sidan
            setTimeout(() => {
                const descriptionElement = document.querySelector(`#bookshelf-${shelfId} .shelf-description`);
                if (descriptionElement) {
                    descriptionElement.textContent = description || 'No description available';
                }
            }, 1000); // Kortare tid eftersom vi inte laddar om sidan
        })
        .catch(error => {
            console.error("Error:", error);

            // Visa felmeddelande
            if (typeof showError === 'function') {
                showError('Failed to update description: ' + error.message);
            } else {
                alert('Error updating description: ' + error.message);
            }
        });

    return false; // Extra säkerhet för att förhindra formulärskickning
}


function showEditDescriptionModal(button) {
    const shelfId = button.getAttribute('data-shelf-id');

    // Hämta den aktuella beskrivningen direkt från DOM
    const descriptionElement = document.querySelector(`#bookshelf-${shelfId} .shelf-description`);
    let currentDescription = '';

    if (descriptionElement) {
        currentDescription = descriptionElement.textContent.trim();
        // Om det visar "No description available", använd tom sträng istället
        if (currentDescription === 'No description available') {
            currentDescription = '';
        }
    }

    document.getElementById('editShelfId').value = shelfId;
    document.getElementById('editShelfDescription').value = currentDescription;

    const modal = new bootstrap.Modal(document.getElementById('editDescriptionModal'));
    modal.show();
}



// Function to show the modal for renaming a bookshelf
function showRenameShelfModal(bookshelfId, currentName) {
    document.getElementById('renameShelfId').value = bookshelfId;
    document.getElementById('newShelfName').value = currentName;

    const modal = new bootstrap.Modal(document.getElementById('renameShelfModal'));
    modal.show();
}

// Function to rename a bookshelf
const renameShelfButton = document.getElementById('renameShelfButton');
if (renameShelfButton) {
    renameShelfButton.addEventListener('click', function () {
        const bookshelfId = document.getElementById('renameShelfId').value;
        const newName = document.getElementById('newShelfName').value.trim();

        if (newName) {
            fetch(`/profile/bookshelves/${bookshelfId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: `name=${encodeURIComponent(newName)}`
            })
                .then(response => response.json())
                .then(data => {
                    if (data.error) {
                        showError('Error: ' + data.error);
                    } else {
                        // Close the modal
                        const modal = bootstrap.Modal.getInstance(document.getElementById('renameShelfModal'));
                        modal.hide();

                        // Update the name in the DOM
                        const titleElement = document.querySelector(`#bookshelf-${bookshelfId} .bookshelf-title`);
                        if (titleElement) {
                            titleElement.textContent = newName;
                        }
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    showError('An error occurred: ' + error);
                });
        } else {
            showError('Please enter a name for the bookshelf');
        }
    });
}

// Function to confirm bookshelf deletion
function confirmDeleteShelf(bookshelfId) {
    showConfirm('Are you sure you want to delete this bookshelf and all its books?', function() {
        fetch(`/profile/bookshelves/${bookshelfId}`, {
            method: 'DELETE'
        })
            .then(response => response.json())
            .then(data => {
                if (data.error) {
                    showError('Error: ' + data.error);
                } else {
                    // Visa bekräftelsemeddelande
                    showError('Bookshelf successfully deleted');

                    // Efter en kort paus, ta bort bokhyllan från DOM
                    setTimeout(() => {
                        // Remove the bookshelf from the DOM
                        const shelfElement = document.getElementById(`bookshelf-${bookshelfId}`);
                        if (shelfElement) {
                            shelfElement.remove();
                        }

                        // If no bookshelves remain, show message
                        const shelvesContainer = document.getElementById('bookshelves-container');
                        if (shelvesContainer.querySelectorAll('.bookshelf-container').length === 0) {
                            shelvesContainer.innerHTML = `
                            <div class="empty-shelf-message">
                                <p>You don't have any bookshelves yet. Click "Create New Bookshelf" to get started!</p>
                            </div>
                        `;
                        }
                    }, 1000);
                }
            })
            .catch(error => {
                console.error('Error:', error);
                showError('An error occurred: ' + error);
            });
    });
}

// Function to delete a bookshelf
const confirmDeleteShelfButton = document.getElementById('confirmDeleteShelfButton');
if (confirmDeleteShelfButton) {
    confirmDeleteShelfButton.addEventListener('click', function () {
        const bookshelfId = document.getElementById('deleteShelfId').value;

        fetch(`/profile/bookshelves/${bookshelfId}`, {
            method: 'DELETE'
        })
            .then(response => response.json())
            .then(data => {
                if (data.error) {
                    showError('Error: ' + data.error);
                } else {
                    // Close the modal
                    const modal = bootstrap.Modal.getInstance(document.getElementById('deleteShelfModal'));
                    modal.hide();

                    // Remove the bookshelf from the DOM
                    const shelfElement = document.getElementById(`bookshelf-${bookshelfId}`);
                    if (shelfElement) {
                        shelfElement.remove();
                    }

                    // If no bookshelves remain, show message
                    const shelvesContainer = document.getElementById('bookshelves-container');
                    if (shelvesContainer.querySelectorAll('.bookshelf-container').length === 0) {
                        shelvesContainer.innerHTML = `
                    <div class="empty-shelf-message">
                        <p>You don't have any bookshelves yet. Click "Create New Bookshelf" to get started!</p>
                    </div>
                `;
                    }
                }
            })
            .catch(error => {
                console.error('Error:', error);
                showError('An error occurred: ' + error);
            });
    });
}

// Hantera Enter-tryck för alla relevanta inmatningsfält som inte har det automatiskt
document.addEventListener('DOMContentLoaded', function() {
    // För boksökningsfältet i profile
    const bookSearchInput = document.getElementById('bookSearch');
    if (bookSearchInput) {
        bookSearchInput.addEventListener('keydown', function(event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                document.getElementById('searchBookButton').click();
            }
        });
    }

    // För bokhyllans namnfält vid skapande av bokhylla
    const shelfNameInput = document.getElementById('shelfName');
    if (shelfNameInput) {
        shelfNameInput.addEventListener('keydown', function(event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                document.getElementById('saveShelfButton').click();
            }
        });
    }

    // För namnbytesfältet för bokhyllan
    const newShelfNameInput = document.getElementById('newShelfName');
    if (newShelfNameInput) {
        newShelfNameInput.addEventListener('keydown', function(event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                document.getElementById('renameShelfButton').click();
            }
        });
    }
});

// Funktion för att hantera "Add to Bookshelf" från boksidan
document.addEventListener('DOMContentLoaded', function() {
    console.log("DOM fully loaded - checking for bookshelf functionality");

    // Kontrollera om vi är på boksidan och om relevanta element finns
    const addToBookshelfBtn = document.getElementById('addToBookshelfBtn');
    const bookshelfModal = document.getElementById('bookshelfModal');

    console.log("Found addToBookshelfBtn:", !!addToBookshelfBtn);
    console.log("Found bookshelfModal:", !!bookshelfModal);

    // Om knappen inte finns är vi troligen inte på boksidan, så avsluta tidigt
    if (!addToBookshelfBtn || !bookshelfModal) return;

    const bookshelfListContainer = document.getElementById('bookshelfListContainer');
    const doneBtn = document.getElementById('doneBtn');
    const closeModalBtn = document.querySelector('.close-modal');

    // Bokens ID från URL
    const workId = window.location.pathname.split('/').pop();
    const selectedBookshelves = new Set();

    // Öppna modalen när användaren klickar på Add to Bookshelf-knappen
    addToBookshelfBtn.addEventListener('click', function() {
        bookshelfModal.style.display = 'block';
        loadBookshelves();
    });

    // Stäng modalen
    if (closeModalBtn) {
        closeModalBtn.addEventListener('click', function() {
            bookshelfModal.style.display = 'none';
        });
    }

    // Stäng modalen om användaren klickar utanför innehållet
    window.addEventListener('click', function(event) {
        if (event.target === bookshelfModal) {
            bookshelfModal.style.display = 'none';
        }
    });

    // Klicka på Done-knappen för att lägga till boken i valda bokhyllor
    if (doneBtn) {
        doneBtn.addEventListener('click', function() {
            if (selectedBookshelves.size === 0) {
                bookshelfModal.style.display = 'none';
                return;
            }

            addBookToBookshelves(Array.from(selectedBookshelves), workId);
        });
    }

    // Hämta användarens bokhyllor via AJAX
    function loadBookshelves() {
        bookshelfListContainer.innerHTML = '<div class="loading-spinner">Loading bookshelves...</div>';

        fetch('/profile/bookshelves')
            .then(response => {
                if (!response.ok) {
                    throw new Error('Failed to load bookshelves');
                }
                return response.json();
            })
            .then(bookshelves => {
                displayBookshelves(bookshelves);
            })
            .catch(error => {
                if (typeof showError === 'function') {
                    showError(error.message);
                } else {
                    bookshelfListContainer.innerHTML = `<div class="error">Error: ${error.message}</div>`;
                }
            });
    }

// Uppdaterad displayBookshelves funktion med "Add to New Bookshelf" och "Add your first bookshelf" alternativ
    function displayBookshelves(bookshelves) {
        if (bookshelves.length === 0) {
            // Visa formulär för att skapa första bokhyllan (samma som innan)
            bookshelfListContainer.innerHTML = `
            <div class="first-bookshelf-container">
                <p>You don't have any bookshelves yet.</p>
                <p>Create your first bookshelf and we'll add this book to it automatically!</p>
                
                <div class="create-first-shelf-form">
                    <input type="text" id="firstShelfName" placeholder="Enter bookshelf name" class="form-control mb-2">
                    <textarea id="firstShelfDescription" placeholder="Description (optional)" class="form-control mb-2" rows="2"></textarea>
                    <button id="createFirstShelfBtn" class="btn-rose">Create Bookshelf & Add Book</button>
                </div>
            </div>
        `;

            setupFirstBookshelfForm();
            return;
        }

        bookshelfListContainer.innerHTML = ''; // Rensa tidigare innehåll

        // Lägg till "Add to New Bookshelf" sektion först
        const newBookshelfSection = document.createElement('div');
        newBookshelfSection.className = 'new-bookshelf-section';
        newBookshelfSection.innerHTML = `
        <div class="new-bookshelf-option">
            <button id="addToNewShelfBtn" class="btn-new-shelf">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-plus-circle me-2" viewBox="0 0 16 16">
                    <path d="M8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14zm0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16z"/>
                    <path d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4z"/>
                </svg>
                Add to New Bookshelf
            </button>
        </div>
        
        <div id="newBookshelfForm" class="new-bookshelf-form" style="display: none;">
            <input type="text" id="newShelfName" placeholder="Enter bookshelf name" class="form-control mb-2">
            <textarea id="newShelfDescription" placeholder="Description (optional)" class="form-control mb-2" rows="2"></textarea>
            <div class="form-buttons">
                <button id="createNewShelfBtn" class="btn-rose">Create & Add Book</button>
                <button id="cancelNewShelfBtn" class="btn-secondary">Cancel</button>
            </div>
        </div>
    `;

        bookshelfListContainer.appendChild(newBookshelfSection);

        // Lägg till divider
        const divider = document.createElement('div');
        divider.className = 'bookshelf-divider';
        divider.innerHTML = '<hr><p class="divider-text">Or choose existing bookshelf:</p>';
        bookshelfListContainer.appendChild(divider);

        // Skapa checkboxar för befintliga bokhyllor
        bookshelves.forEach(shelf => {
            const shelfItem = document.createElement('div');
            shelfItem.className = 'bookshelf-item';

            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.id = `shelf-${shelf.id}`;
            checkbox.value = shelf.id;
            checkbox.addEventListener('change', function() {
                if (this.checked) {
                    selectedBookshelves.add(this.value);
                } else {
                    selectedBookshelves.delete(this.value);
                }
            });

            const label = document.createElement('label');
            label.htmlFor = `shelf-${shelf.id}`;
            label.className = 'bookshelf-name';
            label.textContent = shelf.name;

            shelfItem.appendChild(checkbox);
            shelfItem.appendChild(label);
            bookshelfListContainer.appendChild(shelfItem);
        });

        // Setup event listeners för "Add to New Bookshelf"
        setupNewBookshelfForm();
    }

// Hjälpfunktion för att sätta upp första bokhylle-formuläret
    function setupFirstBookshelfForm() {
        const createFirstShelfBtn = document.getElementById('createFirstShelfBtn');
        const firstShelfNameInput = document.getElementById('firstShelfName');

        // Enter-tangent support
        firstShelfNameInput.addEventListener('keydown', function(event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                createFirstShelfBtn.click();
            }
        });

        createFirstShelfBtn.addEventListener('click', function() {
            const shelfName = firstShelfNameInput.value.trim();
            const shelfDescription = document.getElementById('firstShelfDescription').value.trim();

            if (!shelfName) {
                if (typeof showError === 'function') {
                    showError('Please enter a name for your bookshelf');
                } else {
                    alert('Please enter a name for your bookshelf');
                }
                return;
            }

            createBookshelfAndAddBook(shelfName, shelfDescription, workId);
        });
    }

// Hjälpfunktion för att sätta upp "Add to New Bookshelf" formuläret
    function setupNewBookshelfForm() {
        const addToNewShelfBtn = document.getElementById('addToNewShelfBtn');
        const newBookshelfForm = document.getElementById('newBookshelfForm');
        const createNewShelfBtn = document.getElementById('createNewShelfBtn');
        const cancelNewShelfBtn = document.getElementById('cancelNewShelfBtn');
        const newShelfNameInput = document.getElementById('newShelfName');

        // Visa/dölj formuläret
        addToNewShelfBtn.addEventListener('click', function() {
            if (newBookshelfForm.style.display === 'none') {
                newBookshelfForm.style.display = 'block';
                addToNewShelfBtn.textContent = 'Hide New Bookshelf Form';
                newShelfNameInput.focus();
            } else {
                newBookshelfForm.style.display = 'none';
                addToNewShelfBtn.innerHTML = `
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-plus-circle me-2" viewBox="0 0 16 16">
                    <path d="M8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14zm0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16z"/>
                    <path d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4z"/>
                </svg>
                Add to New Bookshelf
            `;
            }
        });

        // Enter-tangent support
        newShelfNameInput.addEventListener('keydown', function(event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                createNewShelfBtn.click();
            }
        });

        // Skapa ny bokhylla
        createNewShelfBtn.addEventListener('click', function() {
            const shelfName = newShelfNameInput.value.trim();
            const shelfDescription = document.getElementById('newShelfDescription').value.trim();

            if (!shelfName) {
                if (typeof showError === 'function') {
                    showError('Please enter a name for your bookshelf');
                } else {
                    alert('Please enter a name for your bookshelf');
                }
                return;
            }

            createBookshelfAndAddBook(shelfName, shelfDescription, workId);
        });

        // Avbryt
        cancelNewShelfBtn.addEventListener('click', function() {
            newBookshelfForm.style.display = 'none';
            addToNewShelfBtn.innerHTML = `
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-plus-circle me-2" viewBox="0 0 16 16">
                <path d="M8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14zm0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16z"/>
                <path d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4z"/>
            </svg>
            Add to New Bookshelf
        `;

            // Rensa formuläret
            newShelfNameInput.value = '';
            document.getElementById('newShelfDescription').value = '';
        });
    }

// Ombenämnd och generell funktion för att skapa bokhylla och lägga till bok
    function createBookshelfAndAddBook(shelfName, shelfDescription, bookWorkId) {
        // Hitta vilken knapp som används (första eller nya bokhylla)
        const createBtn = document.getElementById('createFirstShelfBtn') || document.getElementById('createNewShelfBtn');
        const originalText = createBtn.textContent;

        // Visa laddningsindikator
        createBtn.disabled = true;
        createBtn.textContent = 'Creating...';

        // Skapa bokhyllan först
        const body = `name=${encodeURIComponent(shelfName)}&description=${encodeURIComponent(shelfDescription)}`;

        fetch('/profile/bookshelves', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: body
        })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(errorData => {
                        throw new Error(errorData.errorMessage || 'Failed to create bookshelf');
                    });
                }
                return response.json();
            })
            .then(data => {
                // Bokhyllan skapad! Nu lägger vi till boken automatiskt
                const newBookshelfId = data.id;

                return fetch(`/profile/bookshelves/${newBookshelfId}/books`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: `workId=${encodeURIComponent(bookWorkId)}`
                });
            })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(errorData => {
                        throw new Error(errorData.errorMessage || 'Failed to add book to bookshelf');
                    });
                }
                return response.json();
            })
            .then(data => {
                // Framgång! Stäng modalen och visa bekräftelse
                bookshelfModal.style.display = 'none';

                if (typeof showError === 'function') {
                    showError(`Bookshelf "${shelfName}" created and book added successfully!`);
                } else {
                    alert(`Bookshelf "${shelfName}" created and book added successfully!`);
                }
            })
            .catch(error => {
                console.error('Error:', error);

                if (typeof showError === 'function') {
                    showError('Error: ' + error.message);
                } else {
                    alert('Error: ' + error.message);
                }
            })
            .finally(() => {
                // Återställ knappen
                createBtn.disabled = false;
                createBtn.textContent = originalText;
            });
    }

    // Lägg till boken i de valda bokhyllorna
    function addBookToBookshelves(shelfIds, bookId) {
        // Visa laddningsindikator
        doneBtn.disabled = true;
        doneBtn.textContent = 'Adding...';

        // Skapa en array av promises för varje bokhylla som ska uppdateras
        const addPromises = shelfIds.map(shelfId => {
            return fetch(`/profile/bookshelves/${shelfId}/books`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: `workId=${bookId}`
            })
                .then(response => {
                    if (!response.ok) {
                        return response.json().then(err => {
                            throw new Error(err.errorMessage || 'Failed to add book to bookshelf');
                        });
                    }
                    return response.json();
                });
        });

        // Vänta på att alla promises är klara
        Promise.all(addPromises)
            .then(() => {
                // Stäng modalen och visa bekräftelse
                bookshelfModal.style.display = 'none';
                if (typeof showConfirm === 'function') {
                    showError('Book added to selected bookshelves successfully!'); //Ej error men används ändå
                } else {
                    alert('Book added to selected bookshelves successfully!');
                }
            })
            .catch(error => {
                if (typeof showError === 'function') {
                    showError(error.message);
                } else {
                    alert('Error: ' + error.message);
                }
            })
            .finally(() => {
                // Återställ knappen
                doneBtn.disabled = false;
                doneBtn.textContent = 'Done';
                selectedBookshelves.clear();
            });
    }
});

document.addEventListener("DOMContentLoaded", function () {
    handleVisibilityToggle();
});

function handleVisibilityToggle() {
    const toggleButtons = document.querySelectorAll(".toggle-visibility-btn");

    toggleButtons.forEach(button => {
        // Ta bort befintliga event listeners
        button.removeEventListener("click", handleToggleClick);
        button.addEventListener("click", handleToggleClick);
    });
}

function handleToggleClick(event) {
    event.preventDefault();
    event.stopPropagation();

    const button = this;
    const shelfId = button.dataset.shelfId;
    const isCurrentlyPublic = button.dataset.shelfPublic === "true";

    fetch(`/profile/bookshelves/${shelfId}/toggle-visibility`, {
        method: "POST"
    }).then(response => {
        if (response.ok) {
            const newIsPublic = !isCurrentlyPublic;
            button.dataset.shelfPublic = newIsPublic.toString();

            // Uppdatera ikonen och texten
            const icon = button.querySelector('svg');
            const text = button.querySelector('span');

            if (newIsPublic) {
                icon.innerHTML = `
                    <path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8zM1.173 8a13.133 13.133 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13.133 13.133 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755C11.879 11.332 10.119 12.5 8 12.5c-2.12 0-3.879-1.168-5.168-2.457A13.134 13.134 0 0 1 1.172 8z"/>
                    <path d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5zM4.5 8a3.5 3.5 0 1 1 7 0 3.5 3.5 0 0 1-7 0z"/>
                `;
                text.textContent = 'Make Private';
            } else {
                icon.innerHTML = `
                    <path d="M13.359 11.238C15.06 9.72 16 8 16 8s-3-5.5-8-5.5a7.028 7.028 0 0 0-2.79.588l.77.771A5.944 5.944 0 0 1 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13.134 13.134 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755-.165.165-.337.328-.517.486l.708.709z"/>
                    <path d="M11.297 9.176a3.5 3.5 0 0 0-4.474-4.474l.823.823a2.5 2.5 0 0 1 2.829 2.829l.822.822zm-2.943 1.299.822.822a3.5 3.5 0 0 1-4.474-4.474l.823.823a2.5 2.5 0 0 0 2.829 2.829z"/>
                    <path d="M3.35 5.47c-.18.16-.353.322-.518.487A13.134 13.134 0 0 0 1.172 8l.195.288c.335.48.83 1.12 1.465 1.755C4.121 11.332 5.881 12.5 8 12.5c.716 0 1.39-.133 2.02-.36l.77.772A7.029 7.029 0 0 1 8 13.5C3 13.5 0 8 0 8s.939-1.721 2.641-3.238l.708.709zm10.296 8.884-12-12 .708-.708 12 12-.708.708z"/>
                `;
                text.textContent = 'Make Public';
            }

            // Stäng dropdown
            const dropdown = button.closest('.dropdown');
            const dropdownBtn = dropdown.querySelector('[data-bs-toggle="dropdown"]');
            if (dropdownBtn) {
                const bsDropdown = bootstrap.Dropdown.getInstance(dropdownBtn);
                if (bsDropdown) {
                    bsDropdown.hide();
                }
            }

        } else {
            console.error("Failed to toggle visibility. Status: " + response.status);
            if (typeof showError === 'function') {
                showError("Failed to toggle bookshelf visibility");
            } else {
                alert("Failed to toggle visibility. Status: " + response.status);
            }
        }
    }).catch(error => {
        console.error("Error:", error);
        if (typeof showError === 'function') {
            showError("Error toggling visibility: " + error.message);
        } else {
            alert("Error toggling visibility.");
        }
    });
}