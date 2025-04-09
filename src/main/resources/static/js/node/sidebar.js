/*

https://github.com/codzsword/sidebar-bootstrap
demo: https://codzsword.github.io/sidebar-bootstrap/
How to Create Sidebar Using Bootstrap 5 | Responsive Sidebar With Bootstrap | Sidebar Menu
https://www.youtube.com/watch?v=i7uJAOFEd4g

 */

function functionSidebarWidth() {

    var savedWidth = sessionStorage.getItem('sidebarWidth');

    if (savedWidth) {
        savedWidth = Math.round(savedWidth);
        $('#sidebar').css('width', savedWidth + 'px');
        $('#sidebar').css('min-width', savedWidth + 'px');
    } else {
            var firstStartWidth = 300;
            $('#sidebar').css('width', firstStartWidth + 'px');
            $('#sidebar').css('min-width', firstStartWidth + 'px');
    }

}

    functionSidebarWidth();


$(document).ready(function () {


    $('#btn-toggle-sidebar').click(function () {

        functionSidebarWidth();

        if ($('#sideBarContent').css('visibility') == 'visible') {
            $('#sideBarContent').css({
                "visibility": "hidden"
            });
            $('#sidebar').css('width', '20px');
            $('#sidebar').css('min-width', '20px');
        } else {
            $('#sideBarContent').css({
                "visibility": "visible"
            });
            $('#sidebar').css('width', savedWidth + 'px');
            $('#sidebar').css('min-width', savedWidth + 'px');
        }
    });

    var isResizing = false,
    lastDownX = 0;

    var sidebar = $('#sidebar');
    var resizer = $('.resizer');

    resizer.on('mousedown', function (e) {
        isResizing = true;
        lastDownX = e.clientX;

        $(document).on('mousemove', function (e) {
            if (!isResizing)
                return;

            var offsetRight = document.body.offsetWidth - (e.clientX - document.body.offsetLeft);
            var newWidth = document.body.offsetWidth - offsetRight;

            if (newWidth > 20 && newWidth < 500) { // Minimum ve maksimum genişlik değerleri
                $('#sidebar').css('width', newWidth + 'px');
                $('#sidebar').css('min-width', newWidth + 'px');
            }


        });

        $(document).on('mouseup', function (e) {
            isResizing = false;
            var newWidth = sidebar.width();
            $(document).off('mousemove');
            $(document).off('mouseup');
            sessionStorage.setItem('sidebarWidth', newWidth); // Genişliği sessionStorage'a kaydet


        });
    });
});
