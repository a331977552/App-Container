#there should be three parts:
##the api
api is the common bridge for the container and app, which means the container and app should import it but app should set scopt to provided as container will import the real package.
##the app
the app will implement the real solutions and be packaged in a specific location for container to load when container sees there a new app jar in place
##the container
the container will start everything and all the supporting components exception the real buession level application.
         when the container starts, it will scan a specific location, if there is a jar file in this location and the jar is just what we wanted,
         then the conatiner will create a new loader to load it and discard the previous loader, which means the loader and associated classes loaded by this loader will be garbage collected.
    