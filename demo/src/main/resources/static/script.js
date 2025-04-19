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

            if (!validatePassword(password)) {
                event.preventDefault();
                return;
            }

            if (password !== repeatPassword) {
                alert("The passwords does not match.");
                event.preventDefault();
            }
        });
    }
});


function validatePassword(password) {
    var passwordStrong = true;
    var message = '';

    if (password.length < 12) {
        message += 'The password must contain at least 12 characters. \n';
        passwordStrong = false;
    }

    if (!/[A-Z]/.test(password)) {
        message += 'The password must contain at least one uppercase letter.\n';
        passwordStrong = false;
    }

    if (!/[a-z]/.test(password)) {
        message += 'The password must contain at least one lowercase letter.\n';
        passwordStrong = false;
    }

    if (!/\d/.test(password)) {
        message += 'The password must contain at least one number.\n';
        passwordStrong = false;
    }

    if (!passwordStrong) {
        alert(message); // Alternativt kan du använda showPasswordErrorModal() för modal
        return false;
    }

    return true;
}




