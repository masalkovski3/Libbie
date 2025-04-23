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
    }
}

function closeModal() {
    const modal = document.getElementById("errorModal");
    if (modal) {
        modal.style.display = "none";
    }
}





