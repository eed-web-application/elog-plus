# ELOG+
## Overview

The ELOG+ is a Java-based microservice that provides a robust platform for managing log entries, logbooks, and offers secure authentication and authorization mechanisms.

## Table of Contents

- [Features](#features)
- [Getting Started](#getting-started)
- [Application tokens](#application-tokens)


## Features

The Log Entry Microservice offers the following key features:

- **Log Entry Management**: Create, update, retrieve, and delete log entries.
- **Logbook Support**: Log entries can belong to one or more logbooks.
- **Tagging**: Logbooks can define tags for categorizing log entries.
- **Authentication and Authorization**:
    - User Authentication: Users can sign up and log in with varying levels of access (read, write, admin) on specific logbooks.
    - Application Token Authentication: Secure access for applications with generated tokens.

## Getting Started

The ELOG+ Microservice is a Java backend application designed to manage log entries. Each log 
entry can belong to one or more logbooks, and each logbook can define one or more tags to categorize 
and specify the log entries. The application supports user or application token authentication and 
authorization for securing access. Additionally, it provides fine-grained access control, allowing 
users to have read, write, and admin authorization on each logbook.

### Scalability

Scalability is a critical aspect on modern Microservices architecture, the new ELOGs uses stateless REST api 
allowing to have more than one instance that run into modern orchestrator. This help to increase the number of
http request that can be processed.

### Prerequisites

ELOG+ run on java virtual machine version 19+. A dockerfile is provided to permit the creation of a container image.

## Application tokens
ELOG+ provides robust support for external application authentication through the use of custom-managed JSON Web Tokens (JWT). This functionality allows for secure and token-based communication between ELOG+ and third-party applications. To utilize this feature, a cryptographic key must be configured through the ELOG_PLUS_APP_TOKEN_JWT environment variable. To generate a cryptographic key for JWT token verification, you need to execute the following command using OpenSSL:

```shell
openssl rand -hex <size> 
```

Here, <size> should be replaced by the desired byte size for the cryptographic key. This generated key needs to be set in the ELOG_PLUS_APP_TOKEN_JWT environment variable for ELOG+ to utilize it for JWT token generation and validation.
To pre-configure a root token, associate a JSON-formatted string to the ***ELOG_PLUS_ROOT_AUTHENTICATION_TOKEN_JSON*** environment variable. The JSON string should include the name and expiration properties for each root token you wish to establish.
The following code snippet demonstrates how to create a root token named ***root-token-1*** with an expiration date set to December 31, 2024:

```properties
ELOG_PLUS_ROOT_AUTHENTICATION_TOKEN_JSON: '[{"name":"root-token-1","expiration":"2024-12-31"}]'
```

### Execute the application in docker
To launch the backend application, simply execute the following command:
```shell
docker compose -f docker-compose.yml -f docker-compose-app.yml up --build 
```

***Note***: the '**_--build_**' parameter force the backend service to be rebuilt every time is started up (useful when the code is updated).

This command initiates the Docker Compose setup, which includes spinning up Minio, Kafka, MongoDB, and the backend as a unified Docker Compose application. Once the process is complete, you can access the backend through port 8080.