'use strict';

const UI_UPDATE_INTERVAL = 1000

function update() {
    // $.ajax({
    //     url: "localhost:9000/cluster/state",
    //     success: function (result) {
    //         $("#jsonContent").html(result);
    //     },
    //     error: function (xhr, ajaxOptions, thrownError) {
    //         alert(xhr.status);
    //         alert(thrownError);
    //     }
    // });

    $.ajax({
        url: "http://localhost:9000/cluster/state"
    }).then(function(data) {
        $("#jsonContent").html(data);
    }).fail(function(jqXHR, textStatus, errorThrown) {
        alert(errorThrown);
    });
}

function onPageLoaded() {
    setInterval(update, UI_UPDATE_INTERVAL);
}

console.log("Welcome to your Play application's JavaScript!");
document.addEventListener("DOMContentLoaded", onPageLoaded);
$(document).ready(function() {

});