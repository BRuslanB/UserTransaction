# User Transaction Service
This backend Java-based service is backed by a REST API for storing and processing expense transactions, as well as setting custom spending limits.

Note. This custom transaction service was created as a test task for an internship at one of the IT companies (examples of scripts for checking can be found in the UserTransactionNew\scripts folder).

## Features
The User Transaction Service allows you to process two subtypes of services (banking and user):

1. Banking services:
* Receive and save transaction costs;
* Receive current exchange rates from an external resource;
* Convert the transaction amount to determine if the limit has been exceeded. The amount is converted at the current exchange rate and the type of currency of the established limit.

2. User services:
* Set a limit for subsequent transactions in the selected currency;
* Receive a list of all user limits;
* Receive a list of transactions that have exceeded the limit.

## Technologies Used
The User Transaction Service is built using the following technologies:

* Java 17
* Spring Boot
* Spring Data JPA
* Hibernate
* PostgreSQL
* FlyWay
* Log4j2
* Swagger
* JUnit
* H2

## Prerequisites
Before running the Service, you will need to have the following software installed on your machine:

* Java Development Kit (JDK) 17 or higher
* Docker

## Getting Started
To run the Service locally, follow these steps:

1. Clone the repository to your local machine:
```bash
git clone https://github.com/BRuslanB/UserTransaction.git
```
2. Launch:
```bash
docker-compose up -d
```
3. Checking services. Documentation for services can be found at the following link:
```
http://localhost:8000/swagger-ui/index.html
```

## Contributing
If you would like to contribute to developing this Service, please submit a pull request or open an issue on the GitHub repository.

## License
This Service is licensed under the MIT License. See the LICENSE file for more details.
