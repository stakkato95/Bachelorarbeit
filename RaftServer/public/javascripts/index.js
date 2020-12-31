'use strict'

const UI_UPDATE_INTERVAL = 1000

function refreshTable(tableId, content) {
    $(tableId).find("tr:gt(0)").remove()
    for (const [key, value] of Object.entries(content)) {
        $('#nextIndicesTable tr:last').after(
            `<tr>
                <td>${key}</td><td>${value}</td>
            </tr>`
        )
    }
}

function setLeader(leader) {
    $("#leaderContainer #nodeId").text("Node id: " + leader.nodeId)
    refreshTable("#nextIndicesTable", leader.nextIndices)
    $("#leaderContainer #pendingItems").text("Pending items: " + JSON.stringify(leader.pendingItems))
    $("#leaderContainer #leaderCommit").text("Leader commit: " + JSON.stringify(leader.leaderCommit))
    $("#leaderContainer #log").text("Log: " + JSON.stringify(leader.log))
}

function update() {
    $.ajax({
        url: "http://localhost:9000/cluster/state"
    }).then(function (data) {
        if (data.leader !== undefined) {
            setLeader(data.leader)
        }

    }).fail(function (jqXHR, textStatus, errorThrown) {
        // alert(errorThrown)
    })
}

function onPageLoaded() {
    setInterval(update, UI_UPDATE_INTERVAL)
}

console.log("Welcome to your Play application's JavaScript!")
document.addEventListener("DOMContentLoaded", onPageLoaded)
$(document).ready(function () {

})