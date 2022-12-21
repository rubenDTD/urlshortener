$(document).ready(
    function () {
        $("#shortener").submit(
            function (event) {
                event.preventDefault();
                $.ajax({
                    type: "POST",
                    url: "/api/link",
                    data: $(this).serialize(),
                    success: function (msg, status, request) {
                        let location = request.getResponseHeader('Location').split("/")
                        let hash = location[location.length-1]
                        console.log(hash)
                        $("#shortme-result").html(
                            "<div class='alert alert-success lead'>Short URI -> <a target='_blank' href='"
                            + request.getResponseHeader('Location')
                            + "'>"
                            + request.getResponseHeader('Location')
                            + "</a></div>"
                            + "<div class='alert alert-success lead'>URI Summary -> <a target='_blank' href='"
                            + request.getResponseHeader('Location').replace(hash,"api/link/"+ hash)
                            + "'>"
                            + request.getResponseHeader('Location').replace(hash,"api/link/"+ hash)
                            + "</a></div>");
                    },
                    error: function () {
                        $("#shortme-result").html(
                            "<div class='alert alert-danger lead'>ERROR</div>");
                    }
                });
            });
        $("#shortcsv").submit(
            function (event) {
                event.preventDefault();

                let csv = document.getElementById("formFile").files[0]
                const formData = new FormData();
                formData.append('file', csv);

                fetch('http://localhost:8080/api/bulk', {
                  method: 'POST',
                  body: formData
                })
                  .then((response) => response.json())
                  .then((result) => {
                    let location = result.url.split("/")
                    let hash = location[location.length-1]
                    document.getElementById("shortus-result").innerHTML =
                        "<div class='alert alert-success lead'>First URI ->"
                        + "<a target='_blank' href='" + result.url + "'>"
                        + result.url
                        + "</a></div>"
                        + "<div class='alert alert-success lead'>First URI Summary -> <a target='_blank' href='"
                        + result.url.replace(hash,"api/link/"+ hash)
                        + "'>"
                        + result.url.replace(hash,"api/link/"+ hash)
                        + "</a></div>"
                  })
                  /* Para sacar el nombre del fichero a guardar
                  .then(response =>
                    const filename = response.headers
                      .get("content-disposition")
                      .split('"')[1];
                  */
                  /* Para descargarlo
                  .then(response => response.blob())
                  .then(blob => URL.createObjectURL(blob))
                  .then(uril => {
                      var link = document.createElement("a");
                      link.href = uril;
                      link.download = "shortURLs.csv";
                      document.body.appendChild(link);
                      link.click();
                      document.body.removeChild(link);
                  })*/
                  .catch((error) => {
                    console.error('Error:', error);
                  });
            });
    });
