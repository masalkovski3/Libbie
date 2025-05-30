document.addEventListener('DOMContentLoaded', function() {
    const menuToggle = document.getElementById('menuToggle');
    const sidebar = document.getElementById('sidebar');

    if (menuToggle && sidebar) {
        menuToggle.addEventListener('click', function() {
            sidebar.classList.toggle('sidebar-hidden');
            menuToggle.classList.toggle('active');
        });
    }
});

function showPasswordErrorModal() {      //ta bort???
    $('#passwordErrorModal').modal('show');
}

document.addEventListener("DOMContentLoaded", function () {
    const form = document.querySelector("form");
    if (form) {
        form.addEventListener("submit", function (event) {
            const password = document.querySelector("#password-input").value;
            const repeatPassword = document.querySelector("#repeat-password-input").value;

            // Validering av lösenord
            if (!validatePassword(password)) {
                event.preventDefault(); // Förhindrar form submission om lösenordet är ogiltigt
                return; // Avbryt exekvering om lösenordet inte är giltigt
            }

            // Om lösenorden inte matchar, visa felmeddelande i modalen
            if (password !== repeatPassword) {
                showError("The passwords do not match.");
                event.preventDefault(); // Förhindrar form submission om lösenorden inte matchar
            }
        });
    }
});

function validatePassword(password) {
    var passwordStrong = true;
    var message = '';

    // Kontrollera lösenordslängd
    if (password.length < 12) {
        message += 'The password must contain at least 12 characters. \n';
        passwordStrong = false;
    }

    // Kontrollera om lösenordet innehåller stora bokstäver
    if (!/[A-Z]/.test(password)) {
        message += 'The password must contain at least one uppercase letter.\n';
        passwordStrong = false;
    }

    // Kontrollera om lösenordet innehåller små bokstäver
    if (!/[a-z]/.test(password)) {
        message += 'The password must contain at least one lowercase letter.\n';
        passwordStrong = false;
    }

    // Kontrollera om lösenordet innehåller siffror
    if (!/\d/.test(password)) {
        message += 'The password must contain at least one number.\n';
        passwordStrong = false;
    }

    // Om lösenordet inte är starkt, visa felmeddelandet i modalen
    if (!passwordStrong) {
        showError(message); // Använder showError istället för alert
        return false; // Returnera false för att förhindra vidare process
    }

    return true; // Om lösenordet är starkt
}


//funktion för felmeddelanden
function showError(message) {
    const modalMessage = document.getElementById("modalMessage");
    const modal = document.getElementById("errorModal");

    if (modal && modalMessage) {
        modalMessage.textContent = message;
        modal.style.display = "block";
    } else {
        console.error("Could not display error message: modal or modalMessage not found", message);
        // Fallback endast om DOM-element saknas - vi vill undvika alerts helt
        alert(message);
    }
}

function closeModal() {
    const modal = document.getElementById("errorModal");
    if (modal) {
        modal.style.display = "none";
    }
}

// Funktioner för bekräftelsedialog
function showConfirm(message, onConfirm) {
    const confirmMessage = document.getElementById("confirmMessage");
    const modal = document.getElementById("confirmModal");
    const yesButton = document.getElementById("confirmYesButton");

    if (modal && confirmMessage && yesButton) {
        confirmMessage.textContent = message;

        // Rensa tidigare händelsehanterare
        const newYesButton = yesButton.cloneNode(true);
        yesButton.parentNode.replaceChild(newYesButton, yesButton);

        // Lägg till ny händelsehanterare
        newYesButton.addEventListener('click', function() {
            closeConfirmModal();
            onConfirm();
        });

        modal.style.display = "block";
    } else {
        // Fallback till standard confirm
        if (confirm(message)) {
            onConfirm();
        }
    }
}

function closeConfirmModal() {
    const modal = document.getElementById("confirmModal");
    if (modal) {
        modal.style.display = "none";
    }
}

function setupSessionWarning(timeoutMinutes = 10, warningOffsetMinutes = 1) {
    const warningDelay = (timeoutMinutes - warningOffsetMinutes) * 60 * 1000;

    console.log("🕒 Starting timer for session warning after", warningDelay / 1000, "seconds");

    setTimeout(() => {
        const modal = document.getElementById("sessionModal");
        const stayBtn = document.getElementById("stayLoggedInBtn");
        const logoutBtn = document.getElementById("logOutNowBtn");

        if (!modal || !stayBtn || !logoutBtn) {
            console.warn("❌ Modal or buttons not found in DOM.");
            return;
        }

        modal.style.display = "flex";

        // Ta bort tidigare event handlers om de finns
        stayBtn.replaceWith(stayBtn.cloneNode(true));
        logoutBtn.replaceWith(logoutBtn.cloneNode(true));

        const newStayBtn = document.getElementById("stayLoggedInBtn");
        const newLogoutBtn = document.getElementById("logOutNowBtn");

        newStayBtn.addEventListener("click", () => {
            fetch("/ping", { method: "GET" })
                .then(() => {
                    console.log("🔁 Session extended");
                    modal.style.display = "none";
                    setupSessionWarning(timeoutMinutes, warningOffsetMinutes); // Starta om
                })
                .catch(error => {
                    console.error("⚠️ Failed to ping server:", error);
                });
        });

        newLogoutBtn.addEventListener("click", () => {
            console.log("🚪 User chose to log out");
            window.location.href = "/logout";
        });

    }, warningDelay);
}

document.addEventListener("DOMContentLoaded", function () {
    setupSessionWarning(10, 1);
});

if (typeof $ !== 'undefined' && $.fn.slick) {
    $(document).ready(function(){
        if ($('.carousel').length) {
            $('.carousel').slick({
                infinite: true,       // Oändlig scroll
                slidesToShow: 3,      // Visa tre böcker åt gången
                slidesToScroll: 1,    // Rulla en bok åt gången
                prevArrow: '<button type="button" class="slick-prev">←</button>',  // Föregående pil
                nextArrow: '<button type="button" class="slick-next">→</button>',  // Nästa pil
                responsive: [
                    {
                        breakpoint: 768,   // På skärmar mindre än 768px (mobil)
                        settings: {
                            slidesToShow: 1,  // Visa en bok åt gången
                            slidesToScroll: 1
                        }
                    }
                ]
            });
        }
    });

    $(document).ready(function () {
        if ($('.book-slider').length) {
            $('.book-slider').slick({
                slidesToShow: 4,
                slidesToScroll: 1,
                arrows: true,
                dots: true,
                responsive: [
                    {
                        breakpoint: 1024,
                        settings: {
                            slidesToShow: 3,
                        }
                    },
                    {
                        breakpoint: 768,
                        settings: {
                            slidesToShow: 2,
                        }
                    },
                    {
                        breakpoint: 480,
                        settings: {
                            slidesToShow: 1,
                        }
                    }
                ]
            });
        }
    });
} else {
    console.log('jQuery or Slick not loaded, carousel functionality disabled');
}

//används denna???
function searchFriends() {
    console.log("🔍 [JS DEBUG] searchFriends() called");

    const query = document.getElementById('friendSearch').value;
    const resultsDiv = document.getElementById('friend-search-results');
    resultsDiv.innerHTML = ""; // Clear previous results

    if (!query.trim()) {
        console.log("⚠️ [JS DEBUG] No input provided.");

        resultsDiv.innerHTML = "<p>Please enter a search term.</p>";
        return;
    }
    const url = `/profile/searchMembers?query=${encodeURIComponent(query)}`;
    console.log("🌐 [JS DEBUG] Sending fetch to:", url);


    fetch(`/profile/search-members?query=${encodeURIComponent(query)}`)
        .then(response => {
            console.log("📡 [JS DEBUG] Fetch response status:", response.status);

            if (!response.ok) throw new Error("Failed to fetch results");
            return response.json();
        })
        .then(data => {
            console.log("📦 [JS DEBUG] Fetch returned data:", data);

            if (data.length === 0) {
                resultsDiv.innerHTML = "<p>No matching users found.</p>";
                return;
            }

            data.forEach(user => {
                const userCard = document.createElement("div");
                userCard.className = "d-flex justify-content-between align-items-center border p-2 mb-2 rounded";

                const image = document.createElement("img");
                image.src = user.profileImage ? `/profileImages/${user.profileImage}` : "/images/profilepicture.webp";
                image.alt = "Profile";
                image.className = "rounded-circle me-2";
                image.style.width = "40px";
                image.style.height = "40px";

                const nameSpan = document.createElement("span");
                nameSpan.textContent = user.displayName;

                const nameContainer = document.createElement("div");
                nameContainer.className = "d-flex align-items-center";
                nameContainer.appendChild(image);
                nameContainer.appendChild(nameSpan);
                userCard.appendChild(nameContainer);

                const form = document.createElement("form");
                form.method = "post";
                form.action = "/friendship/request";

                const input = document.createElement("input");
                input.type = "hidden";
                input.name = "recipientId";
                input.value = user.id;

                const button = document.createElement("button");
                button.type = "submit";
                button.className = "btn btn-sm btn-success";
                button.textContent = "Send Request";

                form.appendChild(input);
                form.appendChild(button);

                userCard.appendChild(nameSpan);
                userCard.appendChild(form);
                resultsDiv.appendChild(userCard);
            });
        })
        .catch(error => {
            resultsDiv.innerHTML = "<p>Error while searching. Please try again.</p>";
            console.error("❌ [JS DEBUG] Fetch failed:", error);
            console.error(error);
        });
}

// Auto-stäng success modaler efter 2 sekunder
function autoCloseSuccessModals() {
    const errorModal = document.getElementById('errorModal');
    const modalMessage = document.getElementById('modalMessage');

    if (errorModal && modalMessage && errorModal.style.display === 'block') {
        const messageText = modalMessage.textContent;

        if (messageText && (
            messageText.includes('successfully') ||
            messageText.includes('added') ||
            messageText.includes('created')
        )) {
            console.log("Success message detected, will auto-close in 2 seconds");

            setTimeout(function() {
                errorModal.style.display = 'none';
                console.log("Modal closed automatically");
            }, 2000);
        }
    }
}

// Kör auto-close funktionen när DOM laddas
document.addEventListener('DOMContentLoaded', function() {
    setTimeout(autoCloseSuccessModals, 100);
    setTimeout(autoCloseSuccessModals, 500);
});

//kör också auto-close när showError anropas
const originalShowError = window.showError;
if (typeof originalShowError === 'function') {
    window.showError = function(message) {
        originalShowError(message);
        // Kör auto-close efter att modalen visats
        setTimeout(autoCloseSuccessModals, 100);
    };
}

