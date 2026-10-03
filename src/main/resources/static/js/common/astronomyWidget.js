/* Loads only the opened astronomy popover; all calculations remain on the local server. */
(function () {
    "use strict";
    document.querySelectorAll(".astronomy-button").forEach(function (button) {
        button.addEventListener("shown.bs.popover", async function () {
            const popup = document.getElementById(button.getAttribute("aria-describedby"));
            const content = popup && popup.querySelector(".astronomy-popup");
            if (!content) return;
            content.textContent = button.dataset.astronomyLoading;
            try {
                const response = await fetch("/astronomy", {credentials: "same-origin"});
                if (!response.ok || response.redirected) throw new Error("Astronomy request failed");
                content.innerHTML = await response.text();
            } catch (error) {
                content.textContent = button.dataset.astronomyError;
            }
        });
    });
}());
