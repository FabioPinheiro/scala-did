# Pub/Sub Protocol

This protocol is to implement the Publish/Subscribe model.

Its being design to be used for a Push Notification Web API.

- **subscriber** - Subscribes to a Publisher.
- **publisher** - Publisher like a broker that send message to is subscribers.

## PIURI
- `https://fmgp.app/pubsub/v1/request` -
- `https://fmgp.app/pubsub/v1/setup` -
- `https://fmgp.app/pubsub/v1/subscribe` - edit the subscription
- `https://fmgp.app/pubsub/v1/subscription` - show the subscription status. Is a reponse to confirmation


### Roles

- subscriber
  - The DID Comm agent that subscribes.
  - PIURI: 
    - `https://fmgp.app/pubsub/v1/request
    - `https://fmgp.app/pubsub/v1/subscribe
- publisher
  - The DID Comm agent that send messages.
  - PIURI: 
    - `https://fmgp.app/pubsub/v1/setup
    - `https://fmgp.app/pubsub/v1/subscription
    - `https://fmgp.app/pubsub/v1/notification

### Messagem Flow Diagram (Example)

- request? -> setup? -> subscribe -> subscription
- notification


#### Example of Sequence Diagram
