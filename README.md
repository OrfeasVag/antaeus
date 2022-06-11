## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus .
docker run -p 7000:7000 antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!

## Report: Orfeas Vagelakis 
Hello all!,

My name is Orfeas Vagelakis and i am a msc student in the department of Management Science & Technology at Athens University of Economics and Business.
With that said my time was limited due to the multiple ongoing school projects thus i could not be as fast as i wanted in order to complete this challenge in one go.

Thank you for this challenge it was fascinating, I really enjoyed it!

### Training - Code Analysis Stage
My first concern was to understand the project and the requirements. Then i took some time in order to become familiar with kotlin and other tools like Docker, Git and Javalin since throughout my studies and work life this is the first time that i actually work on them.
### Building Stage
Right after completing and the endless projects that i had to work on for my studies i started building the billing logic that handles the payments.

While the payments should run automatically at the start of the month i wanted to add a manual option in case that the billing needs to run ad hoc so i implemented the /rest/v1/invoices/execute endpoint in the API.

Time for the automatic process. I created a schedule function that schedules, executes the billing and reschedules the future execution.

### Avoiding double billing
#### INPROGRESS Status
Finally, after completing the needed functionality i wanted to work on double billing cases. One approach was to add an INPROGRESS status, every time that a worker handle an invoice first it needs to set the status to INPROGRESS so that the other workers ignore it. 
That might not work since if two or more workers select the whole dataset the same time the problem remains since they will both have the same rows with the PENDING status. Thus, they will both change the status to INPROGRESS at the same time and then proceed to the billing. (More database traffic(extra INPROGRESS update) problem remains).
#### Database Slicing
Another idea, which i actually implemented, is to cut the database into slices. Each worker only has access to a specific range of the database that is specified by an external system/user by using the API (/rest/config?dbstart=?&dbend=?). The range could also be setted from .properties file.

Although this works for the needs of the challenge there is always the problem that the user might not set the proper range so there might be an overlap between the workers also the database is not static so the ranges need to be updated before every execution.
#### Worker id Field
To avoid double billing a greater solution would be to have a server act as the Master Server who will mark the pending invoices for each worker.
For example there could be a new field which would refer to the id of the worker, thus each worker will only handle its own slice of the database. Each worker could retrieve its id from a .properties file.

### Main Components
#### BillingService
* In case of a successful payment the invoice status changes to PAID.
* In case of a failed payment due to insufficient funds the invoice status remains PENDING.
* In case of a failed payment due to customer not found the invoice status changes to ERRORCNF in order to be handled manually.
* In case of a failed payment due to currency mismatch the invoice status changes to ERRORCMIS in order to be handled manually.
* In case of a failed payment due to network issues system retries 2 more times.

After the execution of the payment process status message returned to the user.
#### API
The vanilla API has been enriched with the endpoints below: 
* /rest/info - Returns information regarding the db slice that this worker handles.
* /rest/config?dbstart=?&dbend=? - Lets the user configure the available range that the worker handles.
* /rest/v1/invoices/execute - Triggers ad hoc the execution of the payment process, returns a report message.

#### Scheduler
Since it is only one function I completed the implementation in the AntaeusApp.kt file.
The function billingScheduler schedules, executes and reschedules the payment process for the 1st of each month.

### Time spent
* Training: 20 hours (Tutorials, examples, etc).
* Code Analysis Build planing: 4 hours.
* Building: 16 hours.
The above times are not accurate 100% because of the many interruptions due to my school obligations.

### Contact
In case of questions feel free to mail me on ovagelakis@gmail.com
### Others
Tools used for the development:
* Docker
* Intellij
* Postman
