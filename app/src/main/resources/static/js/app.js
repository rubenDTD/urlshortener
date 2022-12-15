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
                        $("#shortme-result").html(
                            "<div class='alert alert-success lead'>Short URI -> <a target='_blank' href='"
                            + request.getResponseHeader('Location')
                            + "'>"
                            + request.getResponseHeader('Location')
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
                    console.log('Success:', result);
                  })
                  .catch((error) => {
                    console.error('Error:', error);
                  });
            });
    });
