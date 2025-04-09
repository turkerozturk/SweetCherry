// <script src="https://cdnjs.cloudflare.com/ajax/libs/raphael/2.1.0/raphael-min.js"></script>
// <script src="https://cdn.jsdelivr.net/npm/morris.js/morris.min.js"></script>

$(document).ready(function () {
    var syntaxes = /*[[${syntaxes}]]*/ [];
    var counts = /*[[${counts}]]*/ [];

    var data = [];
    for (var i = 0; i < syntaxes.length; i++) {
        data.push({
            label: syntaxes[i],
            value: counts[i]
        });
    }

    new Morris.Donut({
        element: 'donut-chart',
        data: data
    });
});
