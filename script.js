let registerForm = document.getElementById("registerForm");

if (registerForm != null) {
  registerForm.addEventListener("submit", function (event) {
    event.preventDefault();

    let name = document.getElementById("name").value;
    let email = document.getElementById("email").value;
    let password = document.getElementById("password").value;
    let confirmPassword = document.getElementById("confirmPassword").value;
    let teachSkill = document.getElementById("teachSkill").value;
    let learnSkill = document.getElementById("learnSkill").value;

    if (name == "") {
      alert("Please enter your name.");
    } else if (email == "") {
      alert("Please enter your email.");
    } else if (password == "") {
      alert("Please enter your password.");
    } else if (confirmPassword == "") {
      alert("Please confirm your password.");
    } else if (teachSkill == "") {
      alert("Please enter a skill you can teach.");
    } else if (learnSkill == "") {
      alert("Please enter a skill you want to learn.");
    } else if (password != confirmPassword) {
      alert("Passwords do not match.");
    } else {
      let data =
        "name=" +
        name +
        "&email=" +
        email +
        "&password=" +
        password +
        "&teachSkill=" +
        teachSkill +
        "&learnSkill=" +
        learnSkill;

      fetch("http://localhost:8080/register", {
        method: "POST",
        body: data,
      })
        .then((response) => response.text())

        .then((result) => {
          alert(result);

          if (result.trim() == "Registration successful!") {
            window.location.href = "login.html";
          }
        })

        .catch((error) => {
          alert("Server connection failed.");
        });
    }
  });
}

let loginForm = document.getElementById("loginForm");

if (loginForm != null) {
  loginForm.addEventListener("submit", function (event) {
    event.preventDefault();

    let email = document.getElementById("loginEmail").value;
    let password = document.getElementById("loginPassword").value;

    if (email == "") {
      alert("Please enter your email.");
    } else if (password == "") {
      alert("Please enter your password.");
    } else {
      let data = "email=" + email + "&password=" + password;

      fetch("http://localhost:8080/login", {
        method: "POST",
        body: data,
      })
        .then((response) => response.text())
        .then((result) => {
          alert(result);

          if (result.trim() == "Login successful!") {
            localStorage.setItem("userEmail", email);

            window.location.href = "dashboard.html";
          }
        })
        .catch((error) => {
          alert("Server connection failed.");
        });
    }
  });
}
