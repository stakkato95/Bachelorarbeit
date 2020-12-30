'use strict';

const UI_UPDATE_INTERVAL = 1000

function update() {
    // $.ajax({
    //     url: "http://localhost:9000/cluster/state"
    // }).then(function (data) {
    //     $("#jsonContent").text(data.leader.nodeId);
    // }).fail(function (jqXHR, textStatus, errorThrown) {
    //     alert(errorThrown);
    // });
}

function onPageLoaded() {
    setInterval(update, UI_UPDATE_INTERVAL);
}

console.log("Welcome to your Play application's JavaScript!");
document.addEventListener("DOMContentLoaded", onPageLoaded);
$(document).ready(function () {

});