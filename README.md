# ELOG+
## Overview

Welcome to the Log Entry Microservice documentation. This Java-based microservice provides a robust 
platform for managing log entries, logbooks, and offers secure authentication and authorization mechanisms.

## Table of Contents

- [Features](#features)
- [Getting Started](#getting-started)
- [Authentication and Authorization](#authentication-and-authorization)
- [API Endpoints](#api-endpoints)
- [Configuration](#configuration)
- [Dependencies](#dependencies)
- [Contributing](#contributing)
- [License](#license)

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

ELOG+ run on java virtual machine version 19+. It uses Spring Boot Framework 3.x

## JWT Application token key
the ***app-token-jwt-key*** properties can be filled using the openssl tools like shown below
```shell
openssl rand -hex <size> 
```


