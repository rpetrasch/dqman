# Introduction
Example for traceabilty (using auto trails / revisions.

# Installation / test
- Make sure docker service is up and running
- Run docker compose for database container to be available
- Prepare for negative test: comment out the @Transactional annotation in BankService
- Start DqTAcidApplication (spring framework application) and check the logs
- Prepare for positive test: use @Transactional annotation in BankService
- Start DqTAcidApplication (spring framework application) and check the logs
 

# ToDo
- Write tests
- Example for problems


