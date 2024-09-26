function showNotification(message) {
    const notification = document.getElementById('notification');
    notification.textContent = message;
    notification.style.display = 'block';
    notification.style.opacity = 1;

    setTimeout(() => {
        notification.style.opacity = 0;
        setTimeout(() => {
            notification.style.display = 'none';
        }, 500);
    }, 3000);
}

function submitData() {
    const xValue = document.getElementById('x-text-input').value;
    const yValue = document.querySelector('input[name="yval"]:checked');
    const rValue = document.querySelector('input[name="rval"]:checked');

    if (xValue.trim() === "" || !yValue || !rValue) {
        showNotification("Введите хоть какую-нибудь цифру в поле икс, гении.");
        return;
    }

    // xValue = xValue.replace(',', '.');
    const x = parseFloat(xValue).toFixed(1);
    const y = parseFloat(yValue.value.replace(',', '.')).toFixed(1);
    const r = parseFloat(rValue.value.replace(',', '.')).toFixed(1);

    if (isNaN(x) || x < -3 || x > 3) {
        showNotification("Не шути так(( значение от -3 до 3.");
        return;
    }

    const url = new URL('/fcgi-bin/server-1.0.jar', window.location.href);
    url.searchParams.set('x', x)
    url.searchParams.set('y', y)
    url.searchParams.set('r', r)

    fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },

    })
        .then(response => response.json())
        .then(json => {
            const resultTable = document.getElementById('result-table');
            const newRow = document.createElement('tr');

            newRow.innerHTML = `
        <td>${x}</td>
        <td>${y}</td>
        <td>${r}</td>
        <td>${json.serverTime !== undefined ? json.serverTime : 'undefined'}</td>
        <td>${json.executionTime !== undefined ? json.executionTime : 'undefined'}</td>
        <td>${json.answer !== undefined ? (json.answer ? 'ЕСТЬ ПРОБИТИЕ' : 'УВЫ, НЕ ПРОБИЛ((') : 'undefined'}</td>
    `;

            resultTable.appendChild(newRow);
            saveResponseToLocalStorage(json);
        })
        .catch(error => console.error('Error:', error));

    drawPoints(x, y, r);
}

function getResponsesFromLocalStorage() {
    let data = localStorage.getItem("data");
    if (data == null) {
        data = '[]';
    }
    const obj = JSON.parse(data);
    return Object.keys(obj).map((key) => obj[key]);
}

function saveResponseToLocalStorage(response) {
    let responses = getResponsesFromLocalStorage();
    responses.push(response);
    console.log('Vse responses:', responses);
    localStorage.setItem("data", JSON.stringify(responses));
}

function showResponse(response) {
    const resultBody = document.getElementById('results');
    const newRow = document.createElement('tr');

    newRow.innerHTML = `
        <td>${response.x !== undefined ? response.x : 'undefined'}</td>
        <td>${response.y !== undefined ? response.y : 'undefined'}</td>
        <td>${response.r !== undefined ? response.r : 'undefined'}</td>
        <td>${response.serverTime !== undefined ? response.serverTime : 'undefined'}</td>
        <td>${response.executionTime !== undefined ? response.executionTime : 'undefined'}</td>
        <td>${response.answer !== undefined ? (response.answer ? 'ЕСТЬ ПРОБИТИЕ' : 'УВЫ, НЕ ПРОБИЛ((') : 'undefined'}</td>
    `;

    resultBody.appendChild(newRow);
}


function init() {
    let data = getResponsesFromLocalStorage();
    for (let i = 0; i < data.length; i++) {
        console.log("Подгружаем пипец:", data[i]);
        showResponse(data[i]);
    }
}

document.getElementById("input-form").addEventListener("submit", function(event) {
    event.preventDefault();
    submitData();
});

function drawPoints(x, y, r) {
    const targetDot = document.getElementById('target-dot');
    if (targetDot) {
        const graphX = 150 + (x / r) * 100;
        const graphY = 150 - (y / r) * 100;

        targetDot.setAttribute("cx", graphX);
        targetDot.setAttribute("cy", graphY);
        targetDot.setAttribute("r", 3);
    }
}

window.onload = function() {
    init();
};
