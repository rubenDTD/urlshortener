# Getting Started with the URL Shortener project
[![Build Status](../../actions/workflows/ci.yml/badge.svg)](../../actions/workflows/ci.yml)

* [Overall structure](#overall-structure)
* [Run](#run)
* [Build and Run](#build-and-run)
* [Functionalities](#functionalities)
* [Delivery](#delivery)
* [Repositories](#repositories)
* [Reference Documentation](#reference-documentation)
* [Guides](#guides)

## Overall structure

The structure of this project is heavily influenced by
[the clean architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html):

* A `core` module where we define the domain entities and the functionalities
  (also known as uses cases, business rules, etc.). They do not know that this application.
  has a web interface or that data is stored in relational databases.
* A `repositories` module that knows how to store domain entities in a relational database.
* A `delivery` module that knows how to expose in the Web the functionalities.
* An `app` module that contains the main, the configuration (i.e. it links `core`, `delivery` and `repositories`),
  and the static assets (i.e. html files, JavaScript files, etc. )

Usually, if you plan to add a new feature, usually:

* You will add a new use case to the `core` module.
* If required, you will modify the persistence model in the `repositories` module.
* You will implement a web-oriented solution to expose to clients in the `delivery` module.

Sometimes, your feature will not be as simple, and it would require:

* To connect a third party (e.g. an external server).
  In this case you will add a new module named `gateway` responsible for such task.
* An additional application.  
  In this case you can create a new application module (e.g. `app2`) with the appropriate configuration to run this second server.

Features that require the connection to a third party or having more than a single app will be rewarded.

## Run

First you need to install and start the RabbitMQ service:

On windows:
```shell
docker pull rabbitmq:3-management
docker run -d -p 15672:15672 -p 5672:5672 --name rabbit-urlshortener rabbitmq:3-management
```

On linux:
```shell
sudo apt install rabbitmq-server
sudo systemctl enable rabbitmq-server
sudo rabbitmq-plugins enable rabbitmq_management
```

Then access to the RabbitMQ dashboard (http://localhost:15672) and login with "guest" as user and password

The application can be run as follows:

```shell
./gradlew :app:bootRun
```

## Build and Run

The uberjar can be built and then run with:

```shell
./gradlew build
java -jar app/build/libs/app.jar
```

## Functionalities

The project offers a minimum set of functionalities:

* **Create a short URL**.
  See in `core` the use case `CreateShortUrlUseCase` and in `delivery` the REST controller `UrlShortenerController`.

* **Redirect to a URL**.
  See in `core` the use case `RedirectUseCase` and in `delivery` the REST controller `UrlShortenerController`.

* **Log redirects**.
  See in `core` the use case `LogClickUseCase` and in `delivery` the REST controller `UrlShortenerController`.

* **Check URL in black list**.
  See in `core` the use case `BlackListUseCase` and in `delivery` the REST controller `UrlShortenerController`.

* **Create Short URLs from csv file**.
  See in `core` the use case `CreateShortUrlCsvUseCase` and in `delivery` the REST controller `UrlShortenerController`.

* **Get info from request's header**.
  See in `core` the use case `HeadersInfoUseCase` and in `delivery` the REST controller `UrlShortenerController`.

* **Show clicks summary from**.
  See in `core` the use case `InfoSummaryUseCase` and in `delivery` the REST controller `UrlShortenerController`.

* **Return clicks summary from URL**.
  See in `core` the use case `SponsorUseCase` and in `delivery` the REST controller `UrlShortenerController`.

The objects in the domain are:

* `ShortUrl`: the minimum information about a short url
* `Redirection`:  the remote URI and the redirection mode
* `ShortUrlProperties`: a handy way to extend data about a short url
* `Click`: the minimum data captured when a redirection is logged
* `ClickProperties`: a handy way to extend data about a click
* `CsvResponse`: a handy way to extend data about the processed urls from a csv

## Delivery

The above functionality is available through a simple API:

* `POST /api/link` which creates a short URL from data sent by a form.
* `POST /api/bulk` which creates short URLs from a csv file sent by a form and downloads the resulting csv on a browser.
* `GET /tiny-{id}` where `id` identifies the short url, deals with redirects, and logs use (i.e. clicks).
* `GET /api/link/{id}` which shows the clicks summary of an URL.

In addition, `GET /` returns the landing page of the system.

## Repositories

All the data is stored in a relational database.
There are only two tables.

* **shorturl** that represents short url and encodes in each row `ShortUrl` related data,
* **click** that represents clicks and encodes in each row `Click` related data.

## Reference Documentation

For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/htmlsingle/)
* [Spring Web](https://docs.spring.io/spring-boot/docs/2.7.3/reference/htmlsingle/#boot-features-developing-web-applications)
* [Spring Data JPA](https://docs.spring.io/spring-boot/docs/2.7.3/reference/htmlsingle/#boot-features-jpa-and-spring-data)

## Guides

The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Messaging with RabbitMQ with Spring](https://spring.io/guides/gs/messaging-rabbitmq/)
* [Setup SonarCloud for Gradle](https://www.baeldung.com/sonar-qube) (Solo en parte, pues al vincular SonarCloud con tu cuenta de github y el proyecto, el propio SonarCloud tiene una guia de un par de pasos sobre como añadirlo a gradle y al CI)
