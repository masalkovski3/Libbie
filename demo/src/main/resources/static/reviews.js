/**
 * Recensionsfunktionalitet för Libbie bokapplikation
 * Hanterar visning, tillägg, redigering och borttagning av bokrecensioner
 */
document.addEventListener('DOMContentLoaded', function() {
    // Kontrollera om vi är på en boksida som behöver recensionsfunktionalitet
    if (!document.getElementById('reviewsList')) {
        return; // Avsluta om vi inte är på en boksida
    }

    // Hämta data från window.reviewData som ställts in från Thymeleaf i book.html
    const { workId, currentMember, userReview } = window.reviewData;

    // Element-referenser
    const reviewsList = document.getElementById('reviewsList');
    const noReviewsMessage = document.getElementById('noReviewsMessage');
    const reviewCount = document.getElementById('reviewCount');
    const avgScore = document.getElementById('avgScore');
    const avgStars = document.getElementById('avgStars');

    // Recensionsformulär (om användaren är inloggad)
    const reviewForm = document.getElementById('reviewForm');
    const ratingStars = reviewForm ? document.getElementById('ratingStars') : null;
    const reviewText = reviewForm ? document.getElementById('reviewText') : null;
    const submitButton = reviewForm ? document.getElementById('submitReview') : null;
    const cancelButton = reviewForm ? document.getElementById('cancelEdit') : null;
    const formTitle = reviewForm ? document.getElementById('reviewFormTitle') : null;

    let currentRating = 0;
    let isEditing = false;

    // Läs in recensioner via AJAX
    loadReviews();

    // Om användaren är inloggad, konfigurera recensionsformuläret
    if (reviewForm) {
        // Hantera stjärnbetyg
        const stars = ratingStars.querySelectorAll('.star');

        stars.forEach(star => {
            star.addEventListener('click', function() {
                const rating = parseInt(this.dataset.rating);
                currentRating = rating;

                // Uppdatera utseendet för stjärnorna
                stars.forEach(s => {
                    if (parseInt(s.dataset.rating) <= rating) {
                        s.textContent = '★'; // Fylld stjärna
                        s.classList.add('selected');
                    } else {
                        s.textContent = '☆'; // Tom stjärna
                        s.classList.remove('selected');
                    }
                });
            });
        });

        // Rensa textarea om värdet är placeholder-texten vid fokus
        if (reviewText) {
            reviewText.addEventListener('focus', function() {
                if (this.value === 'Write your review here...') {
                    this.value = '';
                }
            });
        }

        // Om användaren redan har en recension, fyll i formuläret
        if (userReview) {
            populateUserReview(userReview);
        }

        // Hantera formulärinskickning
        submitButton.addEventListener('click', function() {
            if (currentRating === 0) {
                showError('Please select a rating');
                return;
            }

            const reviewContent = reviewText.value.trim();
            if (reviewContent === '') {
                showError('Please write a review');
                return;
            }

            submitReview(currentRating, reviewContent);
        });

        // Hantera avbrytande av redigering
        cancelButton.addEventListener('click', function() {
            resetForm();
            if (userReview) {
                populateUserReview(userReview);
            }
        });
    }

    /**
     * Hämtar alla recensioner för boken från servern
     */
    function loadReviews() {
        fetch(`/reviews/book/${workId}`)
            .then(response => {
                if (!response.ok) {
                    return response.json().then(err => {
                        throw new Error(err.error || 'Failed to load reviews');
                    });
                }
                return response.json();
            })
            .then(data => {
                displayReviews(data.reviews);
                updateAverageScore(data.averageScore);
            })
            .catch(error => {
                // Använd showError från script.js
                if (typeof showError === 'function') {
                    showError(error.message);
                } else {
                    console.error('Error loading reviews:', error.message);
                }
            });
    }

    /**
     * Skickar en ny recension eller uppdaterar en befintlig
     * @param {number} score - Betyg (1-5)
     * @param {string} text - Recensionstext
     */
    function submitReview(score, text) {
        const formData = new FormData();
        formData.append('score', score);
        formData.append('reviewText', text);

        fetch(`/reviews/book/${workId}`, {
            method: 'POST',
            body: formData
        })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(err => {
                        throw new Error(err.error || 'Failed to submit review');
                    });
                }
                return response.json();
            })
            .then(data => {
                displayReviews(data.reviews);
                updateAverageScore(data.averageScore);

                // Uppdatera användarens recension
                window.reviewData.userReview = {
                    score: score,
                    text: text
                };

                // Återställ formuläret
                resetForm();
                reviewText.value = 'Write your review here...';
                if (window.reviewData.userReview && isEditing) {
                    populateUserReview(window.reviewData.userReview);
                }
            })
            .catch(error => {
                if (typeof showError === 'function') {
                    showError(error.message);
                } else {
                    console.error('Error submitting review:', error.message);
                }
            });
    }

    /**
     * Tar bort en recension
     */
    function deleteReview() {
        // Direktanvändning av showConfirm utan mellanliggande ping-begäran
        if (typeof showConfirm === 'function') {
            // Åsidosätt eventuell returnvärdehantering och använd endast callback
            showConfirm('Are you sure you want to delete your review?', performDeleteRequest);
            // Viktigt: Hindra vidare exekvering
            return false;
        } else {
            // Fallback till standard confirm
            if (confirm('Are you sure you want to delete your review?')) {
                performDeleteRequest();
            }
        }
    }

// Separera ut den faktiska delete-requesten till en egen funktion
    function performDeleteRequest() {
        fetch(`/reviews/book/${window.reviewData.workId}`, {
            method: 'DELETE'
        })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(err => {
                        throw new Error(err.error || 'Failed to delete review');
                    });
                }
                return response.json();
            })
            .then(data => {
                displayReviews(data.reviews);
                updateAverageScore(data.averageScore);

                // Återställ formuläret
                resetForm();

                // Ta bort användarrecensionen
                window.reviewData.userReview = null;
            })
            .catch(error => {
                if (typeof showError === 'function') {
                    showError(error.message);
                } else {
                    console.error('Error deleting review:', error.message);
                }
            });
    }

    /**
     * Visar recensioner på sidan
     * @param {Array} reviews - Alla recensioner för boken
     */
    function displayReviews(reviews) {
        // Rensa nuvarande recensioner
        reviewsList.innerHTML = '';

        // Uppdatera räknaren
        reviewCount.textContent = `(${reviews.length})`;

        // Visa meddelande om det inte finns några recensioner
        if (reviews.length === 0) {
            noReviewsMessage.style.display = 'block';
        } else {
            noReviewsMessage.style.display = 'none';

            // Skapa recensionselement för varje recension
            reviews.forEach(review => {
                const reviewElement = document.createElement('div');
                reviewElement.className = 'review-item';

                // Använd default-avatar om profilbild saknas
                const profileImgSrc = review.profileImage ? review.profileImage : '/images/blue-logo.jpeg';

                // Skapa stjärnor baserat på betyg
                let stars = '';
                for (let i = 1; i <= 5; i++) {
                    stars += i <= review.score ? '★' : '☆';
                }

                // Kontrollera om detta är användarens egen recension
                const isUserReview = currentMember && review.memberName === currentMember.name;

                // Bygg recensionens HTML
                reviewElement.innerHTML = `
                    <div class="review-avatar">
                        <img src="${profileImgSrc}" alt="${review.memberName}'s profile picture">
                    </div>
                    <div class="review-content">
                        <div class="review-meta">
                            <span class="review-author">${review.memberName}</span>
                            <span class="review-date">${review.formattedDate || ''}</span>
                        </div>
                        <div class="review-stars">${stars}</div>
                        <p>${review.text}</p>
                        ${isUserReview ? `
                            <div class="review-actions">
                                <button class="btn-edit">Edit</button>
                                <button class="btn-delete">Delete</button>
                            </div>
                        ` : ''}
                    </div>
                `;

                // Lägg till eventlyssnare för redigering och borttagning om det är användarens recension
                if (isUserReview) {
                    const editButton = reviewElement.querySelector('.btn-edit');
                    const deleteButton = reviewElement.querySelector('.btn-delete');

                    editButton.addEventListener('click', function() {
                        editReview(review);
                    });

                    deleteButton.addEventListener('click', function() {
                        deleteReview();
                    });
                }

                reviewsList.appendChild(reviewElement);
            });
        }
    }

    /**
     * Uppdaterar det genomsnittliga betyget och visar stjärnor
     * @param {number} score - Genomsnittligt betyg
     */
    function updateAverageScore(score) {
        // Visa genomsnittligt betyg med en decimal
        avgScore.textContent = score.toFixed(1);

        // Skapa stjärnrepresentation
        let starsHtml = '';
        const fullStars = Math.floor(score);
        const hasHalfStar = score - fullStars >= 0.25 && score - fullStars < 0.75;
        const hasFullStar = score - fullStars >= 0.75;

        for (let i = 1; i <= 5; i++) {
            if (i <= fullStars) {
                starsHtml += '★'; // Fylld stjärna
            } else if (i === fullStars + 1 && hasHalfStar) {
                starsHtml += '⯨'; // Halv stjärna
            } else if (i === fullStars + 1 && hasFullStar) {
                starsHtml += '★'; // Fylld stjärna
            } else {
                starsHtml += '☆'; // Tom stjärna
            }
        }

        avgStars.innerHTML = starsHtml;
    }

    /**
     * Sätter formuläret i redigeringsläge för en befintlig recension
     * @param {Object} review - Recensionen som ska redigeras
     */
    function editReview(review) {
        // Växla till redigeringsläge
        isEditing = true;
        formTitle.textContent = 'Edit Your Review';
        submitButton.textContent = 'Update Review';
        cancelButton.style.display = 'inline-block';

        // Fyll i formuläret med nuvarande värden
        currentRating = review.score;
        reviewText.value = review.text;

        // Uppdatera stjärnorna
        const stars = ratingStars.querySelectorAll('.star');
        stars.forEach(star => {
            const rating = parseInt(star.dataset.rating);
            if (rating <= currentRating) {
                star.textContent = '★'; // Fylld stjärna
                star.classList.add('selected');
            } else {
                star.textContent = '☆'; // Tom stjärna
                star.classList.remove('selected');
            }
        });

        // Scrolla till formuläret
        reviewForm.scrollIntoView({ behavior: 'smooth' });
    }

    /**
     * Fyller i formuläret med användarens befintliga recension
     * @param {Object} review - Användarens befintliga recension
     */
    function populateUserReview(review) {
        if (!review) return;

        currentRating = review.score;
        reviewText.value = review.text;

        // Uppdatera stjärnorna
        const stars = ratingStars.querySelectorAll('.star');
        stars.forEach(star => {
            const rating = parseInt(star.dataset.rating);
            if (rating <= currentRating) {
                star.textContent = '★'; // Fylld stjärna
                star.classList.add('selected');
            } else {
                star.textContent = '☆'; // Tom stjärna
                star.classList.remove('selected');
            }
        });

        // Uppdatera formulärrubriken
        formTitle.textContent = 'Edit Your Review';
        submitButton.textContent = 'Update Review';
    }

    /**
     * Återställer formuläret till standardläge
     */
    function resetForm() {
        // Återställ formuläret till standardtillstånd
        isEditing = false;
        formTitle.textContent = 'Add Your Review';
        submitButton.textContent = 'Submit Review';
        cancelButton.style.display = 'none';

        // Rensa värden
        currentRating = 0;
        reviewText.value = '';

        // Återställ stjärnor
        const stars = ratingStars.querySelectorAll('.star');
        stars.forEach(star => {
            star.textContent = '☆'; // Tom stjärna
            star.classList.remove('selected');
        });
    }
});