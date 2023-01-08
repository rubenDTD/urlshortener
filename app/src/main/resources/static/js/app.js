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

                const fetch_csv = fetch('http://localhost:8080/api/bulk', {
                  method: 'POST',
                  body: formData
                })
                let hash = ""
                fetch_csv.then(response => {
                  // Get location and hash to redirect later
                  let location = response.headers.get("location").split("/")
                  hash = location[location.length-1]

                  document.getElementById("shortus-result").innerHTML =
                        "<div class='alert alert-success lead'>First URI ->"
                      + "<a target='_blank' href='" + response.headers.get("location") + "'>"
                      + response.headers.get("location")
                      + "</a></div>"
                      + "<div class='alert alert-success lead'>First URI Summary -> <a target='_blank' href='"
                      + response.headers.get("location").replace(hash,"api/link/"+ hash)
                      + "'>"
                      + response.headers.get("location").replace(hash,"api/link/"+ hash)
                      + "</a></div>"
                })
                .catch((error) => {
                    console.error('Error:', error);
                })

                fetch_csv.then(response => response.blob())
                .then(blob => URL.createObjectURL(blob))
                .then(uril => {
                    if(hash != "EMPTY" && hash != "ALL_INVALID") {
                        var link = document.createElement("a");
                        link.href = uril;
                        link.download = "shortURLs.csv";
                        document.body.appendChild(link);
                        link.click();
                        document.body.removeChild(link);
                    }
                })
                .catch((error) => {
                    console.error('Error:', error);
                });
            });
    });
