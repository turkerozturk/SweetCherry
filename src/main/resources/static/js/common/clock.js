function updateTime() {
    var now = new Date();
    var hours = now.getHours();
    var minutes = now.getMinutes();
    var seconds = now.getSeconds();
    var day = now.getDate();
    var month = now.getMonth() + 1; // JavaScript'te aylar 0'dan başlar, bu yüzden +1 ekliyoruz.
    var year = now.getFullYear();

    // Zamanı iki haneli sayılar olarak formatlayalım (örneğin, 01:02:03 instead of 1:2:3)
    hours = hours < 10 ? "0" + hours : hours;
    minutes = minutes < 10 ? "0" + minutes : minutes;
    seconds = seconds < 10 ? "0" + seconds : seconds;
    month = month < 10 ? "0" + month : month;
    day = day < 10 ? "0" + day : day;

    var clockElement = document.getElementById('clock');
    clockElement.innerHTML = year + "-" + month + "-" + day + " " + hours + ":" + minutes + ":" + seconds;

}

// updateTime fonksiyonunu her saniye çağıralım
setInterval(updateTime, 1000);

// Sayfa yüklendiğinde saat bilgisini göstermek için updateTime fonksiyonunu bir kez çağıralım
updateTime();