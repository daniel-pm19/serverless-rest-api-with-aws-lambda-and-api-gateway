# Prediction — `DELETE /orders/{id}`

Written **before** asking the AI assistant to implement the feature.

## Which Java classes should change?

- `OrderService`: it needs a new operation, something like `void deleteOrder(String id) throws Exceptions`, that removes the order from the list or throws `ORDER_NOT_FOUND` if it does not exist. It should reuse the same validation as `getOrder` for a null/blank id (`INVALID_ORDER`).
- `OrdersHandler`: `DELETE` on `/orders/{id}` currently returns 405. It has to call `deleteOrder` and return the right response.
- I don't think `Order`, `OrderRequest` or `Exceptions` need to change: `ORDER_NOT_FOUND` and `INVALID_ORDER` already exist.
- I suspect `generateOrderId()` may need to change. It uses `orders.size() + 1`, so once orders can be deleted the size goes down and the next id could repeat an id that already exists. A counter that only increases would be safer.

## Do you need a new Lambda function?

For the proxy architecture (B), no. `serverless-rest-api-orders-proxy` already handles every method on `/orders/{id}`, so I only need to upload the new JAR.

For the mapping architecture (A) I would need a fourth function with the handler `OrderService::deleteOrder`. I am not planning to extend A, only B.

## Does API Gateway configuration need to change?

For B, no. `/orders/{id}` uses `ANY` with proxy integration, so `DELETE` requests already reach the Lambda; today the adapter answers 405. No redeploy of the API is needed, only of the Lambda code.

For A, yes: a new `DELETE` method on `/orders/{id}`, a mapping template for the id, method responses for 204/400/404/500, and integration responses with the regexes. Then a redeploy.

## Where should the deletion logic reside?

In `OrderService`, next to `createOrder` and `getOrder`. Deleting an order is a business operation on the order collection; it has nothing to do with HTTP.

## Where should the decision to return 204 or 404 reside?

In the HTTP adapter (`OrdersHandler`). The service only says what happened: it returns normally if the order was deleted, or throws `ORDER_NOT_FOUND` if it did not exist. The adapter translates that into `204 No Content` with an empty body, or `404` with the usual `{error, message}` body. The existing error translation should already produce the 404.

## Which tests should be added?

`OrderServiceTest`:
- deleting an existing order removes it (`getOrder` afterwards throws `ORDER_NOT_FOUND`);
- deleting a nonexistent order throws `ORDER_NOT_FOUND`;
- deleting with a null/blank id throws `INVALID_ORDER`;
- deleting one order does not remove the others of the same customer;
- after a delete, a newly created order does not get an id that already exists.

`OrdersHandlerTest`:
- `DELETE /orders/ORD-42` returns 204 with no body;
- `DELETE /orders/ORD-999` returns 404 with `ORDER_NOT_FOUND`;
- `DELETE` followed by `GET` of the same id returns 404;
- deleting the same order twice: the second call returns 404.
- Change the existing test that expects 405 for `DELETE /orders/ORD-42`; it should use another method, e.g. `PUT`.
