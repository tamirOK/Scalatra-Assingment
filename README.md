# Assignment2 #

## Build & Run ##

```sh
$ cd Scalatra-Assingment
$ sbt
> ~;jetty:stop;jetty:start
```

POST [http://localhost:8080/register/](#) - Register new user. Pass email, login, password in body

POST [http://localhost:8080/login/](#) - Authentication. JWT is returned.

*Pass JWT in Authorization header in below requests*

POST [http://localhost:8080/create_tweet/](#) - Creates new tweet. Pass tweet body.

PUT [http://localhost:8080/edit_tweet/](#) - Edits created tweet. Pass tweet id and new tweet body.

DELETE [http://localhost:8080/remove_tweet/](#) - Removes tweet with specified id.

GET [http://localhost:8080/subscribe/](#) - Subscribe to user with specified id

GET [http://localhost:8080/feed/](#) - Get feed of the current user

GET [http://localhost:8080/feed/id](#) - Get feed of a user with specified id


## Testing ##

Start server as described above.

See collection [here](https://documenter.getpostman.com/collection/view/3625254-e6388eef-89de-6980-1bf6-6bb6f812f8f3)

Make sure you have Newman to run tests:

```sh
$ newman run https://www.getpostman.com/collections/e1a392c95d77b33ef49d
```


