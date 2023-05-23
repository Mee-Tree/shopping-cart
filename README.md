# Shopping Cart

[![Scala CI](https://github.com/Mee-Tree/shopping-cart/actions/workflows/ci.yml/badge.svg)](https://github.com/Mee-Tree/shopping-cart/actions/workflows/ci.yml)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-brightgreen.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org) 
<a href="https://typelevel.org/cats/"><img src="https://raw.githubusercontent.com/typelevel/cats/c23130d2c2e4a320ba4cde9a7c7895c6f217d305/docs/src/main/resources/microsite/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>

## Overview

A pet-project shop application using Tagless Final approach.

#### Open HTTP endpoints

---
- `POST /auth/register`
  - *201*: user was successfully created
  - *400*: invalid username or password
  - *409*: the username is already taken
- `POST /auth/login`
  - *200*: user was successfully logged in
  - *403*: invalid username or password
- `GET /brands`
  - *200*: returns a list of brands
- `GET /categories`
  - *200*: returns a list of categories
- `GET /items`
  - *200*: returns a list of items
- `GET /items?brand={name}`
  - *200*: returns a list of items with the specified brand
---

#### Secure HTTP endpoints (requiring an Auth Token returned by `/auth` endpoints)
All of them can return the following response statuses in addition to the specific ones.
- *401*: unauthorized user, needs to log in
- *403*: the user does not have permission to perform this action

---
- `POST /auth/logout`
  - *204*: user was successfully logged out
- `GET /cart`
  - *200*: returns the cart for the current user
- `POST /cart`
  - *201*: item was added to the cart
  - *409*: item is already in the cart
- `PUT /cart`
  - *200*: quantity of some items were updated in the cart
  - *400*: quantities must be greater than zero
- `DELETE /cart/{itemId}`
  - *204*: the speciﬁed item was removed from the cart, if it existed
- `POST /checkout`
  - *201*: order was processed successfully
  - *400*: invalid card details
- `GET /orders`
  - *200*: returns the list of orders for the current user
- `GET /orders/{orderId}`
  - *200*: returns specific order for the current user
  - *404*: order not found
---

#### Admin HTTP endpoints (can be accessed only by admins with a specific Access Token)
Can return *401* and *403* for the same reasons as Secure HTTP endpoints.

---
- `POST /brands`
  - *201*: brand successfully created
  - *409*: brand name is already taken
- `POST /categories`
  - *201*: category successfully created
  - *409*: category name is already taken
- `POST /items`
  - *201*: items successfully created
  - *409*: some of the items already exist
- `PUT /items`
  - *200*: item’s price successfully updated
  - *400*: the price must be greater than zero
---

## Tech Stack

Every used library can be found in [Dependencies.scala](project/Dependencies.scala).

## Tests

To run Unit Tests:

```
sbt test
```

To run Integration Tests you need to run both `PostgreSQL` and `Redis`:

```
docker-compose up
sbt IntegrationTest/test
docker-compose down
```

## Payment Client

The configured payment client is a fake API ([https://beeceptor.com/console/payments](https://beeceptor.com/console/payments)) that always returns 200 with a Payment Id.
