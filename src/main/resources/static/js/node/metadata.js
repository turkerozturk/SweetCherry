$(document).ready(function () {
    // Popup'u acmak icin dugmeye tiklandiginda
    $("#openPopupBtn").click(function () {
        $("#nodeMetaPopup").show();
    });

    // Popup'u kapatmak icin close dugmesine tiklandiginda
    $(".close").click(function () {
        $("#nodeMetaPopup").hide();
    });

    // Popup'u kapatmak için disariya tiklandiginda
    $(window).click(function (e) {
        if (e.target.id === "nodeMetaPopup") {
            $("#nodeMetaPopup").hide();
        }
    });

    // Escape tusuna basildiginda popup'u kapat
    $(document).keyup(function (e) {
        if (e.key === "Escape") {
            $("#nodeMetaPopup").hide();
        }
    });
});