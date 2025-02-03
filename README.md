```
___  ___                                ______           _             
|  \/  |                                | ___ \         | |            
| .  . | ___  ___ ___  __ _  __ _  ___  | |_/ /_ __ ___ | | _____ _ __ 
| |\/| |/ _ \/ __/ __|/ _` |/ _` |/ _ \ | ___ \ '__/ _ \| |/ / _ \ '__|
| |  | |  __/\__ \__ \ (_| | (_| |  __/ | |_/ / | | (_) |   <  __/ |   
\_|  |_/\___||___/___/\__,_|\__, |\___| \____/|_|  \___/|_|\_\___|_|   
                             __/ |                                     
                            |___/                                      
```

# Message Broker and DNS Server

This project is an implementation of a distributed message broker system with a DNS server, inspired by the Advanced Message Queuing Protocol (AMQP). The system enables asynchronous communication between clients by managing message exchanges and queues while supporting leader election for fault tolerance.

## Features

### DNS Server
- Implements a **Simple DNS Protocol (SDP)** to register, unregister, and resolve domain names.
- Allows **message brokers to register themselves** at startup.
- Handles **multiple simultaneous clients** using multi-threading.
- Supports **in-memory persistence** for fast lookups.

### Message Broker
- Implements a **Simple Message Queuing Protocol (SMQP)** for message routing.
- Supports **direct, fanout, and topic exchanges** to route messages efficiently.
- Enables **subscribers to receive messages asynchronously**.
- Handles **leader election** to determine the active broker in a distributed environment.

## Getting Started

### Prerequisites
- **Java JDK 21**
- **Maven**
- **Netcat (Optional)** for manual testing.

### Installation
Clone the repository:
```bash
git clone git@github.com:tonyzamyatin/message-broker.git
cd message-broker
```
Compile the project:
```bash
mvn compile
```
Run the DNS Server:
```bash
mvn exec:java@dns-0
```
Run the Message Broker:
```bash
mvn exec:java@broker-0
```

## Protocols

### Simple DNS Protocol (SDP)
The DNS server provides domain name resolution for brokers.

| Command                 | Description                                        | Response Example         |
|-------------------------|----------------------------------------------------|--------------------------|
| `register <name> <ip:port>` | Registers a domain name.                         | `ok`                     |
| `resolve <name>`       | Resolves a domain name to IP and port.            | `192.168.1.10:8080`      |
| `unregister <name>`    | Removes a domain registration.                     | `ok`                     |

### Simple Message Queuing Protocol (SMQP)
The message broker routes messages between clients.

| Command                       | Description                                      | Response Example                        |
|--------------------------------|--------------------------------------------------|-----------------------------------------|
| `exchange <type> <name>`       | Creates an exchange (`direct`, `fanout`, `topic`). | `ok`                                   |
| `queue <name>`                 | Declares a queue.                                | `ok`                                   |
| `bind <binding-key>`           | Binds a queue to an exchange.                    | `ok`                                   |
| `publish <routing-key> <msg>`  | Publishes a message with a routing key.          | `ok`                                   |
| `subscribe`                    | Subscribes to messages in a queue.               | `ok`                                   |

### Leader Election Protocol (LEP)
The system supports leader election to ensure high availability.

| Command             | Description                          | Response Example   |
|---------------------|--------------------------------------|--------------------|
| `elect <id>`       | Initiates an election.               | `ok`              |
| `declare <id>`     | Declares a broker as the leader.     | `ack <sender-id>`  |
| `ping`             | Leader sends heartbeat messages.     | `pong`            |

## Manual Testing

You can test the components manually using **Netcat**:
```bash
nc localhost 20000  # Connect to the Message Broker
nc localhost 18000  # Connect to the DNS Server
```

## License
This project is open-source and available under the MIT License.

## Author
Anton Zamyatin

