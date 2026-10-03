$(document).ready(function() {

    $("#locales").change(function () {
        var selectedOption = $('#locales').val();
        if (selectedOption != ''){
            // Keep the selected node, tenant token, map options and anchor when switching language.
            var target = new URL(window.location.href);
            target.searchParams.set('lang', selectedOption);
            window.location.replace(target.toString());
        }
    });


});