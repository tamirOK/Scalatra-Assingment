# Assignment1 #

## Build & Run ##

```sh
$ cd Assignment1
$ sbt
> ~;jetty:stop;jetty:start
```

GET [http://localhost:8080/messages/](http://localhost:8080/messages/) - returns list of all messages

GET [http://localhost:8080/messages/id](http://localhost:8080/messages/1) - returns message with specified id

POST [http://localhost:8080/messages/](http://localhost:8080/messages/) - adds new message with specified id and text if message with such id does not exist

PUT [http://localhost:8080/messages/id](http://localhost:8080/messages/id) - changes text of the message with specified id if message with such id exists

DELETE [http://localhost:8080/messages/id](http://localhost:8080/messages/id) - removes message with specified id if message with such id exists



