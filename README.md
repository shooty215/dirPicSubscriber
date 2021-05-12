# dictPicSubscriber
## Description:
Subscribes to broker's topic, saves picture and gives impulse to ring alert.

Jar is meant to run in terminal with connection and authentication credentials as arguments.
## Usage:
java -jar dirPicSubscriber.jar <broker_ip> <broker_port> <broker_topic> <image_save_directory> <key_store_directory> <user_name> <user_password>

## Status:
Running project's main method (with respective arguments) will subscribe to argumentative broker, saving received files to a argumentative file.

Project is still in its infancy.

Basic functionality is implemented, can't be build without errors yet, etc.

Crude validations implemented.

Terminal usability achieved.

Next step is providing a working jar file on github.

The long term goal is to assure connection and data security.
