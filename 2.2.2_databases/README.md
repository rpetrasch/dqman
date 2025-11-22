# Introduction
Performance tests for PostgreSQL and Cassandra (single node)


# Installation / Test
- run docker compose: docker-compose up
- enter container: docker exec -it dq_cassandra /bin/bash 
  - execute: cqlsh -u cassandra -p cassandra
- or outside the container: cqlsh 127.0.0.1 9042 -u cassandra -p cassandra
- check with SQL: SELECT data_center FROM system.local;


# ToDo
- trace https://stackoverflow.com/questions/34075261/cassandra-query-execution-time-analysis
