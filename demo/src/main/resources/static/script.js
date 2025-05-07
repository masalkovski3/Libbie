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

function showPasswordErrorModal() {
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
    setupSessionWarning(2, 1);
});
