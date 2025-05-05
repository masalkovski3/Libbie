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
document.getElementById('saveShelfButton').addEventListener('click', function() {
    const shelfName = document.getElementById('shelfName').value.trim();

    if (shelfName) {
        fetch('/profile/bookshelves', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: `name=${encodeURIComponent(shelfName)}`
        })
            .then(response => {
                // First check if the response is OK
                if (!response.ok) {
                    // If not, try to parse the JSON error message
                    return response.json().then(errorData => {
                        throw new Error(errorData.errorMessage || 'An error occurred');
                    });
                }
                return response.json();
            })
            .then(data => {
                // This code only runs if the response was OK
                const modal = bootstrap.Modal.getInstance(document.getElementById('addShelfModal'));
                modal.hide();
                location.reload();
            })
            .catch(error => {
                console.error('Error:', error);
                showError(error.message);
            });
    } else {
        showError('Please enter a name for the bookshelf');
    }
});

// Function to show the modal for adding a book
function showAddBookModal(bookshelfId) {
    document.getElementById('selectedBookshelfId').value = bookshelfId;
    document.getElementById('bookSearch').value = '';
    document.getElementById('searchResults').innerHTML = '';

    const modal = new bootstrap.Modal(document.getElementById('addBookModal'));
    modal.show();
}

// Search for books using the API
document.getElementById('searchBookButton').addEventListener('click', function() {
    const query = document.getElementById('bookSearch').value.trim();
    const resultsContainer = document.getElementById('searchResults');

    if (query) {
        resultsContainer.innerHTML = '<p>Searching...</p>';

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
                    resultItem.onclick = function() {
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
        alert('Please enter a search term');
    }
});

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
                alert('Error: ' + data.error);
            } else {
                // Close the modal
                const modal = bootstrap.Modal.getInstance(document.getElementById('addBookModal'));
                modal.hide();

                // Reload the page to show the new book
                location.reload();
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('An error occurred: ' + error);
        });
}

// Function to remove a book from a bookshelf
function removeBookFromShelf(bookshelfId, workId) {
    if (confirm('Are you sure you want to remove this book from the bookshelf?')) {
        fetch('/profile/bookshelves/1/books/OL12345W', {
            method: 'DELETE'
        })


            .then(response => {
                if (!response.ok) {
                    // Om det inte är ett lyckat svar (status 200–299), kasta fel
                    return response.text().then(text => {
                        throw new Error(`Server responded with status ${response.status}: ${text}`);
                    });
                }
                return response.json(); // Endast om det verkligen är JSON
            })
            .then(data => {
                if (data.error) {
                    alert('Error: ' + data.error);
                } else {
                    const bookElement = document.getElementById(`book-${bookshelfId}-${workId.replace(/\//g, '-')}`);
                    if (bookElement) {
                        bookElement.remove();
                    }

                    const booksContainer = document.getElementById(`books-${bookshelfId}`);
                    if (booksContainer.querySelectorAll('.book-item').length === 0) {
                        booksContainer.innerHTML = `
                    <div class="empty-shelf-message">
                        <p>This bookshelf is empty. Use the "Add Book" button above to add books.</p>
                    </div>
                `;
                    }
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert('An error occurred: ' + error.message);
            });
    }
}

// Function to show the modal for renaming a bookshelf
function showRenameShelfModal(bookshelfId, currentName) {
    document.getElementById('renameShelfId').value = bookshelfId;
    document.getElementById('newShelfName').value = currentName;

    const modal = new bootstrap.Modal(document.getElementById('renameShelfModal'));
    modal.show();
}

// Function to rename a bookshelf
document.getElementById('renameShelfButton').addEventListener('click', function() {
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
                    alert('Error: ' + data.error);
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
                alert('An error occurred: ' + error);
            });
    } else {
        alert('Please enter a name for the bookshelf');
    }
});

// Function to confirm bookshelf deletion
function confirmDeleteShelf(bookshelfId) {
    document.getElementById('deleteShelfId').value = bookshelfId;
    const modal = new bootstrap.Modal(document.getElementById('deleteShelfModal'));
    modal.show();
}

// Function to delete a bookshelf
document.getElementById('confirmDeleteShelfButton').addEventListener('click', function() {
    const bookshelfId = document.getElementById('deleteShelfId').value;

    fetch(`/profile/bookshelves/${bookshelfId}`, {
        method: 'DELETE'
    })
        .then(response => response.json())
        .then(data => {
            if (data.error) {
                alert('Error: ' + data.error);
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
            alert('An error occurred: ' + error);
        });
});