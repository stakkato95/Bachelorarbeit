'use strict'

const UI_UPDATE_INTERVAL = 1000

function refreshTableFromMap(tableId, content) {
    $(tableId).find("tr:gt(0)").remove()
    for (const [key, value] of Object.entries(content)) {
        $(`${tableId} tr:last`).after(
            `<tr>
                <td>${key}</td><td>${value}</td>
            </tr>`
        )
    }
}

function refreshTableFromArray(tableId, content, itemPrint) {
    $(tableId).find("tr:gt(0)").remove()
    for (const item of content) {
        $(`${tableId} tr:last`).after(`<tr>${itemPrint(item)}</tr>`)
    }
}

function setLeader(leader) {
    $("#leaderContainer #nodeId").text("Node id: " + leader.nodeId)

    refreshTableFromMap("#nextIndicesTable", leader.nextIndices)

    if (leader.leaderCommit === undefined) {
        $("#leaderContainer").hide()
    } else {
        $("#leaderCommit").show()
        $("#leaderCommit").text("Leader commit: " + leader.leaderCommit)
    }

    if (leader.log.stateMachineValue === "") {
        $("#leaderContainer #stateMachineValue").hide()
    } else {
        $("#leaderContainer #stateMachineValue").show()
        $("#leaderContainer #stateMachineValue").text("State machine value: " + stateMachineValue)
    }

    if (leader.log.log.length) {
        refreshTableFromArray(
            "#logItemsTable",
            leader.log.log,
            item => `<td>${item.leaderTerm}</td><td>${item.value}</td>`
        )
    }
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