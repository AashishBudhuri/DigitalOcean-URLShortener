const API_URL = "/api/shorten";

const form = document.getElementById("shorten-form");
const longUrlInput = document.getElementById("long-url");
const aliasInput = document.getElementById("alias");
const messageEl = document.getElementById("message");
const resultEl = document.getElementById("result");
const shortUrlEl = document.getElementById("short-url");

function showMessage(text, isError = false) {
  messageEl.hidden = false;
  messageEl.textContent = text;
  messageEl.className = isError ? "error" : "info";
}

function clearFeedback() {
  messageEl.hidden = true;
  messageEl.textContent = "";
  messageEl.className = "";
  resultEl.hidden = true;
  shortUrlEl.href = "#";
  shortUrlEl.textContent = "";
}

function showResult(shortUrl) {
  resultEl.hidden = false;
  shortUrlEl.href = shortUrl;
  shortUrlEl.textContent = shortUrl;
}

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  clearFeedback();

  const longUrl = longUrlInput.value.trim();
  const alias = aliasInput.value.trim();

  if (!longUrl) {
    showMessage("Long URL is required.", true);
    longUrlInput.focus();
    return;
  }

  if (!URL.canParse(longUrl)) {
    showMessage("Enter a valid URL (include https://).", true);
    longUrlInput.focus();
    return;
  }

  if (alias && !/^[A-Za-z0-9_-]+$/.test(alias)) {
    showMessage("Alias may only contain letters, numbers, hyphens, and underscores.", true);
    aliasInput.focus();
    return;
  }

  const body = { longUrl };
  if (alias) {
    body.alias = alias;
  }

  const submitButton = form.querySelector('button[type="submit"]');
  submitButton.disabled = true;

  try {
    const response = await fetch(API_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });

    const data = await response.json().catch(() => ({}));

    if (data.status === "FAILURE") {
      showMessage(data.errorMessage || "Could not shorten URL.", true);
      return;
    }

    if (data.status === "SUCCESS") {
      showMessage("URL shortened successfully.");
      if (data.shortUrl) {
        showResult(data.shortUrl);
      }
      return;
    }

    if (!response.ok) {
      const errorText = data.error || data.errorMessage || data.message || "Could not shorten URL.";
      showMessage(errorText, true);
      return;
    }

    showMessage("Unexpected response from server.", true);
  } catch (error) {
    showMessage("Network error. Is the API running?", true);
    console.error(error);
  } finally {
    submitButton.disabled = false;
  }
});
