// Scroll to top button element
/*
const scrollToTopBtn = document.getElementById('scrollToTopBtn');

// Show or hide the button based on scroll position
window.addEventListener('scroll', () => {
    if (window.pageYOffset > 300) {
        scrollToTopBtn.style.display = 'block';
    } else {
        scrollToTopBtn.style.display = 'none';
    }
});

// Scroll to the top when the button is clicked
scrollToTopBtn.addEventListener('click', () => {
    window.scrollTo({
        top: 0,
        behavior: 'smooth'
    });
});
*/

// Scroll to top button element
const scrollToTopBtn = document.getElementById('scrollToTopBtn');

// Function to check if horizontal scrollbar is present
function hasHorizontalScrollbar() {
    return document.documentElement.scrollWidth > window.innerWidth;
}

// Show or hide the button based on scroll position
window.addEventListener('scroll', () => {
    if (window.pageYOffset > 300) {
        scrollToTopBtn.style.display = 'block';
    } else {
        scrollToTopBtn.style.display = 'none';
    }

    // Adjust position based on horizontal scrollbar presence
    if (hasHorizontalScrollbar()) {
        scrollToTopBtn.style.right = 'auto';
        scrollToTopBtn.style.left = '20px';
    } else {
        scrollToTopBtn.style.left = 'auto';
        scrollToTopBtn.style.right = '20px';
    }
});

// Scroll to the top when the button is clicked
scrollToTopBtn.addEventListener('click', () => {
    window.scrollTo({
        top: 0,
        behavior: 'smooth'
    });
});

// Initial check in case the page loads with a scrollbar
if (hasHorizontalScrollbar()) {
    scrollToTopBtn.style.right = 'auto';
    scrollToTopBtn.style.left = '20px';
}